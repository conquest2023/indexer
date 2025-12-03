package com.queue.indexer.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    public TopicExchange feedExchange() {
        return new TopicExchange("feed.events", true, false);
    }

    @Bean
    public Queue mainQ() {
        return QueueBuilder.durable("indexer.feed.q")
                .withArgument("x-dead-letter-exchange", "feed.events")
                .withArgument("x-dead-letter-routing-key", "feed.dlq")
                .build();
    }

    @Bean
    public Queue retryQ() {
        return QueueBuilder.durable("indexer.feed.retry.q")
                .withArgument("x-message-ttl", 10_000)               // 10초 지연
                .withArgument("x-dead-letter-exchange", "feed.events")
                .withArgument("x-dead-letter-routing-key", "feed.index")
                .build();
    }

    @Bean
    public Queue dlq() {
        return QueueBuilder.durable("indexer.feed.dlq").build();
    }

    @Bean
    public Binding b1() {
        return BindingBuilder.bind(mainQ()).to(feedExchange()).with("feed.index");
    }

    @Bean
    public Binding b2() {
        return BindingBuilder.bind(retryQ()).to(feedExchange()).with("feed.retry");
    }

    @Bean
    public Binding b3() {
        return BindingBuilder.bind(dlq()).to(feedExchange()).with("feed.dlq");
    }
}