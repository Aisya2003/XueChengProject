package com.example.content.model.dto;

import com.example.content.model.po.TeachPlan;
import com.example.content.model.po.TeachPlanMedia;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 教学计划DTO
 */
@Data
public class TeachPlanDto extends TeachPlan implements Serializable {
    /**
     * 媒资信息
     */
    private TeachPlanMedia teachplanMedia;

    /**
     * 子目录
     */
    private List<TeachPlanDto> teachPlanTreeNodes;
}
