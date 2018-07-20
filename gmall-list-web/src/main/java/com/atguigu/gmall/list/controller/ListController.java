package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    private ListService listService;
    @Reference
    private ManageService manageService;
    @RequestMapping("list.html")
   // @ResponseBody
    public String getSkuLSResult(SkuLsParams skuLsParams, Model model){

        // 分页   设置分页显示的条数
        skuLsParams.setPageSize(4);

        SkuLsResult skuLsResult = listService.search(skuLsParams);
        //将数据保存给后台使用
        model.addAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());
        //System.out.println(JSON.toJSONString(skuLsResult));

        //查出平台属性，平台属性值
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        //根据id查询数据 从哪个项目中来
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
        //保存 传递到前台
        model.addAttribute("attrList",attrList);


        //找出哪些属性被选中了‘
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();

        //做个URL拼接 参数skuLsParam
        String makeUrl = makeUrlParam(skuLsParams);
        //去重
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            //取得每一个BaseAttrInfo对象
            BaseAttrInfo baseAttrInfo = iterator.next();
            //取得每一个属性值
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            //循环
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if (baseAttrValue.getId()!=null && baseAttrInfo.getId().length()>0){
                    //判断baseAttrValue。getId 和skulsparams中的id是否相同
                    if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
                        for (String valueId : skuLsParams.getValueId()) {
                            if (valueId.equals(baseAttrValue.getId())){
                                iterator.remove();
                                //创建一个被选中的值的对象
                                BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                                //拼串 例如：屏幕尺寸:5.1-5.5英寸
                                baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                                //添加之前去重复
                                String urlParam = makeUrlParam(skuLsParams,valueId);
                                baseAttrValueSelected.setUrlParam(urlParam);
                                //将更改后的baseAttrValue对象添加到一个被选中的集合
                                baseAttrValueArrayList.add(baseAttrValueSelected);
                            }
                        }
                    }
                }

            }

        }

        //分页
        int totalPages = (int) ((skuLsResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize());
        model.addAttribute("totalPages",totalPages);
        model.addAttribute("pageNo",skuLsParams.getPageNo());

        //将被选中的属性值集合保存起来
        model.addAttribute("baseAttrValuesList",baseAttrValueArrayList);
        //全局检索
        model.addAttribute("keyword",skuLsParams.getKeyword());

        //将重新制作的URL保存
        model.addAttribute("urlParam",makeUrl);


        return "list";
    }



    //拼接方法，判断页面传递过来的参数，在makeUrl中是否存在
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        //拼接字符串
        String makeUrl = "";
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            makeUrl+="keyword="+skuLsParams.getKeyword();
        }
        //三级分类id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            if(makeUrl.length()>0){
                makeUrl+="&";
            }
            makeUrl+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        // 属性id
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环 fori
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];
                // 传递进来的值，跟valueId做比较，一样就不拼接！
                if (excludeValueIds !=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)){
                        // 后面代码不走。
                        continue;
                    }
                }
                if (makeUrl.length()>0){
                    makeUrl+="&";
                }
                makeUrl+="valueId="+valueId;
            }
        }

        return makeUrl;

    }

}