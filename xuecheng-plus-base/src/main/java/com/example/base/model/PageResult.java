package com.example.base.model;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@AllArgsConstructor
@ApiModel("分页结果")
public class PageResult<T> {
    @ApiModelProperty("返回具体结果列表")
    private List<T> items;
    @ApiModelProperty("记录总数")
    private Long counts;
    @ApiModelProperty("当前页码")
    private Long page;
    @ApiModelProperty("当前页面最大记录数")
    private Long pageSize;
}
