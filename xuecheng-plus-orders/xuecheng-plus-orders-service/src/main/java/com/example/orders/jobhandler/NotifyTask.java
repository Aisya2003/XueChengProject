package com.example.orders.jobhandler;

import com.alibaba.fastjson.JSON;
import com.example.messagesdk.model.po.MqMessage;
import com.example.messagesdk.service.MessageProcessAbstract;
import com.example.orders.config.OrderServiceRabbitMQConfig;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class NotifyTask extends MessageProcessAbstract {
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public NotifyTask(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    //任务调度
    @XxlJob("notifyResultHandler")
    public void notifyResultHandler() {
        int shardTotal = XxlJobHelper.getShardTotal();
        int shardIndex = XxlJobHelper.getShardIndex();

        //执行本类任务
        process(shardIndex, shardTotal, OrderServiceRabbitMQConfig.MESSAGE_TYPE, 5, 60);
    }

    //执行方法
    @Override
    public boolean execute(MqMessage mqMessage) {
        //调用rabbit MQ发送消息
        sendNotifyMessage(mqMessage);
        //由消息队列来执行消息的完成处理，将消息移除数据库
        return false;
    }

    /**
     * 调用mq发送消息
     *
     * @param mqMessage 消息记录
     */
    private void sendNotifyMessage(MqMessage mqMessage) {
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
                OrderServiceRabbitMQConfig.NOTIFY_FANOUT_EXCHANGE,
                "",
                message,
                correlationData
        );
    }
}
