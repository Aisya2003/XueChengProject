package com.example.learning.service;

import com.example.content.model.po.CoursePublish;
import com.example.learning.model.dto.ChooseCourseDto;
import com.example.learning.model.dto.CourseTablesDto;
import com.example.learning.model.po.ChooseCourse;
import com.example.learning.model.po.CourseTables;

public interface IChooseCourseService {
    /**
     * 添加选课信息到数据库
     *
     * @param courseId 课程id
     * @return 选课结果
     */
    ChooseCourseDto insertChooseCourse(String userId, Long courseId);

    /**
     * 添加免费课程的选课信息
     *
     * @param userId   用户Id
     * @param courseId 课程Id
     * @return 选课结果
     */
    ChooseCourse addFreeCourse(String userId, Long courseId, CoursePublish coursePublish);

    /**
     * 添加免费课程的选课信息
     *
     * @param userId   用户Id
     * @param courseId 课程Id
     * @return 选课结果
     */
    ChooseCourse addChargeCourse(String userId, Long courseId, CoursePublish coursePublish);

    /**
     * 添加信息到用户的课程表中
     *
     * @param chooseCourse 选课的信息
     * @return 课程表实体
     */
    CourseTables addCourseTable(ChooseCourse chooseCourse);

    /**
     * 添加信息到选课表中
     *
     * @param userId             用户id
     * @param courseId           课程id
     * @param coursePublish      课程发布表中的课程信息
     * @param courseType         课程的收费类型
     * @param courseChooseStatus 课程的支付状态
     * @param coursePrice        课程的价格
     * @return 添加的选课记录实体
     */
    public ChooseCourse addChooseCourseFromCoursePublish(String userId, Long courseId, CoursePublish coursePublish, String courseType, String courseChooseStatus, Float coursePrice);

    /**
     * 获取用户对课程的学习资格
     *
     * @param userId   用户id
     * @param courseId 课程id
     * @return 用户选课表中的记录
     */
    public CourseTablesDto getLearningQualification(String userId, Long courseId);

}
