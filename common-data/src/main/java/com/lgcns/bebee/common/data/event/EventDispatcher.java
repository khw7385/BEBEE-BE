package com.lgcns.bebee.common.data.event;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatcher {
    private final List<EventHandler<? extends DomainEvent>> eventHandlers;
    private Map<Class<? extends DomainEvent>, EventHandler<? extends DomainEvent>> handlerMap;

    @PostConstruct
    public void init(){
        handlerMap = eventHandlers.stream()
                .collect(Collectors.toMap(
                        EventHandler::getEventClass,
                        Function.identity()
                ));
    }

    public void dispatch(DomainEvent event) {
        EventHandler<? extends DomainEvent> handler = handlerMap.get(event.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                String.format("[%s] 해당 이벤트에 대한 핸들러가 없습니다.", event.getClass().getSimpleName())
            );
        }
        callHandler(handler, event);
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> void callHandler(EventHandler<T> handler, DomainEvent event) {
        handler.handle((T) event);
    }
}
