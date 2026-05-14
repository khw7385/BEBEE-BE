package com.lgcns.bebee.common.data.db;

public interface ProcessedEventRepository {
    boolean tryAcquire(Long eventId, String eventType);
    void release(Long eventId, String eventType);
}
