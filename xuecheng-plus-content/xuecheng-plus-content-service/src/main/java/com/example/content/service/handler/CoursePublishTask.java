package com.example.content.service.handler;

import com.alibaba.fastjson.JSON;
import com.example.base.exception.BusinessException;
import com.example.content.model.po.CoursePublish;
import com.example.content.service.ICoursePublishService;
import com.example.messagesdk.model.po.MqMessage;
import com.example.messagesdk.service.MessageProcessAbstract;
import com.example.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

import static com.example.base.constant.RedisConstant.COURSE_QUERY_PREFIX;

@Component
@Slf4j
public class CoursePublishTask extends MessageProcessAbstract {
    private final ICoursePublishService coursePublishService;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public CoursePublishTask(ICoursePublishService coursePublishService, StringRedisTemplate stringRedisTemplate) {
        this.coursePublishService = coursePublishService;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    //执行任务
    @XxlJob("CoursePublishHandler")
    public void coursePublishHandler() throws Exception {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        process(shardIndex, shardTotal, "course_publish", 5, 60);
    }

    /**
     * 从mq-message表中获取任务，开始执行
     *
     * @param mqMessage 执行任务内容
     * @return 执行结果
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        //businessKey约定为课程ID
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());
        log.debug("开始执行任务，课程ID：{}", courseId);
        //课程静态化，静态页面上传MinIO
        generateCourseHtmlAndUploadToMinIO(mqMessage, courseId);

        //上传课程信息到ElasticSearch
        saveCourseIndex(mqMessage, courseId);

        //上传课程信息到Redis
        saveCourseToRedis(mqMessage, courseId);

        //完成上传任务
        int completed = getMqMessageService().completed(mqMessage.getId());
        return completed > 0;
    }

    /**
     * 保存发布课程到Redis
     *
     * @param mqMessage 消息
     * @param courseId  课程ID
     */
    private void saveCourseToRedis(MqMessage mqMessage, Long courseId) {
        CoursePublish course = coursePublishService.getCoursePublishByCourseId(courseId);
        stringRedisTemplate.opsForValue().set(COURSE_QUERY_PREFIX + courseId, JSON.toJSONString(course));
        getMqMessageService().completedStageThree(mqMessage.getId());
    }

    /**
     * 创建课程在ElasticSearch上的索引
     *
     * @param mqMessage 消息
     * @param courseId  课程Id
     */
    private void saveCourseIndex(MqMessage mqMessage, Long courseId) {
        //判断第一阶段任务是否已完成
        Long id = mqMessage.getId();
        if (id == null) {
            BusinessException.cast("消息不存在！");
        }
        //获取MqMessageService注入
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageTwo = mqMessageService.getStageTwo(id);
        if (stageTwo != 0) {
            log.debug("上传课程索引阶段任务已完成！{}", mqMessage);
            return;
        }

        coursePublishService.saveCourseIndex(courseId);

        mqMessageService.completedStageTwo(id);

    }

    /**
     * 生成静态页面,上传MinIO
     *
     * @param mqMessage 消息表
     * @param courseId  课程ID
     */
    private void generateCourseHtmlAndUploadToMinIO(MqMessage mqMessage, Long courseId) {
        //判断第一阶段任务是否已完成
        Long id = mqMessage.getId();
        if (id == null) {
            BusinessException.cast("消息不存在！");
        }
        //获取MqMessageService注入
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageOne = mqMessageService.getStageOne(id);
        if (stageOne != 0) {
            log.debug("静态化页面阶段任务已完成！{}", mqMessage);
            return;
        }

        //生成静态页面
        File htmlFile = coursePublishService.generateHtml(courseId);
        if (htmlFile == null) {
            BusinessException.cast("静态化失败！");
        }
        //上传MinIo
        coursePublishService.uploadHTMLToMinIo(htmlFile, courseId);
        mqMessageService.completedStageOne(id);
    }
}
