package com.example.learning.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author Mr.M
 * @version 1.0
 * @description 消息队列配置
 * @date 2022/10/4 22:25
 */
@Configuration
public class LearningServiceRabbitMQConfig {

    //交换机
    public static final String NOTIFY_REPLY_FANOUT_EXCHANGE = "notify_reply_exchange";
    //监听支付结果通知队列
    public static final String NOTIFY_QUEUE = "notify_queue";
    //接收支付通知结果是否收到的队列
    public static final String NOTIFY_REPLY_QUEUE = "notify_reply_queue";
    public static final String MESSAGE_TYPE = "payResultNotify";

    //支付结果通知队列
    @Bean(name = NOTIFY_REPLY_QUEUE)
    public Queue notifyReplyQueue() {
        return QueueBuilder.durable(NOTIFY_REPLY_QUEUE).build();
    }

    @Bean(name = NOTIFY_QUEUE)
    public Queue notifyQueue() {
        return QueueBuilder.durable(NOTIFY_QUEUE).build();
    }

    @Bean(name = NOTIFY_REPLY_FANOUT_EXCHANGE)
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(NOTIFY_REPLY_FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public Binding binding(
            @Qualifier(NOTIFY_REPLY_QUEUE) Queue queue,
            @Qualifier(NOTIFY_REPLY_FANOUT_EXCHANGE) FanoutExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange);
    }


}
