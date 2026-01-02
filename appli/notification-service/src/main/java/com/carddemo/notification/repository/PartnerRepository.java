package com.carddemo.notification.repository;

import com.carddemo.notification.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Partner Repository
 */
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Integer> {

    /**
     * Find all active partners with a webhook URL configured
     */
    List<Partner> findByIsActiveTrueAndWebhookUrlIsNotNull();

    /**
     * Find partners that have a specific scope in their allowed_scopes array
     */
    @Query(value = "SELECT * FROM partners WHERE is_active = true " +
            "AND webhook_url IS NOT NULL " +
            "AND :scope = ANY(allowed_scopes)", nativeQuery = true)
    List<Partner> findByActiveAndHasScope(@Param("scope") String scope);
}
