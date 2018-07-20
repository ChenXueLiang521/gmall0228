package com.atguigu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.service.UserAddressService;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.List;
@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;
    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        //创建用户地址对象
        UserAddress userAddress = new UserAddress();
        //将用户的id传递给对象
        userAddress.setUserId(userId);
        //使用通用mapper查出信息
        List<UserAddress> listUserAddress = userAddressMapper.select(userAddress);
        //将信息返回
        return listUserAddress;
    }
}
