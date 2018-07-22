package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;

    @Override
    public String saveOrder(OrderInfo orderInfo) {
        //插入时间
        orderInfo.setCreateTime(new Date());
        //使用日历
        Calendar calendar = Calendar.getInstance();
        //当前日期的后一天
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //设置out_trade_no：第三方支付使用
        String out_trade_no = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(out_trade_no);
        //保存数据
        orderInfoMapper.insertSelective(orderInfo);

        //订单详细表
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }


        //返回订单id
        return orderInfo.getId();
    }


    // 生成流水号
    public String getTradeNo(String userId){
        //redis: key:
        String tradeNoKey="user:"+userId+":tradeCode";
        // redis
        Jedis jedis = redisUtil.getJedis();
        String tradeNo = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey,10*60,tradeNo);
        return tradeNo;
    }

    // check 流水号
    public boolean checkTradeCode(String tradeNo,String userId){
        // 比较
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        if (tradeCode!=null && !"".equals(tradeCode)){
            if (tradeCode.equals(tradeNo)){
                return  true;
            }else {
                return  false;
            }
        }
        return false;
    }
    // 删除redis中的tradeNo
    public void delTradeNo(String userId){
        //redis: key:
        String tradeNoKey="user:"+userId+":tradeCode";
        // redis
        Jedis jedis = redisUtil.getJedis();

        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            return true;
        }else {
            return false;
        }
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        //查询orderInfo
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        //更新orderStarus   为PAID
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        //创建连接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            String orderJson = initWareOrder(orderId);
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            //MessageConsumer consumer = session.createConsumer(order_result_queue);
            MessageProducer producer = session.createProducer(order_result_queue);
            //消息内容是json字符串：orderInfo orderDetails 整体可以将其看作是一个map
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(orderJson);
            producer.send(activeMQTextMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();


        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime",new Date()).andEqualTo("processStatus",ProcessStatus.UNPAID);
        //查询过期时间
        return orderInfoMapper.selectByExample(example);
    }
    @Async
    @Override
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 订单信息
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 付款信息
        paymentService.closePayment(orderInfo.getId());
    }

    //最终要返回的字符串
    public String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);
        Map map = initWareOrder(orderInfo);
        String String  = JSON.toJSONString(map);
        return String;
    }
    //拼接字符串的map

    public Map initWareOrder(OrderInfo orderInfo) {
        Map map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        //付款方式
        map.put("paymentWay","2");
        //仓库的id
        map.put("wareId",orderInfo.getWareId());

        //details == 集合orderDetail
        ArrayList<Object> arrayList = new ArrayList<>();
        // 取得到OrderDetail列表
        List<OrderDetail> orderDetailsList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailsList) {
            Map mapDetail = new HashMap();
            mapDetail.put("skuId",orderDetail.getSkuId());
            mapDetail.put("skuNum",orderDetail.getSkuNum());
            mapDetail.put("skuName",orderDetail.getSkuName());
            // 将orderDtail 放入list集合中
            arrayList.add(mapDetail);
        }
        map.put("details",arrayList);
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) throws InvocationTargetException, IllegalAccessException {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        // 第一个获取原始订单 53 === 1,2
        OrderInfo orderInfoOrigin  = getOrderInfo(orderId);
        // wareSkuMap 得到[{"wareId":"1","skuIds":["20","18"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        // 准备将仓库数据中wareId，skuIds进行循环匹配，进行拆单。
        for (Map map : maps) {
            String wareId = (String) map.get("wareId");
            List<String> skuIds = (List<String>) map.get("skuIds");
            // 设置子订单
            OrderInfo subOrderInfo = new OrderInfo();
            // 属性拷贝 id 主键自增 属性拷贝一定放在设置id为null的前面！
            BeanUtils.copyProperties(subOrderInfo,orderInfoOrigin);
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            // 子订单的details
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            // 创建新的子订单详细信息
            List<OrderDetail> subOrderDetailList = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetailList) {
                for (String skuId : skuIds) {
                    if (skuId.equals(orderDetail.getSkuId())){
                        orderDetail.setId(null);
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }
            // 新的子订单给新的orderInfo
            subOrderInfo.setOrderDetailList(subOrderDetailList);
            // 计算一下总钱数
            subOrderInfo.getTotalAmount();
            // 保存到数据库
            saveOrder(subOrderInfo);
            // 返回子订单集合！
            subOrderInfoList.add(subOrderInfo);
        }
        // 更改订单状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);

        return subOrderInfoList;
    }
}
