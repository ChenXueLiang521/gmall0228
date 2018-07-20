package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private RedisUtil redisUtil;

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

        return orderInfo;
    }
}
