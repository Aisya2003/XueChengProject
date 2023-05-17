package com.example.learning.feignclient;

import com.example.base.model.RestResponse;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CourseMediaClientFallbackFactory implements FallbackFactory<CourseMediaClient> {
    @Override
    public CourseMediaClient create(Throwable throwable) {
        return new CourseMediaClient() {
            @Override
            public RestResponse<String> getPlayUrlByMediaId(String mediaId) {
                log.error("调用媒资管理服务发生熔断,{}", throwable.getMessage());
                return null;
            }
        };
    }
}
