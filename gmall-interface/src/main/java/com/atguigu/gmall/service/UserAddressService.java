package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;

import java.util.List;

public interface UserAddressService {

    //通过用户的id查询用户的地址
    List<UserAddress> getUserAddressList(String userId);

}
