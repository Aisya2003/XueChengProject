package com.example.content.service;

import com.example.content.model.dto.CourseCategoryDto;

import java.util.List;

/**
 * 课程分类相关的Service接口
 */
public interface ICourseCategoryService {
    /**
     * 查询课程分类的信息
     *
     * @param id 课程id
     * @return 课程分类信息
     */
    public List<CourseCategoryDto> queryTreeNodes(String id);
}
