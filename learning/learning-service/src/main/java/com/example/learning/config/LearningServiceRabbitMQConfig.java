package com.example.learning.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
public class LearningServiceRabbitMQConfig implements ApplicationContextAware {

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
    //用于处理超过最大重试次数的消息
/*    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, "exchange", "routingkey");
    }
    */

}
