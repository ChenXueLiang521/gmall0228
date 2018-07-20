package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService  {
    //添加购物车
    void  addToCart(String skuId,String userId,Integer skuNum);
    //根据用户id查询购物车
    List<CartInfo> getCartList(String userId);
    //合并购物车
    List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);
    //操作db
    void checkCart(String userId, String skuId, String isChecked);
    //获取购物车选中的商品列表
    List<CartInfo> getCartCheckedList(String userId);
}
