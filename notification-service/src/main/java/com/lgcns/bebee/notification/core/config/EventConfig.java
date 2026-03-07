package com.lgcns.bebee.notification.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.bebee.common.data.event.*;
import com.lgcns.bebee.common.data.event.aws.SqsEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventConfig {

    @Bean
    public ProcessedEventManager processedEventManager(ProcessedEventRepository processedEventRepository) {
        return new ProcessedEventManager(processedEventRepository);
    }

    @Bean
    public EventHandlerRegistry eventHandlerRegistry(List<EventHandler<? extends DomainEvent>> handlers){
        return new EventHandlerRegistry(handlers);
    }

    @Bean
    public SqsEventListener sqsEventListener(
            EventTypeMapper eventTypeMapper,
            EventHandlerRegistry eventHandlerRegistry,
            ProcessedEventManager processedEventManager,
            ObjectMapper objectMapper){
        return new SqsEventListener(objectMapper, eventTypeMapper, eventHandlerRegistry, processedEventManager);
    }
}
