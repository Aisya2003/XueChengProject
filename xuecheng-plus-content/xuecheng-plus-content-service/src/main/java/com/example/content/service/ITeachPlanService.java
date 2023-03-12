package com.example.content.service;

import com.example.content.model.dto.SaveTeachplanDto;
import com.example.content.model.dto.TeachPlanDto;

import java.util.List;

public interface ITeachPlanService {
    /**
     * 获取教学大纲
     * @param courseId 课程id
     * @return 课程教学大纲的树型结构以及相关的媒资信息
     */
    List<TeachPlanDto> selectTeachPlanTree(Long courseId);

    /**
     * 保存课程大纲（新增和修改）
     * @param dto 课程大纲添加dto
     */
    void saveTeachPlan(SaveTeachplanDto dto);
}
