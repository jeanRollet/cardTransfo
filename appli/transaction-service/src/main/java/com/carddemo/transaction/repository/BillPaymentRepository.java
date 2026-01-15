package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {

    List<BillPayment> findByAccountIdOrderByPaymentDateDesc(Long accountId);

    @Query(value = "SELECT bp.* FROM bill_payments bp " +
           "JOIN bill_payees p ON bp.payee_id = p.payee_id " +
           "WHERE p.customer_id = :customerId ORDER BY bp.payment_date DESC", nativeQuery = true)
    List<BillPayment> findByCustomerId(@Param("customerId") Integer customerId);

    @Query(value = "SELECT bp.* FROM bill_payments bp " +
           "JOIN bill_payees p ON bp.payee_id = p.payee_id " +
           "WHERE p.customer_id = :customerId AND bp.status IN ('PENDING', 'SCHEDULED') " +
           "ORDER BY bp.scheduled_date ASC", nativeQuery = true)
    List<BillPayment> findScheduledByCustomerId(@Param("customerId") Integer customerId);

    List<BillPayment> findByStatusAndIsRecurringTrue(String status);
}
