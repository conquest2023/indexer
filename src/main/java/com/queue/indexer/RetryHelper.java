package com.queue.indexer;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetryHelper {
    private final AmqpTemplate amqp;
    @Value("${indexer.retry.maxAttempts}") int maxAttempts;
    @Value("${indexer.retry.delayMillis}") int delay;

//    public boolean isDuplicate(String eventId) {
//        // 간단 버전: eventId 없거나 null이면 통과
//        // 가능하면 Redis SET with TTL(예: 1일)로 recent events 저장
//        return eventId != null && Recent.contains(eventId);
//    }

    public void toRetryQueue(Message msg) {
        int attempt = (int) msg.getMessageProperties().getHeaders()
                .getOrDefault("x-retry", 0);
        if (attempt >= maxAttempts) { toDlq(msg); return; }

        MessageProperties props = msg.getMessageProperties();
        props.setExpiration(String.valueOf(delay)); // per-message TTL
        props.setHeader("x-retry", attempt + 1);
        amqp.send("feed.events", "feed.retry", msg); // retry.q로 라우팅
    }

    public void toDlq(Message msg) {
        amqp.send("feed.events", "feed.dlq", msg);
    }
}
