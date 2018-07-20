package com.atguigu.gmall.bean;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

public class SkuImage implements Serializable {
    @Id
    @Column
    String id;
    @Column
    String skuId;
    @Column
    String imgName;
    @Column
    String imgUrl;
    @Column
    String spuImgId;
    @Column
    String isDefault;

    public String getId() {
        return id;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getImgName() {
        return imgName;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getSpuImgId() {
        return spuImgId;
    }

    public String getIsDefault() {
        return isDefault;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public void setSpuImgId(String spuImgId) {
        this.spuImgId = spuImgId;
    }

    public void setIsDefault(String isDefault) {
        this.isDefault = isDefault;
    }
}
