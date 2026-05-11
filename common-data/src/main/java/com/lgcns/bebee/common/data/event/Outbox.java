package com.lgcns.bebee.common.data.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lgcns.bebee.common.data.domain.BaseTimeEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox extends BaseTimeEntity {

    @Id @Tsid
    @Column(name = "outbox_id")
    private Long id;

    @Column(length = 100)
    private String eventType;

    @Column(columnDefinition = "json", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.READY;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column
    private LocalDateTime nextRetryAt;

    public enum Status {
        READY, DONE, FAILED
    }

    public static Outbox create(DomainEvent event, ObjectMapper objectMapper) {
        Outbox outbox = new Outbox();

        try {
            outbox.eventType = event.getClass().getSimpleName();
            outbox.payload = objectMapper.writeValueAsString(event);
            outbox.status = Status.READY;
            outbox.retryCount = 0;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return outbox;
    }

    public void markAsDone() {
        this.status = Status.DONE;
    }

    public void markAsFailed() {
        this.status = Status.FAILED;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void scheduleNextRetry(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
        this.status = Status.READY;
    }

    public DomainEvent getEvent(ObjectMapper objectMapper, EventTypeMapper mapper) {
        Class<? extends DomainEvent> targetClass = mapper.getClass(this.eventType);
        try {
            return objectMapper.readValue(this.payload, targetClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
