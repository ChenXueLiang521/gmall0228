package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    //根据三级分类id查询属性表
    public List<BaseAttrInfo> getBaseAttrInfoListByCatalog3id(Long catalog3Id);
    //根据id查询 属性 和属性值
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param(value = "valueId") String valueId);
}
