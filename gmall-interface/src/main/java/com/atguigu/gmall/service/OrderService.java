package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public interface OrderService {
    //保存订单信息
    String  saveOrder(OrderInfo orderInfo);
    //生成流水号
    String getTradeNo(String userId);
    //验证流水号
    boolean checkTradeCode(String userId,String tradeNo);
    //删除redis中的tradeNo
    void  delTradeNo(String userId);
    //检查库存
    boolean checkStock(String skuId, Integer skuNum);
    //根据orderId查询order信息
    OrderInfo getOrderInfo(String orderId);
    //更新orderInfo
    void updateOrderStatus(String orderId, ProcessStatus processStatus);
    //发送订单状态消息
    void sendOrderStatus(String orderId);
    //；轮询
    List<OrderInfo> getExpiredOrderList();
    //处理延时订单
    void execExpiredOrder(OrderInfo orderInfo);
    //拼接字符串的map、
    Map initWareOrder(OrderInfo orderInfo);
    //拆单
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap) throws InvocationTargetException, IllegalAccessException;
}
