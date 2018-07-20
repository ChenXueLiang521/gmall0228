package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UserInfoService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;


@Controller
public class PassPortController {
    @Value("${token.key}")
    String signKey;
    @Reference
    private UserInfoService userInfoService;


    @RequestMapping(value = "index")
    public String index(HttpServletRequest request){
        //取得跳转过来的URL并保存
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    @RequestMapping(value = "login",method = RequestMethod.POST)
    @ResponseBody
    public String login(HttpServletRequest request, UserInfo userInfo){
        //自动获取ip ，通过nginx 的反向代理得到的
        String ip = request.getHeader("X-forwarded-for");
        //当用户登录成功之后，生成token
        if(userInfo!=null){
            UserInfo info = userInfoService.login(userInfo);
            if (info!=null){
                //生成token
                Map map = new HashMap();
                map.put("userId",info.getId());
                map.put("nickName",info.getNickName());
                String token = JwtUtil.encode(signKey, map, ip);
                System.out.println("token："+token);
                return token;
            }
        }
        return "fail";
    }


    /*验证verify*/
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        //取得参数
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");
        //解密
        Map<String, Object> decode = JwtUtil.decode(token, signKey, currentIp);
        if (decode!=null){
            //取得用户id
            String userId = (String) decode.get("userId");
            //根据用户id判断redis中是否有登录用户 查找  用户
            UserInfo userInfo = userInfoService.verify(userId);
            if (userInfo!=null){
                return "success";
            }

        }
        return "fail";
    }
}
