package com.atguigu.gmall.bean;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

public class SpuImage implements Serializable {
    @Column
    @Id
    private String id;
    @Column
    private String spuId;
    @Column
    private String imgName;
    @Column
    private String imgUrl;

    public String getId() {
        return id;
    }

    public String getSpuId() {
        return spuId;
    }

    public String getImgName() {
        return imgName;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSpuId(String spuId) {
        this.spuId = spuId;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
