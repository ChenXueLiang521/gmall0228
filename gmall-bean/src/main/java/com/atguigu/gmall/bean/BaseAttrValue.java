package com.atguigu.gmall.bean;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;

public class BaseAttrValue implements Serializable {

    @Id
    @Column
    private String id;
    @Column
    private String valueName;
    @Column
    private String attrId;
    //追加平台属性值
    @Transient
    private String urlParam;

    public String getUrlParam() {
        return urlParam;
    }

    public void setUrlParam(String urlParam) {
        this.urlParam = urlParam;
    }

    public String getId() {
        return id;
    }

    public String getValueName() {
        return valueName;
    }

    public String getAttrId() {
        return attrId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValueName(String valueName) {
        this.valueName = valueName;
    }

    public void setAttrId(String attrId) {
        this.attrId = attrId;
    }
}
