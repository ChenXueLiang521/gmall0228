package com.atguigu.gmall.usermanage.controller;


import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @RequestMapping("findAll")
    @ResponseBody
    public List<UserInfo> findAll(){
        List<UserInfo> list = userInfoService.findAll();
        // iter
        for (UserInfo userInfo : list) {
            System.out.println(userInfo);
        }
        return list;
    }

    @RequestMapping("like")
    @ResponseBody
    public List<UserInfo> like(){
        List<UserInfo> list = userInfoService.findLikeUserInfo();
        // iter
        for (UserInfo userInfo : list) {
            System.out.println(userInfo);
        }
        return list;
    }

    @RequestMapping("add")
    @ResponseBody
    public void add(){
        UserInfo userInfo = new UserInfo();
        userInfo.setId(null);
        userInfo.setLoginName("王二小");
        userInfo.setNickName("baobao");
        userInfo.setName("宋哲");
        userInfo.setPasswd("520");
        userInfo.setPhoneNum("8888888");
      userInfoService.addUserInfo(userInfo);
    }

    @RequestMapping("upd")
    @ResponseBody
    public void upd(UserInfo userInfo){
        userInfo.setId("6");
        userInfo.setLoginName("啊哲666");
        userInfoService.upd(userInfo);
    }
    @RequestMapping("upd1")
    @ResponseBody
    public void upd1(UserInfo userInfo){
        userInfo.setLoginName("王二小");
        userInfo.setPhoneNum("0000000000");
        userInfoService.upd1(userInfo);
    }

    @RequestMapping("del")
    @ResponseBody
    public void del(UserInfo userInfo){
        userInfo.setId("1003");
        userInfoService.del(userInfo);
    }

}
