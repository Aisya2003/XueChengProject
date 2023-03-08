package com.example.content.api;

import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.content.model.dto.AddCourseDto;
import com.example.content.model.dto.CourseBaseInfoDto;
import com.example.content.model.dto.QueryCourseParamsDto;
import com.example.content.model.po.CourseBase;
import com.example.content.service.ICourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "课程管理相关接口",tags = "课程管理相关接口")
public class CourseBaseInfoController {

    private ICourseBaseInfoService baseInfoService;

    @Autowired
    public CourseBaseInfoController(ICourseBaseInfoService baseInfoService) {
        this.baseInfoService = baseInfoService;
    }

    @ApiOperation("课程查询相关接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams params, @RequestBody QueryCourseParamsDto dto){
        return baseInfoService.queryCourseBaseList(params,dto);
    }

    @ApiOperation("课程添加接口")
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(@RequestBody AddCourseDto dto){

        //获取培训机构的id
        Long companyId = 22L;

       return baseInfoService.createCourseBase(companyId,dto);
    }
}
