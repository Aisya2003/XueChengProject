package com.example.learning.service;

import com.example.base.model.RestResponse;

public interface ILearningCenterService {

    /**
     * 获取视频的播放地址
     *
     * @param userId      用户id
     * @param courseId    课程id
     * @param teachPlanId 教学计划id
     * @return 播放地址
     */
    RestResponse<String> getVideoUrl(String userId, Long courseId, Long teachPlanId, String mediaId);
}
