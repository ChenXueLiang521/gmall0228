package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;


public interface ManageService {
    //查询一级分类的所有
    List<BaseCatalog1> getCatalog1();

    //根据一级查询二级分类
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    //根据二级分类查询三级分类
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    //根据三级分类的id查询属性
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    //保存数据的方法
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    //编辑数据（获取属性值数据）
    BaseAttrInfo getAttrInfo(String attrId);

    //获取销售属性
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    //查询基本销售属性表
    List<BaseSaleAttr> getBaseSaleAttrList();

    //保存商品属性信息
    void saveSpuInfo(SpuInfo spuInfo);

    //从spu中获取商品图片信息
    List<SpuImage> getSpuImageList(String spuId);

    //获取spu的销售属性
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    //保存sku信息
    void saveSkuInfo(SkuInfo skuInfo);

    //查询skuInfo列表
    // List<SkuInfo> getSkuInfoListBySpu(String spuId);
    //根据skuId查询商品信息
    SkuInfo getSkuInfo(String skuId);
    //根据spuId 和skuId 查询属性列表
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo);
    //根据spuId查找销售属性列表
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);
    //根据属性id 查询属性 属性值
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
