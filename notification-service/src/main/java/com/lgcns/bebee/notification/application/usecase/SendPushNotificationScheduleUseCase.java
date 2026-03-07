package com.lgcns.bebee.notification.application.usecase;

import com.lgcns.bebee.notification.application.BatchPushMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendPushNotificationScheduleUseCase {
    private final BatchPushMessageProcessor batchPushMessageProcessor;

    @Value("${push.batch.mode:synchronized}")
    private String batchMode;

    /**
     * 1초마다 버퍼에 쌓인 메시지를 배치 전송합니다.
     * push.batch.mode 설정에 따라 동작 방식이 달라집니다.
     * - synchronized: 기존 synchronized 방식
     * - lock-free: AtomicInteger/AtomicBoolean 기반 lock-free 방식
     */
    @Scheduled(fixedDelay = 1000)
    public void execute() {
        if ("lock-free".equals(batchMode)) {
            batchPushMessageProcessor.flushLockFree();
        } else {
            batchPushMessageProcessor.flush();
        }
    }
}
