package com.carddemo.partner.repository;

import com.carddemo.partner.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Integer> {

    Optional<Partner> findByPartnerIdAndIsActiveTrue(Integer partnerId);

    List<Partner> findByIsActiveTrueOrderByPartnerNameAsc();

    List<Partner> findByPartnerTypeAndIsActiveTrue(String partnerType);

    @Query("SELECT p FROM Partner p WHERE p.isActive = true AND p.partnerName LIKE %:name%")
    List<Partner> searchByName(String name);

    boolean existsByPartnerName(String partnerName);

    boolean existsByContactEmail(String contactEmail);
}
