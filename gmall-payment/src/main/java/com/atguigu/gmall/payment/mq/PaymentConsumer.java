package com.atguigu.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;

import javax.jms.JMSException;
import javax.jms.MapMessage;

public class PaymentConsumer {
    @Reference
    PaymentService  paymentService;
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void  consumerPaymentResult(MapMessage mapMessage) throws JMSException, AlipayApiException {
    //取得数据对象
        String outTradeNo = mapMessage.getString("outTradeNo");
        //获取消息队列中的时间
        int delaySec = mapMessage.getInt("delaySec");
        //设置检查次数
        int checkCount = mapMessage.getInt("checkCount");
        //创建paymentInfo需要outTradeNo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        //检查是否付款
        boolean result = paymentService.checkPayment(paymentInfo);
        System.out.println("检查的支付结果result="+result);
        //没有付款的时候，并且次数不能为0
        if (!result && checkCount!=0){
            System.out.println("检查的次数checkout="+checkCount);
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }
    }
}
