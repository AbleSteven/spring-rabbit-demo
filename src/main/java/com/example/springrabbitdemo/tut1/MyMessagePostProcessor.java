package com.example.springrabbitdemo.tut1;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

public class MyMessagePostProcessor implements MessagePostProcessor {
    private final Long ttl;

    public MyMessagePostProcessor(Long ttl) {
        this.ttl = ttl;
    }

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        message.getMessageProperties().setExpiration(ttl.toString());

        return message;
    }
}
