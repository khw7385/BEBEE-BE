package com.lgcns.bebee.common.data.event;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class ProcessedEventManager {
    private final ProcessedEventRepository processedEventRepository;

    /**
     * 이벤트 처리 권한 획득 시도
     * @return true: 처리 가능 (신규), false: 중복 이벤트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(Long eventId, String eventType) {
        try {
            processedEventRepository.saveAndFlush(new ProcessedEvent(eventId, eventType));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    /**
     * 이벤트 처리 실패 시 재시도 가능하도록 기록 삭제
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(Long eventId) {
        processedEventRepository.deleteById(eventId);
    }
}
