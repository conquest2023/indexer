package com.queue.indexer.buffer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.queue.indexer.event.FeedDoc;
import com.queue.indexer.event.FeedEvent;
import com.queue.indexer.event.FeedPayload;
import com.queue.indexer.exception.BadEventException;
import com.queue.indexer.exception.EsBulkErrorClassifier;
import com.queue.indexer.exception.RetryableEsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class EsWriter {

    private final ElasticsearchClient es;

    @Value("${elasticsearch.index.writeAlias}")
    String writeAlias;

    public void bulkWrite(List<FeedEvent> events) {
        List<BulkOperation> ops = new ArrayList<>();

        for (FeedEvent e : events) {
            // type 예: "feed.created" | "comment.updated" | "reply.deleted"
            ParsedType t = parseType(e.getType());
            FeedPayload p = e.getPayload();

            switch (t.action) {
                case "created" -> {
                    FeedDoc doc = toDoc(p, e.getEventId());
                    addIndexOp(ops, doc);
                    // 필요하면 부모 카운터 증가(comment/reply) 스크립트도 여기에서 추가
                }
                case "updated" -> {
                    FeedDoc doc = toDoc(p, e.getEventId());
                    addUpsertOp(ops, doc);
                }
                case "deleted" -> {
                    String id = String.valueOf(e.getAggregateId());
                    addDeleteOp(ops, id);
                    // 필요하면 부모 카운터 감소(comment/reply) 스크립트도 여기에서 추가
                }
                default -> throw new BadEventException("Unknown action: " + t.action);
            }
        }
        try {
            var resp = es.bulk(b -> b.operations(ops));
            if (Boolean.TRUE.equals(resp.errors())) {
                EsBulkErrorClassifier.handle(resp);
            }
        } catch (IOException ioe) {
            throw new RetryableEsException(ioe);
        }
    }



    private void addIndexOp(List<BulkOperation> ops, FeedDoc doc) {
        ops.add(BulkOperation.of(b -> b.index(i -> i
                .index(writeAlias)
                .id(doc.getId())
                .document(doc)
        )));
    }

    /** update with doc_as_upsert(true) -> 부분 업데이트 + 없으면 생성 */
    private void addUpsertOp(List<BulkOperation> ops, FeedDoc doc) {
        ops.add(BulkOperation.of(b ->
                b.update(u -> u
                .index(writeAlias)
                .id(doc.getId())
                .action(a -> a.doc(doc).docAsUpsert(true))
        )));
    }
    private void addDeleteOp(List<BulkOperation> ops, String id) {
        ops.add(BulkOperation.of(b ->
                b.delete(d -> d
                .index(writeAlias)
                .id(id)
        )));
    }

    private record ParsedType(String kind, String action) {

    }

    private ParsedType parseType(String type) {

        int dot = type == null ? -1 : type.indexOf('.');
        if (dot <= 0 || dot >= type.length() - 1) {
            throw new BadEventException("Invalid event type: " + type);
        }
        String kind = type.substring(0, dot);
        String action = type.substring(dot + 1);
        // kind는 "feed" | "comment" | "reply" 중 하나라고 가정 (검증 필요하면 추가)
        return new ParsedType(kind, action);
    }

    private FeedDoc toDoc(FeedPayload p, String lastEventId) {
        log.info(p.toString());
        return new FeedDoc(
                p.getId(),
                p.getDocType(),
                p.getTitle(),
                p.getCategory(),
                p.getContent(),
                p.getAuthorId(),
                p.getParentId(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                lastEventId,
                p.getEventVersion());
    }
}