package com.example;

import com.example.base.model.PageParams;
import com.example.base.model.PageResult;
import com.example.content.mapper.CourseBaseMapper;
import com.example.content.mapper.CourseCategoryMapper;
import com.example.content.model.dto.CourseCategoryDto;
import com.example.content.model.dto.QueryCourseParamsDto;
import com.example.content.model.po.CourseBase;
import com.example.content.model.po.CourseCategory;
import com.example.content.service.ICourseBaseInfoService;
import com.example.content.service.ICourseCategoryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


@SpringBootTest(classes = XuechengPlusContentServiceApplication.class)
class XuechengPlusContentServiceApplicationTests {

    private CourseBaseMapper mapper;
    private ICourseBaseInfoService service;
    private ICourseCategoryService categoryService;

    private CourseCategoryMapper categoryMapper;

    @Autowired
    public XuechengPlusContentServiceApplicationTests(
            CourseBaseMapper mapper,
            ICourseBaseInfoService service,
            ICourseCategoryService categoryService,
            CourseCategoryMapper categoryMapper

            ) {
        this.categoryService = categoryService;
        this.service = service;
        this.mapper = mapper;
        this.categoryMapper = categoryMapper;
    }

    @Test
    void testMapper() {
        CourseBase courseBase = mapper.selectById("1");
        System.out.println("courseBase = " + courseBase);
    }

    @Test
    void contextLoads() {
        List<CourseCategoryDto> courseCategoryDtoList = categoryMapper.selectTreeNodes("1");
        Assertions.assertNotNull(courseCategoryDtoList);
    }

    @Test
    void testPagination() {
        PageParams params = new PageParams(1L, 10L);
        PageResult<CourseBase> courseBasePageResult = service.queryCourseBaseList(params, new QueryCourseParamsDto());
        System.out.println("courseBasePageResult = " + courseBasePageResult);
    }

    @Test
    void testTreeNodes() {
        List<CourseCategoryDto> courseCategoryDtoList = categoryService.queryTreeNodes("1");
        System.out.println(courseCategoryDtoList);
    }
}
