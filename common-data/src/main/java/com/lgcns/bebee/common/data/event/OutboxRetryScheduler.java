package com.lgcns.bebee.common.data.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OutboxRetryScheduler {
    private final OutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final EventTypeMapper eventTypeMapper;

    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 100;
    private static final int[] RETRY_DELAYS_SECONDS = {30, 60, 120};

    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional
    public void retryFailedOutboxes() {
        log.debug("=== Outbox 재처리 스케줄러 시작 ===");

        List<Outbox> retryableOutboxes = outboxRepository.findRetryableOutboxesWithLock(
                LocalDateTime.now(),
                MAX_RETRY_COUNT,
                BATCH_SIZE
        );

        if (retryableOutboxes.isEmpty()) {
            log.debug("재처리 대상 Outbox가 없습니다.");
            return;
        }

        log.info("재처리 대상 Outbox 수: {}", retryableOutboxes.size());

        int successCount = 0;
        int failCount = 0;

        for (Outbox outbox : retryableOutboxes) {
            try {
                processOutbox(outbox);
                successCount++;
            } catch (Exception e) {
                handleRetryFailure(outbox, e);
                failCount++;
            }
        }

        log.info("=== Outbox 재처리 스케줄러 종료: 성공 {}건, 실패 {}건 ===", successCount, failCount);
    }

    private void processOutbox(Outbox outbox) {
        outbox.markAsProceeding();

        DomainEvent event = outbox.getEvent(objectMapper, eventTypeMapper);
        EventEnvelope envelope = EventEnvelope.from(outbox.getId(), event);

        eventPublisher.publish(envelope);

        outbox.markAsDone();
        log.info("Outbox 재처리 성공 - ID: {}, EventType: {}", outbox.getId(), outbox.getEventType());
    }

    private void handleRetryFailure(Outbox outbox, Exception e) {
        outbox.incrementRetryCount();

        if (outbox.getRetryCount() >= MAX_RETRY_COUNT) {
            outbox.markAsFailed();
            log.error("Outbox 최대 재시도 횟수 초과로 FAILED 처리 - ID: {}, EventType: {}",
                    outbox.getId(), outbox.getEventType(), e);
        } else {
            int delaySeconds = RETRY_DELAYS_SECONDS[Math.min(outbox.getRetryCount() - 1, RETRY_DELAYS_SECONDS.length - 1)];
            LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);
            outbox.scheduleNextRetry(nextRetryAt);
            log.warn("Outbox 재처리 실패, 다음 재시도 예약 - ID: {}, RetryCount: {}, NextRetryAt: {}",
                    outbox.getId(), outbox.getRetryCount(), nextRetryAt, e);
        }
    }

    /**
     * 완료된 오래된 Outbox 정리 (7일 이상 된 DONE 상태)
     * 실행 시점: 매일 새벽 4시
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupCompletedOutboxes() {
        log.info("=== Outbox 정리 스케줄러 시작 ===");

        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<Outbox> oldOutboxes = outboxRepository.findCompletedOutboxesBefore(threshold);

        if (oldOutboxes.isEmpty()) {
            log.info("정리 대상 Outbox가 없습니다.");
            return;
        }

        outboxRepository.deleteAll(oldOutboxes);
        log.info("=== Outbox 정리 완료: {}건 삭제 ===", oldOutboxes.size());
    }
}
