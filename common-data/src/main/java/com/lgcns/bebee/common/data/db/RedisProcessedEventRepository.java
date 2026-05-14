package com.lgcns.bebee.common.data.db;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisProcessedEventRepository implements ProcessedEventRepository {
    private final RedisTemplate<String, String> redisTemplate;

    private static final int TTL_HOURS = 24;

    @Override
    public boolean tryAcquire(Long eventId, String eventType){
        String key = String.format("%s:%d", eventType, eventId);
        Boolean isSuccess = redisTemplate.opsForValue().setIfAbsent(key
                , "1", TTL_HOURS, TimeUnit.HOURS);
        return Boolean.TRUE.equals(isSuccess);
    }

    @Override
    public void release(Long eventId, String eventType) {
        String key = String.format("%s:%d", eventType, eventId);
        redisTemplate.delete(key);
    }
}
