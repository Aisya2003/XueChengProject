package com.example.content.service;

import com.example.content.model.dto.BindTeachPlanMediaDto;
import com.example.content.model.dto.SaveTeachPlanDto;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.model.po.TeachPlanMedia;

import java.util.List;

public interface ITeachPlanService {
    /**
     * 获取教学大纲
     *
     * @param courseId 课程id
     * @return 课程教学大纲的树型结构以及相关的媒资信息
     */
    List<TeachPlanDto> selectTeachPlanTree(Long courseId);

    /**
     * 保存课程大纲（新增和修改）
     *
     * @param dto 课程大纲添加dto
     */
    void saveTeachPlan(SaveTeachPlanDto dto);


    /**
     * 教学计划和媒资绑定
     *
     * @param dto 请求参数
     * @return 教学计划媒资关系表
     */
    TeachPlanMedia bindMedia(BindTeachPlanMediaDto dto);
}
