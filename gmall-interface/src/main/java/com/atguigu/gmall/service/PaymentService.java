package com.atguigu.gmall.service;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    //保存支付信息
    void savePaymentInfo(PaymentInfo paymentInfo);

    //更新支付状态
    void updatePaymentInfo(PaymentInfo paymentInfo, String out_trade_no);

    //查询paymentInfo对象
    PaymentInfo getpaymentInfo(PaymentInfo paymentInfo);

    //发送支付消息
    void sendPaymentResult(PaymentInfo paymentInfo, String result);

    //检查支付消息
    boolean checkPayment(PaymentInfo paymentInfoQuery)throws AlipayApiException;
    //检查设置延时队列
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);
    //关闭订单
    void closePayment(String id);
}