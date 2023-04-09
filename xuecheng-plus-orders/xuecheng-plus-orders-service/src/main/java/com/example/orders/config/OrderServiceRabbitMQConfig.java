package com.example.orders.config;

import com.alibaba.fastjson.JSON;
import com.example.messagesdk.model.po.MqMessage;
import com.example.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OrderServiceRabbitMQConfig implements ApplicationContextAware {
    public static final String NOTIFY_FANOUT_EXCHANGE = "notify_exchange";
    //监听支付结果通知队列
    public static final String NOTIFY_REPLY_QUEUE = "notify_reply_queue";
    public static final String NOTIFY_QUEUE = "notify_queue";
    public static final String NOTIFY_BINDING = "notify_pay_binding";

    //消息类型
    public static final String MESSAGE_TYPE = "payResultNotify";

    //构建队列
    @Bean(name = NOTIFY_QUEUE)
    public Queue notifyQueue() {
        return QueueBuilder.durable(NOTIFY_QUEUE).build();
    }

    //构建交换机
    @Bean(name = NOTIFY_FANOUT_EXCHANGE)
    public FanoutExchange notifyFanoutExchange() {
        return new FanoutExchange(NOTIFY_FANOUT_EXCHANGE, true, false);
    }

    //构建交换机和队列的绑定
    @Bean(name = NOTIFY_BINDING)
    public Binding notifyBinding(
            @Qualifier(NOTIFY_QUEUE) Queue queue,
            @Qualifier(NOTIFY_FANOUT_EXCHANGE) FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queue).to(fanoutExchange);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //保证消息的可靠性，当消息从交换机发送到队列时失败会调用此方法
        //获取mq
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        //设置失败处理方法
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息投递到队列失败：响应码：{}，错误原因：{}，交换机：{}，routingKey：{}，消息：{}",
                    replyCode, replyText, exchange, routingKey, message);
        });
    }
}
