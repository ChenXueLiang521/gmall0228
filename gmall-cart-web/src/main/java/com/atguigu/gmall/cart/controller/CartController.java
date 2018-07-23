package com.atguigu.gmall.cart.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @RequestMapping(value = "addToCart",method = RequestMethod.POST)
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response, Model model){
        //先判断cartInfo中是否有该商品，如果有数据加一，没有就新创建
        //取得userId，skuNum，skuId
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        String userId = (String) request.getAttribute("userId");
        //判断用户是否登录
        if(userId!=null){
            //走数据库
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else{
            //走cookie
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
        }
        //获取skuInfo的信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //存储skuInfo对象
        model.addAttribute("skuInfo",skuInfo);
        model.addAttribute("skuNum",skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        //判断是否登录
        String userId = (String) request.getAttribute("userId");

        List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
        List<CartInfo> cartList = null;
        if (userId!=null){
            if (cartListFromCookie!=null && cartListFromCookie.size()>0){
                //合并购物车  根据skuid相同的合并
                cartList = cartService.mergeToCartList(cartListFromCookie,userId);
                //cookie删除掉
                cartCookieHandler.deleteCartCookie(request,response);

            }else {
                //从redis中取得，或者从数据库
                cartList = cartService.getCartList(userId);
            }
           //将集合保存到前台
           request.setAttribute("cartList",cartList);
        }else {
            //没有登录，cookie中取得
            List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
            request.setAttribute("cartList",cookieHandlerCartList);
        }
        //需要保存购物车的数据
        return "cartList";
    }

    @RequestMapping(value = "checkCart",method = RequestMethod.POST)
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        //取得userId判断是否登录
        String userId = (String) request.getAttribute("userId");
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        if (userId!=null){
            //登录
            cartService.checkCart(userId,skuId,isChecked);
        }else {
            //未登录
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }


    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true )
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        //取得userId
        String userId = (String) request.getAttribute("userId");

        //点击结算时cookie+db
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);

        //循环遍历cookie中的值，跟db合并
        if(cookieHandlerCartList!=null && cookieHandlerCartList.size()>0){
            //准备合并
            cartService.mergeToCartList(cookieHandlerCartList,userId);
            //删除cookie中的数据
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }


}
