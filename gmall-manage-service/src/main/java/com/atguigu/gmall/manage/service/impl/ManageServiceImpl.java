package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.manage.constan.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;
    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;
    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<BaseCatalog1> getCatalog1() {

        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);

        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
      /*  BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);*/
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3id(Long.parseLong(catalog3Id));
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //保存数据，编辑数据一起来处理

        //判断否有主键
        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
            //有主键则修改
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        } else {
            //没有主键则添加
            if (baseAttrInfo.getId().length() == 0) {
                baseAttrInfo.setId(null);
            }
            //开始插入数据
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        //操作属性值，把原来的属性值全部清空
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);

        //重新插入属性值
        if (baseAttrInfo.getAttrValueList() != null && baseAttrInfo.getAttrValueList().size() > 0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                //插入操作
                if (attrValue.getId().length() == 0) {
                    attrValue.setId(null);
                }
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        //创建属对象(attrId实际是BaseAttrInfo的id)
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        //创建属性值对象
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        //根据attrid字段查询 对象
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        //给属性对象中的属性值集合赋值
        List<BaseAttrValue> attrValueList = baseAttrValueMapper.select(baseAttrValue);
        baseAttrInfo.setAttrValueList(attrValueList);
        //将属性对象返回
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        List<SpuInfo> spuInfoList = spuInfoMapper.select(spuInfo);
        return spuInfoList;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {

        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存数据，spuinfo，spuimage，spusaleattr，spusalettrvalue 和更新一同操作
        if (spuInfo.getId() != null && spuInfo.getId().length() > 0) {
            spuInfoMapper.updateByPrimaryKey(spuInfo);
        } else {
            //判断key
            if (spuInfo.getId() != null && spuInfo.getId().length() == 0) {
                spuInfo.setId(null);
            }
            spuInfoMapper.insertSelective(spuInfo);
        }
        //先删除，在插入(商品图片列表)
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImage);

        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        for (SpuImage image : spuImageList) {
            //设置Id为null,启动自增长
            if (image.getId() != null && image.getId().length() > 0) {
                image.setId(null);
            }
            //因为前台页面传递的数据没有spuid，所以设置shpid
            image.setSpuId(spuInfo.getId());
            spuImageMapper.insertSelective(image);
        }

        //属性，sh属性值， 先删除后插入
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);

        SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuInfo.getId());
        spuSaleAttrValueMapper.delete(spuSaleAttrValue);

        //先找到SpuSaleAttrList
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        for (SpuSaleAttr saleAttr : spuSaleAttrList) {
            if (saleAttr.getId()!=null && saleAttr.getId().length()==0){
                saleAttr.setId(null);
            }
            //因为前台页面传递的数据没有spuid 所以设置spuid
            saleAttr.setSpuId(spuInfo.getId());
            spuSaleAttrMapper.insertSelective(saleAttr);

            //插入属性值
            List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
            for (SpuSaleAttrValue saleAttrValue : spuSaleAttrValueList) {
                if (saleAttrValue.getId()!=null && saleAttrValue.getId().length()==0){
                    saleAttrValue.setId(null);
                }
                //因为前台页面传递的数据没有spuId 所以设置spuId
                saleAttrValue.setSpuId(spuInfo.getId());
                spuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(Long.parseLong(spuId));
        return spuSaleAttrList;
    }

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //添加SkuInfo数据
        if (skuInfo.getId()==null || skuInfo.getId().length()==0){
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        } else {
            skuInfoMapper.updateByPrimaryKey(skuInfo);
        }

        // 先删除，再添加
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue attrValue : skuAttrValueList) {
            // 坑！
            attrValue.setSkuId(skuInfo.getId());
            if (attrValue.getId()!=null&& attrValue.getId().length()==0){
                attrValue.setId(null);
            }
            skuAttrValueMapper.insertSelective(attrValue);
        }
        // 属性值添加
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
            saleAttrValue.setSkuId(skuInfo.getId());
            if (saleAttrValue.getId()!=null && saleAttrValue.getId().length()==0){
                saleAttrValue.setSkuId(null);
            }
            skuSaleAttrValueMapper.insertSelective(saleAttrValue);
        }
        // 图片添加
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage image : skuImageList) {
            image.setSkuId(skuInfo.getId());
            if (image.getId()!=null && image.getId().length()==0){
                image.setId(null);
            }
            skuImageMapper.insertSelective(image);
        }

    }




  /*  @Override
    public List<SkuInfo> getSkuInfoListBySpu(String spuId) {
        List<SkuInfo> skuInfoList = skuInfoMapper.selectSkuInfoListBySpu(Long.parseLong(spuId));
        return  skuInfoList;
    }*/

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        //判断当前redis中是否有key
        Jedis jedis = redisUtil.getJedis();
        SkuInfo skuInfo = null;
        String skuInfoKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
        if (jedis.exists(skuInfoKey)) {
            // 如果存在 取出数据
            String skuInfoJson = jedis.get(skuInfoKey);

            if (skuInfoJson != null && !"".equals(skuInfoJson)) {
                //将字符串转成对象
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                //返回
                return skuInfo;
            }
        } else {
            //不存在从数据库中取
            //数据先从redis取  redis没有再从数据库中取
            skuInfo = getSkuInfoDB(skuId);//提取出来的ctrl+alt+m
            //放到redis中jedis.set()没有过期时间
            jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
            return skuInfo;
        }
        return skuInfo;
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        /*skuImage*/
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        //将图片集合放入Info对象
        skuInfo.setSkuImageList(skuImageList);


        //添加查询销售属性
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValues);

        //属性值
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuSaleAttrValue> skuSaleAttrValues = skuSaleAttrValueMapper.select(skuSaleAttrValue);
        skuInfo.setSkuSaleAttrValueList(skuSaleAttrValues);


        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(Long.parseLong(skuInfo.getId()), Long.parseLong(skuInfo.getSpuId()));

        return spuSaleAttrList;
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);

        return skuSaleAttrValueListBySpu;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        //将集合传进来的id进行分割 ["13","5","10","6","9","11","12","8"]
        String valueId = StringUtils.join(attrValueIdList,",");
        // 调用mapper层查询数据id
        List<BaseAttrInfo> baseAttrInfos =  baseAttrInfoMapper.selectAttrInfoListByIds(valueId);
        return baseAttrInfos;
    }

}
