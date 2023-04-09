package com.example.learning.service.listener;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.base.constant.Dictionary;
import com.example.learning.config.LearningServiceRabbitMQConfig;
import com.example.learning.mapper.ChooseCourseMapper;
import com.example.learning.model.po.ChooseCourse;
import com.example.learning.service.impl.ChooseCourseServiceImpl;
import com.example.messagesdk.mapper.MqMessageMapper;
import com.example.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.Objects;

@Component
@Slf4j
public class NotifyListener {
    private final ChooseCourseMapper chooseCourseMapper;
    private final ChooseCourseServiceImpl chooseCourseService;
    private final MqMessageMapper mqMessageMapper;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public NotifyListener(ChooseCourseMapper chooseCourseMapper, ChooseCourseServiceImpl chooseCourseService, MqMessageMapper mqMessageMapper, RabbitTemplate rabbitTemplate) {
        this.chooseCourseMapper = chooseCourseMapper;
        this.chooseCourseService = chooseCourseService;
        this.mqMessageMapper = mqMessageMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = {LearningServiceRabbitMQConfig.NOTIFY_QUEUE})
    public void notifyListener(Message message) {
        //获取消息
        String mqMessageJson = new String(message.getBody());
        MqMessage mqMessage = JSON.parseObject(mqMessageJson, MqMessage.class);

        //判断消息类型
        if (!mqMessage.getMessageType().equals(LearningServiceRabbitMQConfig.MESSAGE_TYPE)) return;
        if (!mqMessage.getBusinessKey2().equals(Dictionary.ORDER_TYPE_COURSE.getCode())) return;

        //获取选课记录
        Long chooseCourseId = Long.valueOf(mqMessage.getBusinessKey1());
        ChooseCourse chooseCourse = chooseCourseMapper.selectById(chooseCourseId);
        if (Objects.isNull(chooseCourse)) {
            log.info("支付结果通知的选课记录为空");
            //回复
            sendReplyMessage(mqMessage);
            return;
        }

        //更新选课记录
        ChooseCourse updateChooseCourse = new ChooseCourse();
        updateChooseCourse.setStatus(Dictionary.CHOOSE_COURSE_STATUS_SUCCESS.getCode());
        LambdaQueryWrapper<ChooseCourse> updateMapper = new LambdaQueryWrapper<>();
        updateMapper.eq(ChooseCourse::getId, chooseCourseId);
        chooseCourseMapper.update(updateChooseCourse, updateMapper);
        log.info("选课状态更新完成,chooseCourseId:{}", chooseCourseId);

        //将选课记录插入用户课表
        //获取最新记录
        chooseCourse = chooseCourseMapper.selectById(chooseCourseId);
        chooseCourseService.addCourseTable(chooseCourse);
        log.info("插入课表成功");

        //通知删除消息
        sendReplyMessage(mqMessage);
    }

    /**
     * 收到消息处理结束后，向reply队列发送消息
     *
     * @param mqMessage 消息
     */
    private void sendReplyMessage(MqMessage mqMessage) {
        Long id = mqMessage.getId();
        //参数为全局消息id
        CorrelationData correlationData = new CorrelationData(id.toString());
        //构建消息
        Message message = MessageBuilder.withBody(JSON.toJSONBytes(mqMessage)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        //设置消息的回调方法
        correlationData.getFuture().addCallback(
                new SuccessCallback<CorrelationData.Confirm>() {
                    @Override
                    public void onSuccess(CorrelationData.Confirm confirm) {
                        if (confirm.isAck()) log.info("消息成功发送到交换机:{}", JSON.toJSONString(mqMessage));
                        log.error("消息发送失败：{}", JSON.toJSONString(mqMessage));
                    }
                }, new FailureCallback() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        //发送异常
                        log.error("消息发送异常：{}，{}", JSON.toJSONString(mqMessage), throwable.getMessage());
                    }
                });

        //发送
        rabbitTemplate.convertAndSend(
                LearningServiceRabbitMQConfig.NOTIFY_REPLY_FANOUT_EXCHANGE,
                "",
                message,
                correlationData
        );

    }
}
