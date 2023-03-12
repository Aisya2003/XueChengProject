package com.example.content.api;

import com.example.base.exception.ValidationGroups;
import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.content.model.dto.AddCourseDto;
import com.example.content.model.dto.CourseBaseInfoDto;
import com.example.content.model.dto.EditCourseDto;
import com.example.content.model.dto.QueryCourseParamsDto;
import com.example.content.model.po.CourseBase;
import com.example.content.service.ICourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    /*启用校验,设置校验分组*/
    public CourseBaseInfoDto createCourseBase(
            @RequestBody
            @Validated(ValidationGroups.Insert.class) AddCourseDto dto){

        //获取培训机构的id
        Long companyId = 22L;

       return baseInfoService.createCourseBase(companyId,dto);
    }

    /**
     * 获取修改课程的信息
     * @param courseId 课程id
     * @return 课程信息Dto
     */
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseInfoDto(@PathVariable("courseId")Long courseId){
        return baseInfoService.getCourseBaseInfoDto(courseId);
    }

    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBaseInfoDto(@RequestBody EditCourseDto dto){

        Long companyId = 22L;
        return baseInfoService.updateCourseBaseInfo(companyId,dto);
    }

}
