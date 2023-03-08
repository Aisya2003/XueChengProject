package com.example.content.service;

import com.example.base.model.*;
import com.example.content.model.dto.AddCourseDto;
import com.example.content.model.dto.CourseBaseInfoDto;
import com.example.content.model.dto.QueryCourseParamsDto;
import com.example.content.model.po.CourseBase;

/**
 * 课程管理service
 */

public interface ICourseBaseInfoService {

    /**
     * 课程查询
     * @param params 分页参数
     * @param dto 查询参数下·
     * @return
     */
    PageResult<CourseBase> queryCourseBaseList(PageParams params, QueryCourseParamsDto dto);

    /**
     * 新增课程
     * @param companyId 培训机构id
     * @param dto   新增课程信息
     * @return 课程包括基本信息，营销信息
     */
    CourseBaseInfoDto createCourseBase(Long companyId,AddCourseDto dto);
}
