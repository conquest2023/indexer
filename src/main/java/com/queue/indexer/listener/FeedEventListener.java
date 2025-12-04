package com.queue.indexer.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queue.indexer.RetryHelper;
import com.queue.indexer.buffer.BulkBuffer;
import com.queue.indexer.event.FeedEvent;
import com.queue.indexer.exception.BadEventException;
import com.queue.indexer.exception.MappingException;
import com.queue.indexer.exception.RetryableEsException;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedEventListener {

    private final BulkBuffer buffer;
    private final RetryHelper retryHelper;
    private final ObjectMapper mapper;

    @RabbitListener(queues = "indexer.feed.q", containerFactory = "manualAckFactory", concurrency = "3")
    public void onMessage(Map<String, Object> event, Message msg, Channel ch) throws Exception {
        long tag = msg.getMessageProperties().getDeliveryTag();
        try {
            log.info(" eventType={}, retry={}",
                    event.get("type"),
                    msg.getMessageProperties().getHeaders().get("x-retry"));
            FeedEvent evt = mapper.convertValue(event, FeedEvent.class);
            buffer.add(evt);
            ch.basicAck(tag, false);

        } catch (RetryableEsException e) {
            log.warn("🔁 retryable -> retry.q : {}", e.getMessage());
            retryHelper.toRetryQueue(msg);
            ch.basicAck(tag, false);

        } catch (BadEventException | MappingException e) {
            log.error("💀 non-retryable -> DLQ : {}", e.getMessage());
            retryHelper.toDlq(msg);
            ch.basicAck(tag, false);

        } catch (Exception e) {
            log.error("❌ unexpected -> retry.q : {}", e.toString(), e);
            retryHelper.toRetryQueue(msg);
            ch.basicAck(tag, false);
        }
    }
}
