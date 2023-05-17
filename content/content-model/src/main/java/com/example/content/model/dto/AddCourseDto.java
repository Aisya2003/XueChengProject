package com.example.content.model.dto;

import com.example.base.exception.ValidationGroups;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;


@Data
public class AddCourseDto {
    /**
     * 课程名称
     */
    private String name;

    /**
     * 适用人群
     */
    private String users;
    /**
     * 课程标签
     */
    private String tags;

    /**
     * 大分类
     */
    private String mt;
    /**
     * 小分类
     */
    private String st;
    /**
     * 课程等级
     */
    private String grade;
    /**
     * 教学模式
     */
    private String teachmode;

    /**
     * 课程描述
     */
    private String description;

    /**
     * 课程图片
     */
    private String pic;

    /**
     * 收费规则
     */
    @ApiModelProperty(value = "收费规则，对应数据字典", required = true)
    private String charge;

    /**
     * 价格
     */
    private Float price;
    /**
     * 原价
     */
    private Float originalPrice;


    /**
     * qq
     */
    private String qq;

    /**
     * 微信
     */
    private String wechat;
    /**
     * 电话号码
     */
    private String phone;

    /**
     * 有效期
     */
    private Integer validDays;
}
