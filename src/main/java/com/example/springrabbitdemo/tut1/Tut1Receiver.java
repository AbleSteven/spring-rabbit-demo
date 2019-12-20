package com.example.springrabbitdemo.tut1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class Tut1Receiver {
    private Logger logger = LogManager.getLogger(Tut1Receiver.class);
    @Value("${server.port}")
    private String port;

    // 此处是注解监听器的写法：
    // @QueueBinding 表示绑定queue 和 exchange ，括号里面配置的是对应的 queue 和 exchange
    //     value = @Queue 配置的是 queue 的名称，和相关属性（durable = "true" 表示把queue持久化），
    //       配置的 queue 如果 rabbit sever 中不存在则自动创建，若存在，则不执行任何动作；和在配置类里
    //       声明 queue 效果一样
    //     exchange = @Exchange 配置的是与 queue 绑定的 exchange 名称；等同于配置类里声明 exchange
    //     key 配置的是 bindingKey ，此处应该和 对应的投递消息的方法中使用的 routingKey 一致，否则接受
    //         不到消息。
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "dlx_queue", durable = "true"),
                    exchange = @Exchange(value = "dlx_exchange"),
                    key = "routingKey"
            )
    )
    // 监听方法只能有一个 @Payload 参数，否则无法自动转换成相应的类型（此处相应的类型是 Date ）
    // 监听方法顺利执行后（即 无异常抛出），框架会自动执行 channel.basicAck 方法。
    // 一旦监听方法抛出任何异常，框架就会自动执行 channel.basicReject 方法。
    public void getDate(@Payload Date date) {
        Date now = new Date();

        try {
            Thread.sleep(1000);
        } catch (Throwable e) {
            logger.error(e);
        }

        logger.info("[x] " + port + ": Get date : " + date.getTime() + " ---- Get now : " + now.getTime());
        /*logger.info("[x] Date subtract Now : " + (now.getTime() - date.getTime()));*/

    }

}
