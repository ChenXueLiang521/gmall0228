package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

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
}
