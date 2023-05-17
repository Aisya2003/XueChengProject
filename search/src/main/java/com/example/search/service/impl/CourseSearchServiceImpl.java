package com.example.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.search.dto.SearchCourseParamDto;
import com.example.search.dto.SearchPageResultDto;
import com.example.search.po.CourseIndex;
import com.example.search.service.ICourseSearchService;
import com.example.base.model.PageParams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class CourseSearchServiceImpl implements ICourseSearchService {

    @Value("${elasticsearch.course.index}")
    private String courseIndexStore;
    @Value("${elasticsearch.course.source_fields}")
    private String sourceFields;

    private final String[] MATCH_FIELDS = {"name", "description"};

    private final RestHighLevelClient client;

    @Autowired
    public CourseSearchServiceImpl(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public SearchPageResultDto<CourseIndex> queryCoursePubIndex(PageParams pageParams, SearchCourseParamDto dto) {

        //设置索引
        SearchRequest searchRequest = new SearchRequest(courseIndexStore);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        //source源字段过虑
        String[] sourceFieldsArray = sourceFields.split(",");
        searchSourceBuilder.fetchSource(sourceFieldsArray, new String[]{});


        if (dto == null) {
            //上传条件为null则返回默认查询
            dto = new SearchCourseParamDto();
        }


        //关键字
        if (!StringUtils.isEmpty(dto.getKeywords())) {
            //匹配关键字
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(
                    dto.getKeywords(),
                    MATCH_FIELDS);
            //设置匹配占比
            multiMatchQueryBuilder.minimumShouldMatch("70%");
            //提升另个字段的Boost值
            multiMatchQueryBuilder.field("name", 10);
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }


        //过滤
        if (!StringUtils.isEmpty(dto.getMt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("mtName", dto.getMt()));
        }
        if (!StringUtils.isEmpty(dto.getSt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("stName", dto.getSt()));
        }
        if (!StringUtils.isEmpty(dto.getGrade())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade", dto.getGrade()));
        }


        //分页
        int pageNo = pageParams.getPageNo().intValue();
        int pageSize = pageParams.getPageSize().intValue();
        int from = (pageNo - 1) * pageSize;
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(pageSize);


        //布尔查询
        searchSourceBuilder.query(boolQueryBuilder);

        //高亮设置
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        //设置高亮字段
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        highlightBuilder.fields().add(new HighlightBuilder.Field("description"));
        searchSourceBuilder.highlighter(highlightBuilder);


        //请求搜索
        searchRequest.source(searchSourceBuilder);

        //聚合设置
        buildAggregation(searchRequest);

        //尝试搜索
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("课程搜索异常：{}", e.getMessage());
            return new SearchPageResultDto<>(new ArrayList<>(), 0, 0, 0);
        }

        //结果集处理
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        //记录总数
        TotalHits totalHits = hits.getTotalHits();
        //设置高亮字段和数据列表
        List<CourseIndex> list = handleResponse(searchHits);

        //获取聚合结果
        List<String> mtList = getAggregation(searchResponse.getAggregations(), "mtAgg");
        List<String> stList = getAggregation(searchResponse.getAggregations(), "stAgg");

        //构建返回参数
        SearchPageResultDto<CourseIndex> pageResult = new SearchPageResultDto<>(list, totalHits.value, pageNo, pageSize);
        pageResult.setMtList(mtList);
        pageResult.setStList(stList);

        return pageResult;
    }

    /**
     * 设置高亮，构建数据
     *
     * @param searchHits 搜索结果
     * @return 数据列表
     */
    private List<CourseIndex> handleResponse(SearchHit[] searchHits) {
        List<CourseIndex> courseIndices = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            String sourceAsString = hit.getSourceAsString();
            CourseIndex courseIndex = JSON.parseObject(sourceAsString, CourseIndex.class);

            //课程id
            Long id = courseIndex.getId();
            //取出名称
            String name = courseIndex.getName();
            //获取描述
            String description = courseIndex.getDescription();
            //取出高亮字段内容
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null) {
                //设置两个字段值的高亮属性
                HighlightField nameField = highlightFields.get("name");
                HighlightField descriptionField = highlightFields.get("description");
                if (nameField != null) {
                    name = getHighLightFont(nameField);
                }
                if (descriptionField != null) {
                    description = getHighLightFont(descriptionField);
                }
            }
            courseIndex.setId(id);
            //设置高亮
            courseIndex.setName(name);
            courseIndex.setDescription(description);

            courseIndices.add(courseIndex);
        }
        return courseIndices;
    }

    /**
     * 构建高亮属性
     *
     * @param highlightField 高亮字段
     * @return 高亮文字
     */
    private String getHighLightFont(HighlightField highlightField) {
        Text[] fragments = highlightField.getFragments();
        StringBuilder stringBuilder = new StringBuilder();
        for (Text str : fragments) {
            stringBuilder.append(str.string());
        }
        return stringBuilder.toString();
    }


    /**
     * 构建返回的分类数据
     * mtName，stName
     *
     * @param request 构建分类的请求
     */
    private void buildAggregation(SearchRequest request) {
        request.source().aggregation(AggregationBuilders
                .terms("mtAgg")
                .field("mtName")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("stAgg")
                .field("stName")
                .size(100)
        );

    }

    /**
     * 获取分类
     *
     * @param aggregations 聚合
     * @param aggName      聚合名称
     * @return 分类
     */
    private List<String> getAggregation(Aggregations aggregations, String aggName) {
        // 4.1.根据聚合名称获取聚合结果
        Terms brandTerms = aggregations.get(aggName);
        // 4.2.获取buckets
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        // 4.3.遍历
        List<String> brandList = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            // 4.4.获取key
            String key = bucket.getKeyAsString();
            brandList.add(key);
        }
        return brandList;
    }
}
