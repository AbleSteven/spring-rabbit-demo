package com.example.springrabbitdemo.tut1;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class Tut1Config {

    // 正常exchange ，此处把exchange定义为 topic 类型
    @Bean
    public Exchange topicExchange() {
        return ExchangeBuilder.
                topicExchange("topicExchange").
                durable(true).build();
    }

    // 死信exchange 。其实这个也是正常的 direct exchange ，
    // 只是被 正常queue 配置成了 死信exchange 而已。
    @Bean
    public Exchange dlxExchange() {
        return ExchangeBuilder.directExchange("dlx_exchange").durable(true).build();
    }

    // 正常queue
    @Bean
    public Queue topicQueue() {

        // 此处配置的是该 正常queue 对应的 死信exchange 该queue中，设置了 ttl 的message，到期报废后，
        // 会被转发到对应的 死信exchange ，然后会被投递到
        // 与 死信exchange 绑定的 死信queue 中
        // 通过监听 死信队列 获取报废的消息
        // *****************************************
        // 延迟队列就是这样实现的；给message设置过期时间，然后投递到 正常queue 中，
        // 此 正常queue 没有 consumer 消费 message ，
        // message 会一直呆在此 正常queue 中，当到过期时间后，被重新投递到对应的
        // 死信queue（又名 延迟队列），被重新投递的 message 使用的 routingKey 与其第一次被投递时使用的
        // routingKey 是一致的。
        // 死信队列（即 延迟队列 ）有 consumer ，所以 message 被正常消费。
        Map<String,Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dlx_exchange");

        return QueueBuilder.durable("topicQueue").withArguments(args).build();
    }

    // 把 正常queue 绑定到 正常exchange ，此处的 bindingKey 为 # ，
    // 表示此queue接受所有投递到 topicExchange 的消息。
    @Bean
    public Binding topicBinding() {
        return BindingBuilder.
                bind(topicQueue()).
                to(topicExchange()).
                with("#").noargs();
    }

    @Bean
    public Connection connection(CachingConnectionFactory connectionFactory) {
        return connectionFactory.createConnection();
    }

    // 此处把 MessageConverter 配置成这个，比较方便。
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter jackson2JsonMessageConverter =
                new Jackson2JsonMessageConverter();

        return jackson2JsonMessageConverter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory factory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(factory);

        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(500);
        exponentialBackOffPolicy.setMultiplier(10.0);
        exponentialBackOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        rabbitTemplate.setRetryTemplate(retryTemplate);

        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());

        return rabbitTemplate;
    }

    @Bean
    public DirectRabbitListenerContainerFactory directRabbitListenerContainerFactory(
            CachingConnectionFactory connectionFactory) {
        DirectRabbitListenerContainerFactory factory =
                new DirectRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setPrefetchCount(1);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(jackson2JsonMessageConverter());

        return factory;
    }

}
