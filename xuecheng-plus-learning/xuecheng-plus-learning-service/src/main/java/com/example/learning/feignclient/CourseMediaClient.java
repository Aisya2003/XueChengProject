package com.example.learning.feignclient;

import com.example.base.model.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "media-api", fallbackFactory = CourseMediaClientFallbackFactory.class)
public interface CourseMediaClient {
    @GetMapping("/media/open/preview/{mediaId}")
    public RestResponse<String> getPlayUrlByMediaId(@PathVariable("mediaId") String mediaId);
}
