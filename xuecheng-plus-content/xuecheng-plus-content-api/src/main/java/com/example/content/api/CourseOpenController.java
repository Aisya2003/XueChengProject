package com.example.content.api;

import com.example.content.model.dto.CoursePreviewDto;
import com.example.content.service.ICoursePublishService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/open")
public class CourseOpenController {
    private final ICoursePublishService coursePublishService;

    @Autowired
    public CourseOpenController(ICoursePublishService coursePublishService) {
        this.coursePublishService = coursePublishService;
    }

    //课程预览
    @GetMapping("/course/whole/{courseId}")
    public CoursePreviewDto getPreviewInfo(@PathVariable("courseId") Long courseId) {
        return coursePublishService.getCoursePreviewInfo(courseId);
    }
}
