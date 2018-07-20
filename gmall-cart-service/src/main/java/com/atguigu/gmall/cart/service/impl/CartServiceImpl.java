package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Reference
    private ManageService manageService;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //根据skuId查找商品信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //查看购物车cartInfo中是否有该商品
        CartInfo cartInfoQuery = new CartInfo();
        cartInfoQuery.setSkuId(skuId);
        cartInfoQuery.setUserId(userId);

        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQuery);
        if (cartInfoExist!=null){
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            //更改添加都需要放入缓存
        }else {
            //没有创建购物车的话
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            //实时价格
            cartInfo.setSkuPrice(skuInfo.getPrice());
            //加购物车的价格
            cartInfo.setCartPrice(skuInfo.getPrice());
            //新插入的购物车也需要放入缓存
            cartInfoMapper.insertSelective(cartInfo);
            //将插入的信息给 已经存下的购物车对象
            cartInfoExist=cartInfo;
        }
        //将购物车放入redis中 user:userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //获取redis对象
        Jedis jedis = redisUtil.getJedis();
        //保存数据
        String cartJson = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey,skuId,cartJson);
        //更新购物车过期时间  user:userId:info
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        //看缓存 看数据
        //缓存key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //创建redis对象
        Jedis jedis = redisUtil.getJedis();
        //reids--hash 对应  Java-List
        List<String> cartJsons = jedis.hvals(userCartKey);
        //循环
        if (cartJsons!=null && "".equals(cartJsons)){
            List<CartInfo> cartInfoList = new ArrayList<>();
            for (String cartJson : cartJsons){
                //将对象转换成cartInfo对象
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //根据id进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {

                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            //走数据库  验价过程  从数据库读取到缓存
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return  cartInfoList;
        }


    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId) {
        //循环判断
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCarPrice(userId);
        for (CartInfo cartInfoCK : cartListFromCookie) {
            boolean isMatch = false;
            for (CartInfo infoDB : cartInfoListDB) {
                //如果skuId相同，则说明是同一件商品 数据加一
                if (cartInfoCK.getSkuId().equals(infoDB.getSkuId())){
                    infoDB.setSkuNum(cartInfoCK.getSkuNum()+infoDB.getSkuNum());
                //更新
                    cartInfoMapper.updateByPrimaryKeySelective(infoDB);
                    isMatch = true;
                }

            }
            //插入信息
            if(!isMatch){
                //userid赋值
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        List<CartInfo> cartInfoList = loadCartCache(userId);
        //需要跟cookie中的数据进行匹配 根据skuId，并且isChecked=“1” 从新更新数据库
        for (CartInfo cartInfo : cartInfoList) {
            for (CartInfo info : cartListFromCookie) {
                //找skuId
                if (cartInfo.getSkuId().equals(info.getSkuId())){
                    //并且isChecked为1
                    if ("1".equals(info.getIsChecked())){
                        cartInfo.setIsChecked(info.getIsChecked());
                        //更新到redis中
                        checkCart(cartInfo.getSkuId(),info.getIsChecked(),userId);
                    }
                }
            }
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, String skuId, String isChecked) {
        //取出redis数据  定义key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        //hset:因为skuId 是唯一的
        String cartInfoJson = jedis.hget(userCartKey, skuId);
        //将字符串转换为对象
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        //对该对象进行状态修改
        cartInfo.setIsChecked(isChecked);
        //将修改后的数据存到redis中
        jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));
        //将所有选中的商品保存到一个新的key中
         String cartIsCheckKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
         //将数据存到redis当中  isChecked等1是创建   0删除
        if ("1".equals(isChecked)){
            jedis.hset(cartIsCheckKey,skuId,JSON.toJSONString(cartInfo));
        }else {
           jedis.hdel(cartIsCheckKey,skuId);
        }

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //定义key 创建redis对象
        String userCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        List<String> cartCheckedList = jedis.hvals(userCheckedKey);
        //将字符串转换成对象
        //创建一个对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        for (String cartJson : cartCheckedList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            cartInfoList.add(cartInfo);
        }
        return cartInfoList;
    }


    private List<CartInfo> loadCartCache(String userId) {
        //调用mapper
        List<CartInfo> cartInfoList =  cartInfoMapper.selectCartListWithCarPrice(userId);
        //判断集合
        if (cartInfoList!=null && cartInfoList.size()>0){
            //准备redis
            Jedis jedis = redisUtil.getJedis();
            //对数据进行转换
            String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
            //创建map 
            Map<String,String> map = new HashMap<>(cartInfoList.size());
            for (CartInfo cartInfo : cartInfoList) {
            //将cartInfo转换成对象
                String cartJson = JSON.toJSONString(cartInfo);
                map.put(cartInfo.getSkuId(),cartJson);
            }
            //往redis中添加数据
            jedis.hmset(userCartKey,map);
            jedis.close();
        }
        return cartInfoList;
    }
}
