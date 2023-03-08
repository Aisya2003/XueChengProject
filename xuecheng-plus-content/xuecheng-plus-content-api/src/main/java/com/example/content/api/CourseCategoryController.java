package com.example.content.api;

import com.example.content.model.dto.CourseCategoryDto;
import com.example.content.service.ICourseCategoryService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@Api(value = "课程分类相关接口",tags = "课程分类相关接口")
public class CourseCategoryController {
    private ICourseCategoryService service;

    @Autowired
    public CourseCategoryController(ICourseCategoryService service) {
        this.service = service;
    }

    /**
     *课程分类查询接口
     * @return 树形结构
     */
    @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryDto> queryTreeNodes() {
        return  service.queryTreeNodes("1");
    }
}
