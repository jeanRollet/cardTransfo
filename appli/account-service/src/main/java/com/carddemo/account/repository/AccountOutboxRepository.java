package com.carddemo.account.repository;

import com.carddemo.account.entity.AccountOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Account Outbox Repository
 */
@Repository
public interface AccountOutboxRepository extends JpaRepository<AccountOutbox, Long> {

    @Query("SELECT o FROM AccountOutbox o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<AccountOutbox> findPendingEvents();

    long countByStatus(String status);
}
