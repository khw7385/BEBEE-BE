package com.lgcns.bebee.notification.infrastructure.buffer;

import com.lgcns.bebee.notification.application.client.PushMessageBuffer;
import com.lgcns.bebee.notification.core.dto.PushMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class InMemoryPushMessageBuffer implements PushMessageBuffer {
    private final ConcurrentLinkedQueue<PushMessageDTO> messageQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void add(String token, String title, String body, Map<String, String> data) {
        PushMessageDTO pending = new PushMessageDTO(token, title, body, data);
        messageQueue.add(pending);

        log.debug("메시지 큐에 추가됨: queueSize={}, token={}", messageQueue.size(), token);
    }

    @Override
    public List<PushMessageDTO> drain(int maxSize) {
        List<PushMessageDTO> result = new ArrayList<>();
        PushMessageDTO message;

        while (result.size() < maxSize && (message = messageQueue.poll()) != null) {
            result.add(message);
        }

        return result;
    }

    @Override
    public int size() {
        return messageQueue.size();
    }

    @Override
    public boolean isEmpty() {
        return messageQueue.isEmpty();
    }
}