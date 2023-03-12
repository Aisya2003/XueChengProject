package com.example.content.model.dto;

import com.example.content.model.po.Teachplan;
import com.example.content.model.po.TeachplanMedia;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.List;

/**
 * 教学计划DTO
 */
@Data
public class TeachPlanDto extends Teachplan implements Serializable {
    //媒资信息
    private TeachplanMedia teachplanMedia;

    //子目录
    private List<TeachPlanDto> teachPlanTreeNodes;
}
