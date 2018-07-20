package com.atguigu.gmall.usermanage.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60;


    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    RedisUtil redisUtil;


    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> findLikeUserInfo() {
        // 创建一个Example 对象
        Example example = new Example(UserInfo.class);
        example.createCriteria().andLike("loginName","%a%");
        // 调用方法
        List<UserInfo> userInfoList = userInfoMapper.selectByExample(example);
        return userInfoList;
    }

    @Override
    public void addUserInfo(UserInfo userInfo) {
        // insert() 全部插入
        userInfoMapper.insertSelective(userInfo);
        // userInfoMapper.insertSelective(userInfo); 选择性的插入
    }

    @Override
    public void upd(UserInfo userInfo) {
    //   userInfoMapper.updateByPrimaryKey(userInfo);、
        // 选择性
        userInfoMapper.updateByPrimaryKeySelective(userInfo);
    }

    @Override
    public void upd1(UserInfo userInfo) {
        // 根据名称修改
        Example example = new Example(UserInfo.class);
        // 创建sql语句的查询体
        // update user_info set xxx where loginName=? 该?是从外界传递过来。
        example.createCriteria().andEqualTo("loginName",userInfo.getLoginName());
        userInfoMapper.updateByExampleSelective(userInfo,example);
    }

    @Override
    public void del(UserInfo userInfo) {

        userInfoMapper.deleteByPrimaryKey(userInfo);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String passwd = userInfo.getPasswd();
        //spring 工具类了解一下
        String password = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(password);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if (info!=null){
            //将用户信息放入redis
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey_prefix+info.getId()+userinfoKey_suffix,userKey_timeOut, JSON.toJSONString(info));
            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        //从redis中取数据 首先定义key'
        String key = userKey_prefix+userId+userinfoKey_suffix;
        Jedis jedis = redisUtil.getJedis();
        //判断key是否存在
        if (jedis.exists(key)){
            //延长时效
            jedis.expire(key,userKey_timeOut);
            String userJson = jedis.get(key);
            if (userJson!=null && !"".equals(userJson) ){
                //将json转换成对象
                UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
                return userInfo;
            }
        }

        return null;
    }

}
