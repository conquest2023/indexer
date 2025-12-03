package com.queue.indexer.event;

import lombok.Data;

import java.time.Instant;

@Data
public class FeedEvent {
    String eventId;
    String type;
    String aggregateId;
    Long aggregateVersion;
    Instant occurredAt;
    Integer schemaVersion;
    String producer;
    FeedPayload payload;
}