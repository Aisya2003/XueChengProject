package com.example.learning.service.impl;

import com.example.base.constant.Dictionary;
import com.example.base.exception.BusinessException;
import com.example.base.model.RestResponse;
import com.example.content.model.po.CoursePublish;
import com.example.learning.feignclient.CourseMediaClient;
import com.example.learning.feignclient.CoursePublishClient;
import com.example.learning.model.dto.CourseTablesDto;
import com.example.learning.service.IChooseCourseService;
import com.example.learning.service.ILearningCenterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LearningCenterServiceImpl implements ILearningCenterService {
    private final IChooseCourseService chooseCourseService;
    private final CourseMediaClient courseMediaClient;
    private final CoursePublishClient coursePublishClient;

    @Autowired
    public LearningCenterServiceImpl(IChooseCourseService chooseCourseService, CourseMediaClient courseMediaClient, CoursePublishClient coursePublishClient) {
        this.chooseCourseService = chooseCourseService;
        this.courseMediaClient = courseMediaClient;
        this.coursePublishClient = coursePublishClient;
    }

    @Override
    public RestResponse<String> getVideoUrl(String userId, Long courseId, Long teachPlanId, String mediaId) {
        //判断课程信息
        CoursePublish coursePublishInfo = coursePublishClient.getCoursePublishInfo(courseId);
        if (coursePublishInfo == null) BusinessException.cast("课程不存在！");
        //判断用户是否登录
        if (StringUtils.isEmpty(userId)) {
            //免费课程可以直接学习
            String chargeType = coursePublishInfo.getCharge();
            if (Dictionary.COURSE_FREE.getCode().equals(chargeType))
                return courseMediaClient.getPlayUrlByMediaId(mediaId);
        }
        //判断学习资格
        CourseTablesDto learningQualification = chooseCourseService.getLearningQualification(userId, courseId);
        String learnStatus = learningQualification.getLearnStatus();
        if (Dictionary.LEARNING_QUA_NORMAL.getCode().equals(learnStatus))
            return courseMediaClient.getPlayUrlByMediaId(mediaId);
        if (Dictionary.LEARNING_QUA_EXPIRED.getCode().equals(learnStatus))
            return RestResponse.validfail("您的选课已过期，请重新申请学习资格");


        return RestResponse.validfail("请先购买课程再学习");
    }
}
