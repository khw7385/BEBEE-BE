package com.lgcns.bebee.member.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.bebee.common.data.event.*;
import com.lgcns.bebee.common.data.event.aws.SnsEventPublisher;
import com.lgcns.bebee.common.data.event.aws.SqsEventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsClient;

import java.util.List;

@Configuration
public class EventConfig {

    @Bean
    public DomainEventPublisher domainEventPublisher(
            OutboxRepository outboxRepository,
            ApplicationEventPublisher springEventPublisher,
            ObjectMapper objectMapper) {
        return new DomainEventPublisher(outboxRepository, springEventPublisher, objectMapper);
    }

    @Bean
    public EventHandlerRegistry eventHandlerRegistry(List<EventHandler<? extends DomainEvent>> handlers){
        return new EventHandlerRegistry(handlers);
    }

    @Bean
    public ProcessedEventManager processedEventManager(ProcessedEventRepository processedEventRepository) {
        return new ProcessedEventManager(processedEventRepository);
    }

    @Bean
    public SqsEventListener sqsEventListener(
            EventTypeMapper eventTypeMapper,
            EventHandlerRegistry eventHandlerRegistry,
            ProcessedEventManager processedEventManager,
            ObjectMapper objectMapper){
        return new SqsEventListener(objectMapper, eventTypeMapper, eventHandlerRegistry, processedEventManager);
    }

    @Bean
    public SpringEventListener springEventListener(EventPublisher eventPublisher, OutboxRepository outboxRepository){
        return new SpringEventListener(eventPublisher, outboxRepository);
    }

    @Bean
    public SnsEventPublisher snsEventPublisher(SnsClient snsClient, ObjectMapper objectMapper){
        return new SnsEventPublisher(snsClient, objectMapper);
    }
}
