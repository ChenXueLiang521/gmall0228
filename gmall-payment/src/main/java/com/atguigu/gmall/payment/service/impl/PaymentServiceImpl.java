package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private ActiveMQUtil activeMQUtil;
    @Autowired
    private AlipayClient alipayClient;


    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo, String out_trade_no) {
        Example example = new Example(PaymentInfo.class);
        //修改的字段--值
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        // 修改非主键的时候，需要使用updateByExample();
        paymentInfoMapper.updateByExample(paymentInfo,example);

    }

    @Override
    public PaymentInfo getpaymentInfo(PaymentInfo paymentInfo) {

        return paymentInfoMapper.selectOne(paymentInfo);
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        //获取连接 初始化
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue paymentResultQueue = session.createQueue("PAYMENT_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(paymentResultQueue);
            //发送一个订单id  发送一个结果
            ActiveMQMapMessage mqMapMessage = new ActiveMQMapMessage();
            mqMapMessage.setString("orderId",paymentInfo.getOrderId());
            mqMapMessage.setString("result",result);
            //发送消息
            producer.send(mqMapMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    //验证支付宝是否支付成功
    public boolean checkPayment(PaymentInfo paymentInfoQuery) throws AlipayApiException {
        // 根据传入的对象查找paymengInfo 对象
        PaymentInfo paymentInfo = getpaymentInfo(paymentInfoQuery);

        if (paymentInfo.getPaymentStatus()== PaymentStatus.ClOSED || paymentInfo.getPaymentStatus()== PaymentStatus.PAID){
            //  说明该交易成功！
            return true;
        }

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // out_trade_no:ATGUIGU1532006223101012 orderId = 53 的
        request.setBizContent("{" +
                "\"out_trade_no\":\""+paymentInfo.getOutTradeNo()+"\"" +
                "  }");
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            // TRADE_SUCCESS=支付成功，TRADE_FINISHED=支付成功不可退款！
            if ("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus())){
                // 修改订单-支付状态，
                paymentInfo.setPaymentStatus(PaymentStatus.PAID);
                // 修改时间
                paymentInfo.setCreateTime(new Date());
                // 更新数据状态
                updatePaymentInfo(paymentInfo,paymentInfo.getOutTradeNo());
                // 发送消息给订单。修改订单状态。 url:payment.gmall.com/sendPaymentResult?orderId=53&result=success
                sendPaymentResult(paymentInfo,"success");
                System.out.println("调用成功");
                return  true;
            }else{
                return false;
            }
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    //在付款 生成二维码的时候调用
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec , int checkCount) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            //创建生产者
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",outTradeNo);
            activeMQMapMessage.setInt("delaySec",delaySec);
            activeMQMapMessage.setInt("checkCount",checkCount);
            //设置延时多少时间
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);
            producer.send(activeMQMapMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closePayment(String id) {
        //获取paymentInfo
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",id);
        //创建一个paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

}
