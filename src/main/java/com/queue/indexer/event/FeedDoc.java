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
    private String id;          // keyword
    private String docType;     // keyword
    private String parentId;    // keyword
    private String category;    // keyword
    private String title;
    private String content;
    private String authorId;    // keyword
    private List<String> tags;  // keyword (문자열 배열)
    private Instant createdAt;  // date
    private Instant updatedAt;  // date
    private Integer likeCount;
    private Integer commentCount;
    private Integer replyCount;
    private String lastEventId;
    private Integer eventVersion;


    public FeedDoc(String id, String docType, String category, String title, String content, String authorId,String parentId, Instant createdAt, Instant updatedAt, String lastEventId, Integer eventVersion) {
        this.id = id;
        this.docType = docType;
        this.category = category;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.parentId= parentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastEventId = lastEventId;
        this.eventVersion = eventVersion;
    }
}
