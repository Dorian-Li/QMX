package com.example.qmx.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String MODBUS_QUEUE = "modbus.raw.frame.queue";
    public static final String MODBUS_EXCHANGE = "modbus.raw.frame.exchange";
    public static final String MODBUS_ROUTING_KEY = "modbus.raw.frame";

    @Bean
    public Queue modbusQueue() {
        return QueueBuilder.durable(MODBUS_QUEUE).build();
    }

    @Bean
    public DirectExchange modbusExchange() {
        return new DirectExchange(MODBUS_EXCHANGE, true, false);
    }

    @Bean
    public Binding modbusBinding(Queue modbusQueue, DirectExchange modbusExchange) {
        return BindingBuilder.bind(modbusQueue).to(modbusExchange).with(MODBUS_ROUTING_KEY);
    }
}

