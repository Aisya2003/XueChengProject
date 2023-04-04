package com.example.learning.feignclient;

import com.example.content.model.po.CoursePublish;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(value = "content-api", fallbackFactory = CoursePublishClientFallbackFactory.class)
public interface CoursePublishClient {
    @GetMapping("/content/inner/coursepublish/{courseId}")
    @ResponseBody
    public CoursePublish getCoursePublishInfo(@PathVariable("courseId") Long courseId);
}
