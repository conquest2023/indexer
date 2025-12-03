package com.queue.indexer.event;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class FeedPayload {
    String feedId;
    String title;
    String content;
    String authorId;
    List<String> tags;
    String visibility;
    Instant createdAt;
    Instant updatedAt;
}