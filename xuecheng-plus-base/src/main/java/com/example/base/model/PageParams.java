package com.example.base.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@ApiModel("分页请求参数")
public class PageParams {
    /*当前页码默认值*/
    public static final long DEFAULT_PAGE_CURRENT = 1L;
    /*每页记录默认大小*/
    public static final long DEFAULT_PAGE_SIZE = 10L;

    /*当前页码*/
    @ApiModelProperty("当前页码")
    private Long pageNo = DEFAULT_PAGE_CURRENT;
    /*每页记录数*/
    @ApiModelProperty("每页记录数")
    private Long pageSize = DEFAULT_PAGE_SIZE;


}
