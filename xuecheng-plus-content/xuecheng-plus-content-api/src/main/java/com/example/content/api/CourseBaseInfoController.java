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
import com.example.content.util.GetUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
public class CourseBaseInfoController {

    private final ICourseBaseInfoService baseInfoService;

    @Autowired
    public CourseBaseInfoController(ICourseBaseInfoService baseInfoService) {
        this.baseInfoService = baseInfoService;
    }

    //获取课程信息列表
    @PostMapping("/course/list")
    @PreAuthorize("hasAuthority('course_find_list')")
    public PageResult<CourseBase> list(PageParams params, @RequestBody QueryCourseParamsDto dto) {

        //认证后实现细粒度授权
        String companyId = Objects.requireNonNull(GetUser.getUser()).getCompanyId();

        return baseInfoService.queryCourseBaseList(params, dto, companyId);
    }

    //添加课程
    @PostMapping("/course")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_add')")
    /*启用校验,设置校验分组*/
    public CourseBaseInfoDto createCourseBase(
            @RequestBody
            @Validated(ValidationGroups.Insert.class) AddCourseDto dto) {

        //获取培训机构的id
        Long companyId = Long.valueOf(Objects.requireNonNull(GetUser.getUser()).getCompanyId());

        return baseInfoService.createCourseBase(companyId, dto);
    }

    /**
     * 获取修改课程的信息
     *
     * @param courseId 课程id
     * @return 课程信息Dto
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_add')")
    public CourseBaseInfoDto getCourseBaseInfoDto(@PathVariable("courseId") Long courseId) {
        return baseInfoService.getCourseBaseInfoDto(courseId);
    }

    /**
     * 修改课程信息
     *
     * @param dto 请求参数
     * @return 页面数据
     */
    @PutMapping("/course")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_add')")
    public CourseBaseInfoDto modifyCourseBaseInfoDto(@RequestBody EditCourseDto dto) {

        Long companyId = Long.valueOf(Objects.requireNonNull(GetUser.getUser()).getCompanyId());

        return baseInfoService.updateCourseBaseInfo(companyId, dto);
    }

    @DeleteMapping("/course/{courseId}")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_del')")
    public PageResult<CourseBase> deleteCourse(@PathVariable("courseId") Long courseId) {
        Long companyId = Long.valueOf(Objects.requireNonNull(GetUser.getUser()).getCompanyId());
        return baseInfoService.deleteCourse(courseId, companyId);
    }


}
