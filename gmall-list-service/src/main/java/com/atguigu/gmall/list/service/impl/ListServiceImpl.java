package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;
    @Autowired
    RedisUtil redisUtil;


    // 定义一些 index,type.
    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        //保存数据
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //搞一个查询器
        String query = makeQueryStringForSearch(skuLsParams);
        //准备搜索
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //将搜索结果转换成自定义java类型SkuLsResult
       SkuLsResult skuLsResult = makeResultForSearch(skuLsParams,searchResult);
        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        //获取redis对象
        Jedis jedis = redisUtil.getJedis();
        int timesToEs = 10;
        //每点击一次 累加一次
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        //判断什么时候更新es
        if (hotScore%timesToEs==0){
            updateHotScore(skuId,Math.round(hotScore));
        }
    }

    private void updateHotScore(String skuId, Long hotScore) {
        String updateJson="{\n" +
                "   \"doc\":{\n" +
                "     \"hotScore\":"+hotScore+"\n" +
                "   }\n" +
                "}";

        Update update = new Update.Builder(updateJson).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


   /* //数据集转换方法
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();
        //从查询到的结果集中取数据
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        //创建一个新的集合来存放新的实体对象
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        //循环遍历iter
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            //取得对象
            SkuLsInfo skuLsInfo = hit.source;
            //准备取高亮
            if(hit.highlight!=null && hit.highlight.size()>0){
                //取出一个集合
                List<String> list = hit.highlight.get("skuName");
                //取出高亮后的名称
                String skuNameHi = list.get(0);
                skuLsInfo.setSkuName(skuNameHi);
            }
            //循环一次 添加一条
            skuLsInfoArrayList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
        //总条数
        skuLsResult.setTotal(searchResult.getTotal());
        //总页数
        long totalPage = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);

        //聚合
        MetricAggregation aggregations = searchResult.getAggregations();
        //按照名称过去的数据groupby_attr
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");

        //取得bucket
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        //创建一个集合
        ArrayList<String> attrvalueList = new ArrayList<>();

        if(buckets!=null && buckets.size()>0){
            for (TermsAggregation.Entry bucket : buckets) {
                //取得数据
                String valueId = bucket.getKey();
                attrvalueList.add(valueId);
            }
        }
        skuLsResult.setAttrValueIdList(attrvalueList);
        return skuLsResult;

    }*/

    // 数据集转换方法！
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();
        // 从查询到的结果集中取数据
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        // 创建一个新的集合来存放新的实体对象
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        // 循环遍历 iter
        if (hits!=null && hits.size()>0){
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                // 取得对象
                SkuLsInfo skuLsInfo = hit.source;
                // 准备取得高亮
                if (hit.highlight!=null && hit.highlight.size()>0){
                    // 取出一个集合
                    List<String> list = hit.highlight.get("skuName");
                    // 取得高亮后的名称，给对象
                    String skuNameHi = list.get(0);
                    skuLsInfo.setSkuName(skuNameHi);
                }
                // 循环一条添加一条
                skuLsInfoArrayList.add(skuLsInfo);
            }
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
        // 总条数
        skuLsResult.setTotal(searchResult.getTotal());
        // 总页数
        // long pages = searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()/skuLsParams.getPageSize()+1;
        // (total+size-1)/size;
        long pages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(pages);

        // 聚合
        MetricAggregation aggregations = searchResult.getAggregations();
        // 按照名称取得数据groupby_attr
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");

        // 取得buckets
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();

        // 创建一个集合
        ArrayList<String> valusList = new ArrayList<>();
        if (buckets!=null && buckets.size()>0){
            for (TermsAggregation.Entry bucket : buckets) {
                // 取得数据
                String valueId = bucket.getKey();
                valusList.add(valueId);
            }
        }
        skuLsResult.setAttrValueIdList(valusList);
        return skuLsResult;
    }





    public  String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //创建查询build
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //创建bool对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断关键字
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            //创建match 并添加到bool对象中
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);

            //准备设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuName");
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            //将高亮结果放入查询器中
            searchSourceBuilder.highlight(highlightBuilder);
        }
         //整三级分类id catalog3Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //整valueId
        if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            //遍历添加
            for (int i = 0; i <skuLsParams.getValueId().length ; i++) {
                String valueId = skuLsParams.getValueId()[i];
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
       //设置分页
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //sort排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);
        //将整个的dsl的拼接语句执行
        searchSourceBuilder.query(boolQueryBuilder);
        String query = searchSourceBuilder.toString();
        System.err.println("query"+query);
        return query;

    }

}
