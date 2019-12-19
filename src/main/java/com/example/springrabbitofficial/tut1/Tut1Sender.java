package com.example.springrabbitofficial.tut1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Tut1Sender {
    @Autowired
    private RabbitTemplate template;

    @Autowired
    private Exchange topicExchange;

    private Logger logger = LogManager.getLogger(Tut1Sender.class);

    private AtomicLong count = new AtomicLong(0);

    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void send() {
        Date message = new Date();
        CorrelationData correlationData = new CorrelationData();

        // 向消息队列中投递消息时，如果失败执行的降级处理，建议两个方法执行相同的动作。
        correlationData.getFuture().addCallback(
                new ListenableFutureCallback<CorrelationData.Confirm>() {
                    // 报错时，执行这个方法。
                    @Override
                    public void onFailure(Throwable throwable) {
                        logger.error(throwable);
                    }

                    // 不报错，但是消息投递失败时，执行这个方法
                    @Override
                    public void onSuccess(CorrelationData.Confirm confirm) {
                        logger.info("Is ack: " + confirm.isAck());
                    }
                });

        // MyMessagePostProcessor 是自定义的，旨在给 message 设置过期时间。
        // 通过配置 CorrelationData 实现 Publisher Confirm 。
        template.convertAndSend(
                topicExchange.getName(),
                "routingKey",
                message,
                new MyMessagePostProcessor(5000L),
                correlationData);
    }

}
