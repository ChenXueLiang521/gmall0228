package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {
    @Reference
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        //如果成功，则更新order的状态
        if ("success".equals(result)){
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 准备通知库存，进行减库存的操作！
            // 先发送消息队列
            orderService.sendOrderStatus(orderId);
            // 更新订单的进度，变成等待发货 ,拼接字符串的时候：注意：OrderDetail 不能为空！
            orderService.updateOrderStatus(orderId,ProcessStatus.WAITING_DELEVER);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
        }
    }

    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void  consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        //如果成功，则进行更新orderInfo状态
        if ("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(orderId,ProcessStatus.DELEVERED);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.STOCK_EXCEPTION);
        }
    }
}
