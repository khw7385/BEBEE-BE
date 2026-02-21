package com.lgcns.bebee.common.data.event.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.bebee.common.data.event.*;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
@RequiredArgsConstructor
public class SqsEventListener {
    private final ObjectMapper objectMapper;
    private final EventTypeMapper eventTypeMapper;
    private final EventHandlerRegistry handlerRegistry;
    private final ProcessedEventManager processedEventManager;

    @SqsListener("${app.sqs.queue-url}")
    public void handleEvent(@Payload String payload, @Header("eventType") String eventType) {
        EventEnvelope envelope = parseEnvelope(payload, eventType);

        if (!processedEventManager.tryAcquire(envelope.eventId(), envelope.eventType())) {
            log.info("중복 이벤트 스킵 - eventId: {}, eventType: {}", envelope.eventId(), envelope.eventType());
            return;
        }

        try {
            log.info("SQS 이벤트 처리 시작 - eventId: {}, eventType: {}", envelope.eventId(), envelope.eventType());
            processEvent(envelope);
            log.info("SQS 이벤트 처리 완료 - eventId: {}, eventType: {}", envelope.eventId(), envelope.eventType());
        } catch (Exception e) {
            processedEventManager.release(envelope.eventId());
            log.error("SQS 이벤트 처리 실패 - eventId: {}, eventType: {}", envelope.eventId(), envelope.eventType(), e);
            throw e;
        }
    }

    private EventEnvelope parseEnvelope(String payload, String eventType) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            Long eventId = root.get("eventId").asLong();
            Class<? extends DomainEvent> eventClass = eventTypeMapper.getClass(eventType);
            DomainEvent event = objectMapper.treeToValue(root.get("event"), eventClass);

            return new EventEnvelope(eventId, eventType, event, null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 페이로드 역직렬화 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> void processEvent(EventEnvelope envelope) {
        EventHandler<T> handler = (EventHandler<T>) handlerRegistry.getHandler(envelope.event().getClass());
        handler.handle((T) envelope.event());
    }
}
