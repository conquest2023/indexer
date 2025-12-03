package com.queue.indexer.buffer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BulkBuffer {
    private final EsWriter esWriter;

    private final int flushSize = 500;
    private final long flushMillis = 1000;

    private final List<FeedEvent> queue = new ArrayList<>();
    private long lastFlushAt = System.currentTimeMillis();

    public synchronized void add(FeedEvent evt) {
        queue.add(evt);
    }

    public synchronized boolean shouldFlush() {
        return queue.size() >= flushSize || (System.currentTimeMillis() - lastFlushAt) >= flushMillis;
    }

    public synchronized void flush() {
        if (queue.isEmpty()) return;
        var batch = new ArrayList<>(queue);
        queue.clear();
        esWriter.bulkWrite(batch);
        lastFlushAt = System.currentTimeMillis();
    }
}
