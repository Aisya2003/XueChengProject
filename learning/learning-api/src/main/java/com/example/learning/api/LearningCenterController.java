package com.example.learning.api;

import com.example.base.model.RestResponse;
import com.example.learning.service.ILearningCenterService;
import com.example.learning.util.GetUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LearningCenterController {
    private final ILearningCenterService learningCenterService;

    @Autowired
    public LearningCenterController(ILearningCenterService learningCenterService) {
        this.learningCenterService = learningCenterService;
    }

    //获取学习视频
    @GetMapping("/open/learn/getvideo/{courseId}/{teachPlanId}/{mediaId}")
    public RestResponse<String> getLearningVideo(
            @PathVariable("courseId") Long courseId,
            @PathVariable("teachPlanId") Long teachPlanId,
            @PathVariable("mediaId") String mediaId
    ) {
        GetUser.XcUser user = GetUser.getUser();
        String userId = null;
        if (user != null) userId = user.getId();

        return learningCenterService.getVideoUrl(userId, courseId, teachPlanId, mediaId);
    }
}
