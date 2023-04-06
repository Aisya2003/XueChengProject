package com.example.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.constant.Dictionary;
import com.example.content.model.po.CoursePublish;
import com.example.learning.feignclient.CoursePublishClient;
import com.example.learning.mapper.ChooseCourseMapper;
import com.example.learning.mapper.CourseTablesMapper;
import com.example.learning.model.dto.ChooseCourseDto;
import com.example.learning.model.dto.CourseTablesDto;
import com.example.learning.model.po.ChooseCourse;
import com.example.learning.model.po.CourseTables;
import com.example.learning.service.IChooseCourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ChooseCourseServiceImpl implements IChooseCourseService {
    private final CoursePublishClient client;
    private final ChooseCourseMapper chooseCourseMapper;
    private final CourseTablesMapper courseTablesMapper;
    private final IChooseCourseService proxy;

    @Autowired
    @Lazy
    public ChooseCourseServiceImpl(CoursePublishClient client, ChooseCourseMapper chooseCourseMapper, CourseTablesMapper courseTablesMapper, IChooseCourseService proxy) {
        this.client = client;
        this.chooseCourseMapper = chooseCourseMapper;
        this.courseTablesMapper = courseTablesMapper;
        this.proxy = proxy;
    }

    @Override
    public ChooseCourseDto insertChooseCourse(String userId, Long courseId) {
        //在发布表中获取课程信息
        CoursePublish coursePublish = client.getCoursePublishInfo(courseId);
        if (coursePublish == null) {
            log.error("调用远程服务失败！");
        }

        ChooseCourse chooseCourse = null;
        //根据收费的类型使用不同的添加方法
        assert coursePublish != null;
        String chargeType = coursePublish.getCharge();
        if (chargeType.equals(Dictionary.COURSE_FREE.getCode()))
            chooseCourse = proxy.addFreeCourse(userId, courseId, coursePublish);
        else if (chargeType.equals(Dictionary.COURSE_CHARGE.getCode()))
            chooseCourse = proxy.addChargeCourse(userId, courseId, coursePublish);

        //处理事务结果
        if (chooseCourse == null) {
            throw new RuntimeException("添加选课事务失败！");
        }

        //获取学习资格
        //若为免费课程，则添加完成后设置学习状态为正常学习
        CourseTablesDto learningQualification = new CourseTablesDto();
        if (chargeType.equals(Dictionary.COURSE_FREE.getCode()))
            learningQualification.setLearnStatus(Dictionary.LEARNING_QUA_NORMAL.getCode());
        else learningQualification = getLearningQualification(userId, courseId);

        //封装返回信息
        ChooseCourseDto chooseCourseDto = new ChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse, chooseCourseDto);
        chooseCourseDto.setLearnStatus(learningQualification.getLearnStatus());

        return chooseCourseDto;
    }

    @Override
    @Transactional
    public ChooseCourse addFreeCourse(String userId, Long courseId, CoursePublish coursePublish) {
        //查询是否已存在相同的选课记录
        List<ChooseCourse> chosenCourses = checkChooseCourse(
                userId,
                courseId,
                Dictionary.CHOOSE_COURSE_TYPE_FREE.getCode(),
                Dictionary.CHOOSE_COURSE_STATUS_SUCCESS.getCode());

        if (chosenCourses != null && chosenCourses.size() > 0) {
            return chosenCourses.get(0);
        }

        //添加选课信息
        ChooseCourse chooseCourse = proxy.addChooseCourseFromCoursePublish(
                userId,
                courseId,
                coursePublish,
                Dictionary.CHOOSE_COURSE_TYPE_FREE.getCode(),
                Dictionary.CHOOSE_COURSE_STATUS_SUCCESS.getCode(),
                0f);

        //添加到用户的课程表中
        CourseTables courseTables = proxy.addCourseTable(chooseCourse);
        if (courseTables == null) throw new RuntimeException("添加课程表失败");

        return chooseCourse;
    }

    @Transactional
    public ChooseCourse addChooseCourseFromCoursePublish(String userId,
                                                         Long courseId,
                                                         CoursePublish coursePublish,
                                                         String courseType,
                                                         String courseChooseStatus,
                                                         Float coursePrice) {
        //封装选课信息
        ChooseCourse chooseCourse = new ChooseCourse();
        chooseCourse.setCourseId(courseId);
        chooseCourse.setCourseName(coursePublish.getName());
        chooseCourse.setUserId(userId);
        chooseCourse.setCompanyId(coursePublish.getCompanyId());
        chooseCourse.setOrderType(courseType);
        chooseCourse.setCreateDate(LocalDateTime.now());
        chooseCourse.setCoursePrice(coursePrice);
        chooseCourse.setValidDays(coursePublish.getValidDays());
        chooseCourse.setStatus(courseChooseStatus);
        chooseCourse.setValidtimeStart(LocalDateTime.now());
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(chooseCourse.getValidDays()));

        //保存到数据库
        chooseCourseMapper.insert(chooseCourse);
        return chooseCourse;
    }

    @Override
    public CourseTablesDto getLearningQualification(String userId, Long courseId) {
        //查询用户的课程表
        CourseTables courseTables = checkCourseTables(courseId, userId);
        CourseTablesDto courseTablesDto = new CourseTablesDto();

        if (courseTables == null) {
            //没有学习资格
            courseTablesDto.setLearnStatus(Dictionary.LEARNING_QUA_REJECT.getCode());
            return courseTablesDto;
        }

        BeanUtils.copyProperties(courseTables, courseTablesDto);

        //判断课程是否过期
        LocalDateTime expireDate = courseTablesDto.getValidtimeEnd();
        if (LocalDateTime.now().isAfter(expireDate)) {
            //过期
            courseTablesDto.setLearnStatus(Dictionary.LEARNING_QUA_EXPIRED.getCode());
            return courseTablesDto;
        }

        //可以学习
        courseTablesDto.setLearnStatus(Dictionary.LEARNING_QUA_NORMAL.getCode());

        return courseTablesDto;
    }

    /**
     * 查询选课表信息
     *
     * @param userId     用户id
     * @param courseId   课程id
     * @param courseType 收费类型
     * @return 选课实体
     */
    private List<ChooseCourse> checkChooseCourse(String userId, Long courseId, String courseType, String coursePayStatus) {
        LambdaQueryWrapper<ChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChooseCourse::getUserId, userId);
        queryWrapper.eq(ChooseCourse::getCourseId, courseId);
        queryWrapper.eq(ChooseCourse::getOrderType, courseType);//免费课程
        queryWrapper.eq(ChooseCourse::getStatus, coursePayStatus);//已选课
        return chooseCourseMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public ChooseCourse addChargeCourse(String userId, Long courseId, CoursePublish coursePublish) {
        List<ChooseCourse> chooseCourses = checkChooseCourse(
                userId,
                courseId,
                Dictionary.CHOOSE_COURSE_TYPE_CHARGE.getCode(),
                Dictionary.CHOOSE_COURSE_STATUS_UNPAID.getCode());

        if (chooseCourses != null && chooseCourses.size() > 0) {
            return chooseCourses.get(0);
        }

        //向选课记录表添加记录


        return proxy.addChooseCourseFromCoursePublish(
                userId,
                courseId,
                coursePublish, Dictionary.CHOOSE_COURSE_TYPE_CHARGE.getCode(),
                Dictionary.CHOOSE_COURSE_STATUS_UNPAID.getCode(), coursePublish.getPrice());
    }

    /**
     * 添加到课程表中
     *
     * @param chooseCourse 选课的信息
     * @return 课程表
     */

    @Override
    @Transactional
    public CourseTables addCourseTable(ChooseCourse chooseCourse) {
        //校验课程表中是否已存在
        Long courseId = chooseCourse.getCourseId();
        String userId = chooseCourse.getUserId();
        CourseTables courseTables = checkCourseTables(courseId, userId);
        if (courseTables != null) return courseTables;

        //不存在则添加记录
        CourseTables addCourseTables = new CourseTables();
        addCourseTables.setChooseCourseId(chooseCourse.getId());
        addCourseTables.setCourseId(courseId);
        addCourseTables.setCourseType(chooseCourse.getOrderType());
        addCourseTables.setCompanyId(chooseCourse.getCompanyId());
        addCourseTables.setCourseName(chooseCourse.getCourseName());
        addCourseTables.setCreateDate(LocalDateTime.now());
        addCourseTables.setRemarks(chooseCourse.getRemarks());
        addCourseTables.setValidtimeStart(chooseCourse.getValidtimeStart());
        addCourseTables.setValidtimeEnd(chooseCourse.getValidtimeEnd());
        addCourseTables.setUserId(userId);

        courseTablesMapper.insert(addCourseTables);

        return addCourseTables;
    }

    /**
     * 查询课程表中是否已存在课程
     *
     * @param courseId 课程id
     * @param userId   用户id
     * @return 存在返回实体，不存在则返回null
     */
    private CourseTables checkCourseTables(Long courseId, String userId) {
        LambdaQueryWrapper<CourseTables> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTables::getChooseCourseId, courseId);
        queryWrapper.eq(CourseTables::getUserId, userId);
        return courseTablesMapper.selectOne(queryWrapper);
    }
}
