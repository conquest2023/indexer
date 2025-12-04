package com.queue.indexer.event;

import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

@Data
@ToString
public class FeedPayload {
    private String id;          // keyword
    private String docType;     // keyword
    private String title;
    private String category;    // keyword
    private String content;
    private String authorId;    // keyword
    private String parentId;    // keyword
    private Instant createdAt;  // date
    private Instant updatedAt;  // date
    private Integer likeCount;
    private Integer commentCount;
    private Integer replyCount;
    private String lastEventId; // keyword
    private Integer eventVersion;
}