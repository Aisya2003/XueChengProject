package com.example.learning.api;

import com.example.learning.model.dto.ChooseCourseDto;
import com.example.learning.model.dto.CourseTablesDto;
import com.example.learning.service.IChooseCourseService;
import com.example.learning.util.GetUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class CourseTableController {
    private final IChooseCourseService chooseCourseService;

    @Autowired
    public CourseTableController(IChooseCourseService chooseCourseService) {
        this.chooseCourseService = chooseCourseService;
    }

    //添加信息到选课表
    @PostMapping("/choosecourse/{courseId}")
    public ChooseCourseDto insertChooseCourse(@PathVariable("courseId") Long courseId) {
        GetUser.XcUser user = GetUser.getUser();
        if (user == null) return null;
        String userId = user.getId();
        return chooseCourseService.insertChooseCourse(userId, courseId);
    }

    //获取课程的学习状态
    @PostMapping("/choosecourse/learnstatus/{courseId}")
    public ChooseCourseDto getLearningStatus(@PathVariable("courseId") Long courseId) {
        GetUser.XcUser user = GetUser.getUser();
        if (user == null) return null;
        String userId = user.getId();

        
        CourseTablesDto learningQualification = chooseCourseService.getLearningQualification(userId, courseId);
        ChooseCourseDto chooseCourseDto = new ChooseCourseDto();
        chooseCourseDto.setLearnStatus(learningQualification.getLearnStatus());
        return chooseCourseDto;
    }
}
