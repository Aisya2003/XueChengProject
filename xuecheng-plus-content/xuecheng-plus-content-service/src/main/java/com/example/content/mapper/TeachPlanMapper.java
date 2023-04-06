package com.example.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.content.model.dto.TeachPlanDto;
import com.example.content.model.po.TeachPlan;

import java.util.List;

/**
 * <p>
 * 课程计划 Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface TeachPlanMapper extends BaseMapper<TeachPlan> {
    //查询课程计划（树形）
    List<TeachPlanDto> selectTreeNodes(Long courseId);


}
