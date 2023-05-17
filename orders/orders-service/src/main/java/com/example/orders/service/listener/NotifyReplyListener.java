package com.example.orders.service.listener;

import com.alibaba.fastjson.JSON;
import com.example.messagesdk.model.po.MqMessage;
import com.example.messagesdk.service.MqMessageService;
import com.example.orders.config.OrderServiceRabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotifyReplyListener {
    private final MqMessageService mqMessageService;

    @Autowired
    public NotifyReplyListener(MqMessageService mqMessageService) {
        this.mqMessageService = mqMessageService;
    }

    @RabbitListener(queues = {OrderServiceRabbitMQConfig.NOTIFY_REPLY_QUEUE})
    public void notifyReplyListener(String message) {
        log.info("收到支付结果响应消息，{}", message);
        MqMessage mqMessage = JSON.parseObject(message, MqMessage.class);
        //处理完成的消息
        mqMessageService.completed(mqMessage.getId());
    }
}
