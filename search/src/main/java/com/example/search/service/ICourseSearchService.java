package com.example.search.service;

import com.example.base.model.PageParams;
import com.example.search.dto.SearchCourseParamDto;
import com.example.search.po.CourseIndex;
import com.example.search.dto.SearchPageResultDto;

public interface ICourseSearchService {


    /**
     * @param pageParams 分页参数
     * @param dto        搜索条件
     * @return 课程列表
     */
    SearchPageResultDto<CourseIndex> queryCoursePubIndex(PageParams pageParams, SearchCourseParamDto dto);

}
