package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {
    //保存商品信息到es
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);
    //准备完成dsl语句的功能
    SkuLsResult search(SkuLsParams skuLsParams);
    //评分 热度排行
    void incrHotScore(String skuId);

}
