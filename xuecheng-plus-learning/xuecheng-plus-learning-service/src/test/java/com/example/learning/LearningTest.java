package com.example.learning;

import com.example.LearningApplication;
import com.example.content.model.po.CoursePublish;
import com.example.learning.feignclient.CoursePublishClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootTest(classes = LearningApplication.class)
@EnableFeignClients("com.example.learning.feignclient")
public class LearningTest {
    private final CoursePublishClient client;

    @Autowired
    public LearningTest(CoursePublishClient client) {
        this.client = client;
    }

    @Test
    void testFeignPublish() {
        CoursePublish coursePublishInfo = client.getCoursePublishInfo(2L);
        System.out.println("coursePublishInfo = " + coursePublishInfo);
    }
}
