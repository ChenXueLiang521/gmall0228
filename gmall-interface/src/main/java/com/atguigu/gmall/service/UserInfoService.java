package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserInfoService {

    // 业务层必须要手动添加
    public List<UserInfo> findAll();

    // like
    public List<UserInfo> findLikeUserInfo();

    void addUserInfo(UserInfo userInfo);

    // 通过id修改
    void upd(UserInfo userInfo);
    // 通过id修改
    void upd1(UserInfo userInfo);

    // 删除
    void  del(UserInfo userInfo);
    //用户登录信息
    UserInfo login(UserInfo userInfo);
    //验证身份方法
    UserInfo verify(String userId);
}
