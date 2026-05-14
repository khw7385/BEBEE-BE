package com.lgcns.bebee.common.data.event.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.bebee.common.data.event.EventEnvelope;
import com.lgcns.bebee.common.data.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.sns.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SnsEventPublisher implements EventPublisher {
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${app.sns.topic-arn}")
    private String topicArn;

    @Override
    public void publish(EventEnvelope event) {
        try {
            String messagePayload = objectMapper.writeValueAsString(event);

            log.info("이벤트 발행 시작 - Topic: {}, Event: {}", topicArn, event.eventType());

            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messagePayload)
                    .messageAttributes(Map.of(
                            "eventType", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(event.eventType())
                                    .build()
                    ))
                    .build();

            snsClient.publish(request);

            log.info("이벤트 발행 완료 - Event: {}, 발행 시간: {}", event.eventType(), event.producedAt());
        } catch (JsonProcessingException e) {
            log.error("이벤트 발행 실패 - Event: {}", event.eventType(), e);
            throw new RuntimeException("이벤트 이벤트 발행 실패", e);
        }
    }
}
