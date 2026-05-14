package com.lgcns.bebee.common.data.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.sns.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DomainEventPublisher {
    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(DomainEvent event){
        Outbox outbox = Outbox.create(event, objectMapper);
        Outbox savedOutbox = outboxRepository.save(outbox);
        EventEnvelope envelope = EventEnvelope.from(savedOutbox.getId(), event);
        applicationEventPublisher.publishEvent(envelope);
    }
}
