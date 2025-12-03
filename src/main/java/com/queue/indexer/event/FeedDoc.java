package com.queue.indexer.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedDoc {
    private String id;            // feedId
    private String title;
    private String content;       // HTML or text
    private String authorId;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    private String lastEventId;   // 멱등성 처리용
}
