package com.example.content.api;

import com.example.content.model.dto.CoursePreviewDto;
import com.example.content.model.po.CoursePublish;
import com.example.content.service.ICoursePublishService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CoursePublishController {
    private ICoursePublishService coursePublishService;

    @Autowired
    public CoursePublishController(ICoursePublishService coursePublishService) {
        this.coursePublishService = coursePublishService;
    }

    @ApiOperation("课程信息预览数据生成")
    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId) {
        ModelAndView modelAndView = new ModelAndView();

        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

        modelAndView.addObject("model", coursePreviewInfo);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }

    //提交审核
    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitToPublishPre(@PathVariable("courseId") Long courseId) {
        Long companyId = 22L;
        coursePublishService.commitToPublishPre(courseId, companyId);
    }

    //课程发布
    @ResponseBody
    @PostMapping("/coursepublish/{courseId}")
    public void coursePublish(@PathVariable("courseId") Long courseId) {
        Long companyId = 22L;
        coursePublishService.coursePublish(companyId, courseId);
    }

    //查询课程发布表内容
    @GetMapping("/inner/coursepublish/{courseId}")
    @ResponseBody
    public CoursePublish getCoursePublishInfo(@PathVariable("courseId") Long courseId) {
        return coursePublishService.getCoursePublishByCourseId(courseId);
    }
}
