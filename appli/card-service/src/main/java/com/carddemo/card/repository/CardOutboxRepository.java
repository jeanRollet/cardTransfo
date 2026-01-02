package com.carddemo.card.repository;

import com.carddemo.card.entity.CardOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Card Outbox Repository
 */
@Repository
public interface CardOutboxRepository extends JpaRepository<CardOutbox, Long> {

    @Query("SELECT o FROM CardOutbox o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<CardOutbox> findPendingEvents();

    long countByStatus(String status);
}
