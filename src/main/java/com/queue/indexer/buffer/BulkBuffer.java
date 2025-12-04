package com.queue.indexer.buffer;

import com.queue.indexer.event.FeedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkBuffer {
    private final EsWriter esWriter;

    private final int flushSize = 5;
    private final long flushMillis = 1000;

    private final List<FeedEvent> queue = new ArrayList<>();
    private long lastFlushAt = System.currentTimeMillis();

    public synchronized void add(FeedEvent evt) {
        queue.add(evt);
        log.info("buffer size={}", queue.size());
        if (queue.size() >= flushSize)
            flush();
    }
    public synchronized boolean shouldFlush() {
        return queue.size() >= flushSize || (System.currentTimeMillis() - lastFlushAt) >= flushMillis;
    }

    public synchronized void flush() {
        if (queue.isEmpty())
            return;
        var batch = new ArrayList<>(queue);
        queue.clear();
        log.info("🟩bulk flush start size={}", batch.size());
        esWriter.bulkWrite(batch);
        lastFlushAt = System.currentTimeMillis();
        log.info("🟩bulk flush done");
    }
}
