package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {
    //保存支付信息
    void savePaymentInfo(PaymentInfo paymentInfo);
    //更新支付状态
    void updatePaymentInfo(PaymentInfo paymentInfo, String out_trade_no);
}
