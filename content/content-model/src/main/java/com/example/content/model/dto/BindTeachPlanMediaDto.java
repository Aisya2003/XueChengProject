package com.example.content.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BindTeachPlanMediaDto {

    /**
     * 媒资文件id
     */
    private String mediaId;

    /**
     * 媒资文件名称
     */
    private String fileName;

    /**
     * 课程计划标识
     */
    private Long teachplanId;


}

