package com.example.content.service;

import com.example.content.model.dto.CourseCategoryDto;

import java.util.List;

/**
 * 课程分类相关的Service接口
 */
public interface ICourseCategoryService {
    public List<CourseCategoryDto> queryTreeNodes(String id);
}
