package com.lgcns.bebee.common.data.event.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.bebee.common.data.db.ProcessedEventRepository;
import com.lgcns.bebee.common.data.event.*;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsEventListener implements EventListener {
    private final ObjectMapper objectMapper;
    private final EventDispatcher eventDispatcher;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @SqsListener("${app.sqs.queue-url}")
    public void handleEvent(@Payload String payload, @Header("eventType") String eventType) {
        EventEnvelope envelope = parseEnvelope(payload, eventType);

        if (!processedEventRepository.tryAcquire(envelope.eventId(), envelope.eventType())) {
            log.info("중복 이벤트 스킵 - eventId: {}, eventType: {}", envelope.eventId(), envelope.eventType());
            return;
        }

        try {
            log.info("SQS 이벤트 처리 시작 - eventId: {}, eventType: {}", envelope.eventId(), envelope.eventType());
            eventDispatcher.dispatch(envelope.event());
            log.info("SQS 이벤트 처리 완료 - eventId: {}, eventType: {}", envelope.eventId(), envelope.eventType());
        } catch (Exception e) {
            log.error("SQS 이벤트 처리 실패 - eventId: {}, eventType: {}", envelope.eventId(), envelope.eventType(), e);
            processedEventRepository.release(envelope.eventId(), envelope.eventType());
            throw e;
        }
    }

    private EventEnvelope parseEnvelope(String payload, String eventType) {
        try{
            return objectMapper.readValue(payload, EventEnvelope.class);
        }catch(JsonProcessingException e){
            log.error("이벤트 페이로드 역직렬화 실패 - eventType: {}", eventType, e);
            throw new IllegalArgumentException("이벤트 페이로드 역직렬화 실패", e);
        }
    }
}
