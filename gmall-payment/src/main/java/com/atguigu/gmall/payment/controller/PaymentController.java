package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {
    @Reference
    OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    public String index(HttpServletRequest request, Model model){
        // 取得订单id
        String orderId = request.getParameter("orderId");
        // 保存订单orderId
        model.addAttribute("orderId",orderId);
        // 根据orderId 进行查询
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        model.addAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

    @RequestMapping(value = "alipay/submit",method = RequestMethod.POST)
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){
        //取得orderId
        String orderId = request.getParameter("orderId");

        // 取得订单信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setCreateTime(new Date());



        //保存
        paymentService.savePaymentInfo(paymentInfo);

        //使用alpay生成二维码
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        // 声明一个Map
        Map<String,Object> bizContnetMap=new HashMap<>();
        bizContnetMap.put("out_trade_no",paymentInfo.getOutTradeNo());
        bizContnetMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        bizContnetMap.put("subject",paymentInfo.getSubject());
        bizContnetMap.put("total_amount",paymentInfo.getTotalAmount());
        // 将map变成json
        String Json = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(Json);
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
        return form;
    }

    //同步回调：商家跳转到购物结算
    @RequestMapping(value = "/alipay/callback/return",method = RequestMethod.GET)
    public String callbackReturn(){
        return "redirect:"+AlipayConfig.return_order_url;
    }

    //异步回调
    @RequestMapping(value = "/alipay/callback/notify",method = RequestMethod.POST)
    @ResponseBody
    public String paymentNotify(@RequestParam(value = "paramMap") Map<String,String> paramMap) throws AlipayApiException {
        //取得公钥的信息
        boolean signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);
        if (signVerified){
            // 当付款完成 TRADE_SUCCESS，TRADE_FINISHED！
            String trade_status = paramMap.get("trade_status");
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){

                // 如果当前订单已经付款！则该次支付，失败！
                // paymentInfo.paymentStatus


                // 修改订单，支付的状态！
                // 根据trade_no 去修改
                String out_trade_no = paramMap.get("out_trade_no");
                PaymentInfo paymentInfo = new PaymentInfo();
                // 设置状态
                paymentInfo.setPaymentStatus(PaymentStatus.PAID);
                // 设置创建时间
                paymentInfo.setCallbackTime(new Date());
                // 设置内容
                paymentInfo.setCallbackContent(paramMap.toString());
                paymentService.updatePaymentInfo(paymentInfo,out_trade_no);
                return "success";
            }else {
                return "fail";
            }
        }else {
            return "fail";
        }
    }
}
