package com.example.content.feignclient;


import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {

    //使用fallbackFactory可以获取异常信息
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            @Override
            public String upLoad(MultipartFile fileData, String folder, String objectName) {
                log.error("调用媒资管理服务发生熔断，异常信息：{}",throwable.getMessage());
                return null;
            }
        };
    }
}
