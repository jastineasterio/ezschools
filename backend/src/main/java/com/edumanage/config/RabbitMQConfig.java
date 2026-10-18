package com.edumanage.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.queues.sms}")
    private String smsQueue;

    @Value("${app.queues.sms-dead-letter}")
    private String smsDeadLetterQueue;

    @Value("${app.queues.notifications}")
    private String notificationsQueue;

    @Value("${app.queues.reports}")
    private String reportsQueue;

    @Bean
    public Queue smsQueue() {
        return new Queue(smsQueue, true, false, false,
                java.util.Map.of("x-dead-letter-exchange", "edumanage.exchange", "x-dead-letter-routing-key", "sms.dead-letter"));
    }

    @Bean
    public Queue smsDeadLetterQueue() {
        return new Queue(smsDeadLetterQueue, true);
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue(notificationsQueue, true);
    }

    @Bean
    public Queue reportsQueue() {
        return new Queue(reportsQueue, true);
    }

    @Bean
    public TopicExchange edumanageExchange() {
        return new TopicExchange("edumanage.exchange");
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, TopicExchange edumanageExchange) {
        return BindingBuilder.bind(smsQueue).to(edumanageExchange).with("sms.#");
    }

    @Bean
    public Binding smsDeadLetterBinding(Queue smsDeadLetterQueue, TopicExchange edumanageExchange) {
        return BindingBuilder.bind(smsDeadLetterQueue).to(edumanageExchange).with("sms.dead-letter");
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange edumanageExchange) {
        return BindingBuilder.bind(notificationsQueue).to(edumanageExchange).with("notifications.#");
    }

    @Bean
    public Binding reportsBinding(Queue reportsQueue, TopicExchange edumanageExchange) {
        return BindingBuilder.bind(reportsQueue).to(edumanageExchange).with("reports.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
