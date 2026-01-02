package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.TransactionOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Transaction Outbox Repository
 */
@Repository
public interface TransactionOutboxRepository extends JpaRepository<TransactionOutbox, Long> {

    /**
     * Find pending events to publish (ordered by creation time)
     */
    @Query("SELECT o FROM TransactionOutbox o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<TransactionOutbox> findPendingEvents();

    /**
     * Count pending events
     */
    long countByStatus(String status);
}
