package com.example.learning.feignclient;

import com.example.content.model.po.CoursePublish;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CoursePublishClientFallbackFactory implements FallbackFactory<CoursePublishClient> {
    @Override
    public CoursePublishClient create(Throwable throwable) {
        return new CoursePublishClient() {
            @Override
            public CoursePublish getCoursePublishInfo(Long courseId) {
                log.error("调用内容管理服务发生熔断,{}", throwable.getMessage());
                return null;
            }
        };
    }
}
