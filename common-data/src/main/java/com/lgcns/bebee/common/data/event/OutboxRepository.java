package com.lgcns.bebee.common.data.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Outbox o WHERE o.id = :id")
    Optional<Outbox> findByIdWithLock(@Param("id") Long id);

    @Query(value = "SELECT * FROM Outbox o WHERE o.status = 'READY' " +
            "AND (o.next_retry_at IS NULL OR o.next_retry_at <= :now) " +
            "AND o.retry_count < :maxRetryCount " +
            "ORDER BY o.created_at ASC " +
            "LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<Outbox> findRetryableOutboxesWithLock(@Param("now") LocalDateTime now,
                                               @Param("maxRetryCount") int maxRetryCount,
                                               @Param("limit") int limit);

    @Query("SELECT o FROM Outbox o WHERE o.status = 'DONE' AND o.createdAt < :before")
    List<Outbox> findCompletedOutboxesBefore(@Param("before") LocalDateTime before);

    @Modifying
    @Query("DELETE FROM Outbox o WHERE o.status = 'DONE' AND o.updatedAt < :threshold")
    void deleteCompletedOutboxesBefore(@Param("threshold") LocalDateTime threshold);
}

