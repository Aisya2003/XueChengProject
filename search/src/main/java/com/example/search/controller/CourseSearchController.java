package com.example.search.controller;

import com.example.base.model.PageParams;
import com.example.search.dto.SearchCourseParamDto;
import com.example.search.dto.SearchPageResultDto;
import com.example.search.po.CourseIndex;
import com.example.search.service.ICourseSearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course")
public class CourseSearchController {

    private final ICourseSearchService ICourseSearchService;

    @Autowired
    public CourseSearchController(ICourseSearchService ICourseSearchService) {
        this.ICourseSearchService = ICourseSearchService;
    }


    @GetMapping("/list")
    public SearchPageResultDto<CourseIndex> list(PageParams pageParams, SearchCourseParamDto searchCourseParamDto) {

        return ICourseSearchService.queryCoursePubIndex(pageParams, searchCourseParamDto);

    }
}
