package com.lgcns.bebee.common.data.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ProcessedEvent(Long eventId, String eventType){
        this.eventId = eventId;
        this.eventType = eventType;
        this.createdAt = LocalDateTime.now();
    }
}
