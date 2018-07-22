package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserAddressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    // 调用service 层服务
    @Reference
    private UserAddressService userAddressService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    // 根据用id 进行查询 ，用户id 应该是传递过来的值
    // http://localhost:8080/trade?userId=1
   /* @RequestMapping("trade")
    @ResponseBody
    public List<UserAddress> trade(HttpServletRequest request){
        String userId = request.getParameter("userId");
        List<UserAddress> userAddressList = userAddressService.getUserAddressList(userId);
        return  userAddressList;

    }*/

    @RequestMapping("trade")
    @LoginRequire
    public String tradeInit(HttpServletRequest request, Model model){
        String userId = (String) request.getAttribute("userId");
        //得到购物车列表
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        //收货人地址
        List<UserAddress> userAddressList = userAddressService.getUserAddressList(userId);
        model.addAttribute("addressList",userAddressList);
        // 订单信息集合
        List<OrderDetail> orderDetailList=new ArrayList<>();
        //订单详情 从cartInfo中取得的
        for (CartInfo cartInfo : cartInfoList) {
            //创建对象
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetail.setImgUrl(cartInfo.getImgUrl());

            orderDetailList.add(orderDetail);
        }
        model.addAttribute("orderDetailList",orderDetailList);
        //数据展示 //保存信息 给前台显示
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        model.addAttribute("totalAmount",orderInfo.getTotalAmount());
        // 保存流水号到前台
        String tradeNo = orderService.getTradeNo(userId);
        model.addAttribute("tradeCode",tradeNo);
        return "trade";
    }

    @RequestMapping(value = "submitOrder",method = RequestMethod.POST)
    @LoginRequire
    public String submitOrder(HttpServletRequest request, OrderInfo orderInfo, Model model){
        //获取userId
        String userId = (String) request.getAttribute("userId");
        //防止重复提交
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(tradeNo, userId);
        if (!flag){
            model.addAttribute("errMsg","订单提交失败 请勿重复提交表单");
            return "tradeFail";
        }

        /*验库存*/
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result){
                model.addAttribute("errMsg","库存不足,请重新下单!");
                return "tradeFail";
            }
        }


        //orderInfo表中由此与固定的字段值需要设置
        orderInfo.setOrderStatus(OrderStatus.CLOSED);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.setUserId(userId);

        // 计算的过程
        orderInfo.sumTotalAmount();
        orderInfo.setTotalAmount(orderInfo.getTotalAmount());

        //调用service中的方法
        String orderId = orderService.saveOrder(orderInfo);
        //删除redis中的tradeNo`
        orderService.delTradeNo(userId);
        //支付宝接口
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

    //拆单控制器  json字符串
    @RequestMapping(value = "orderSplit",method = RequestMethod.POST)
    @ResponseBody
    public String orderSplit(HttpServletRequest request) throws InvocationTargetException, IllegalAccessException {
        //根据orderId,进行拆单，库存系统
        String orderId = request.getParameter("orderId");
        //返回JSON字符串
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 服务层= 将根据orderId 传递过来的参数，对orderInfo订单信息进行拆单工作.
        List<OrderInfo> orderInfo = orderService.orderSplit(orderId,wareSkuMap);
        List<Map> orderDetailList = new ArrayList<>();
        //从订单中取得子订单，一个订单中有多个子订单
        for (OrderInfo info : orderInfo) {
         Map map = orderService.initWareOrder(info);
         orderDetailList.add(map);

        }
        //将一个集合转成字符串orderDetail集合
        return JSON.toJSONString(orderDetailList);
    }

}
