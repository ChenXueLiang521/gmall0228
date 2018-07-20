package com.atguigu.gmall.bean;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

public class SpuInfo implements Serializable{
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String spuName;

    @Column
    private String description;

    @Column
    private  String catalog3Id;

    /*存属性名集合*/
    @Transient
    private List<SpuSaleAttr> spuSaleAttrList;
    /*spuimg集合*/
    @Transient
    private List<SpuImage> spuImageList;

    public List<SpuSaleAttr> getSpuSaleAttrList() {
        return spuSaleAttrList;
    }

    public void setSpuSaleAttrList(List<SpuSaleAttr> spuSaleAttrList) {
        this.spuSaleAttrList = spuSaleAttrList;
    }

    public void setSpuImageList(List<SpuImage> spuImageList) {
        this.spuImageList = spuImageList;
    }

    public List<SpuImage> getSpuImageList() {
        return spuImageList;
    }

    public String getId() {
        return id;
    }

    public String getSpuName() {
        return spuName;
    }

    public String getDescription() {
        return description;
    }

    public String getCatalog3Id() {
        return catalog3Id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSpuName(String spuName) {
        this.spuName = spuName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCatalog3Id(String catalog3Id) {
        this.catalog3Id = catalog3Id;
    }
}
