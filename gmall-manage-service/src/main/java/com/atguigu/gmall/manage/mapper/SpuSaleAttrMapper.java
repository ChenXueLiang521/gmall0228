package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {
    //根据spu查询 属性集合
    List<SpuSaleAttr> selectSpuSaleAttrList(long spuId);
    //根据skuId spuId查找属性值  列表
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(long skuId,long spuId);

}
