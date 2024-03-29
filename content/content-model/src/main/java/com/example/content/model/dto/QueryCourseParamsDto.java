package com.example.content.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class QueryCourseParamsDto {

    /**
     * 审核状态
     */
    private String auditStatus;
    /**
     * 课程名称
     */
    private String courseName;
    /**
     * 发布状态
     */
    private String publishStatus;
}
