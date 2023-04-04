package com.example.content.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 课程预览数据
 */
@Data
public class CoursePreviewDto {
    /**
     * 课程基本信息
     */
    private CourseBaseInfoDto courseBase;
    /**
     * 课程营销信息
     */
    private List<TeachPlanDto> teachPlans;
}
