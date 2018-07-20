package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    // 根据spuId 查询SkuSaleAttrValue 设计到db的使用sql的
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}
