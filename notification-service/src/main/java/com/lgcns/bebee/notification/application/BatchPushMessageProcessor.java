package com.lgcns.bebee.notification.application;

import com.lgcns.bebee.notification.application.client.PushMessageBuffer;
import com.lgcns.bebee.notification.application.client.PushNotificationClient;
import com.lgcns.bebee.notification.core.dto.PushMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class BatchPushMessageProcessor {
    private final PushMessageBuffer buffer;
    private final PushNotificationClient client;

    private static final int MAX_BATCH_SIZE = 100;

    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    public synchronized void add(String token, String title, String body, Map<String, String> data){
        buffer.add(token, title, body, data);
        if(buffer.size() >= MAX_BATCH_SIZE){
            flush();
        }
    }

    public synchronized void flush(){
        if(buffer.isEmpty()) return;

        List<PushMessageDTO> messages = buffer.drain(MAX_BATCH_SIZE);

        if(!messages.isEmpty()){
            client.sendAllMessagesAsync(messages);
        }
    }

    public void addLockFree(String token, String title, String body, Map<String, String> data) {
        buffer.add(token, title, body, data);
        int size = incrementCount();

        if (shouldFlush(size)) {
            tryFlush();
        }
    }

    public void flushLockFree() {
        List<PushMessageDTO> messages = buffer.drain(MAX_BATCH_SIZE);
        decrementCount(messages.size());

        if (!messages.isEmpty()) {
            client.sendAllMessagesAsync(messages);
        }
    }

    private int incrementCount() {
        return counter.incrementAndGet();
    }

    private void decrementCount(int count) {
        counter.addAndGet(-count);
    }

    private boolean shouldFlush(int currentSize) {
        return currentSize >= MAX_BATCH_SIZE;
    }

    private void tryFlush() {
        if (flushing.compareAndSet(false, true)) {
            try {
                flushLockFree();
            } finally {
                flushing.set(false);
            }
        }
    }
}
