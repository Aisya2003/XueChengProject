package com.example.search.service;

public interface IIndexService {

    /**
     * @param indexName 索引名称
     * @param id        主键
     * @param object    索引对象
     */
    public Boolean addCourseIndex(String indexName, String id, Object object);


    /**
     * @param indexName 索引名称
     * @param id        主键
     * @param object    索引对象
     */
    public Boolean updateCourseIndex(String indexName, String id, Object object);

    /**
     * @param indexName 索引名称
     * @param id        主键
     */
    public Boolean deleteCourseIndex(String indexName, String id);

}
