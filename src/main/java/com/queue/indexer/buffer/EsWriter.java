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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EsWriter {
    private final ElasticsearchClient es;

    @Value("${elasticsearch.index.writeAlias}")
    String writeAlias;

    public void bulkWrite(List<FeedEvent> events) {
        List<BulkOperation> ops = new ArrayList<>();
        for (var e : events) {
            switch (e.getType()) {
                case "feed.created":
                case "feed.updated":
                    FeedDoc doc = toDoc(e.getPayload(), e.getEventId());
                    ops.add(BulkOperation.of(b -> b.index(idx -> idx
                            .index(writeAlias)                          // alias에만 쓰기
                            .id(doc.getId())
                            .document(doc)
//                            .version(e.getVersion() == null ? null : e.getVersion().longValue())
//                            .versionType(e.getVersion() == null ? null : VersionType.External)
                    )));
                    break;
                case "feed.deleted":
                    ops.add(BulkOperation.of(b -> b.delete(d -> d
                            .index(writeAlias).id(e.getAggregateId())
//                            .version(e.getVersion() == null ? null : e.getVersion().longValue())
//                            .versionType(e.getVersion() == null ? null : VersionType.External)
                    )));
                    break;
                default:
                    throw new BadEventException("Unknown type: " + e.getType());
            }
        }

        try {
            var resp = es.bulk(b -> b.operations(ops));
            if (resp.errors()) {
                // 항목별 실패 분해: 400류는 DLQ, 429/5xx는 재시도 유도
                EsBulkErrorClassifier.handle(resp);
            }
        } catch (IOException ioe) {
            throw new RetryableEsException(ioe);
        }
    }

    private FeedDoc toDoc(FeedPayload p, String lastEventId) {
        return new FeedDoc(p.getFeedId(), p.getTitle(), p.getContent(),
                p.getAuthorId(), p.getTags(), p.getCreatedAt(), p.getUpdatedAt(), lastEventId);
    }
}
