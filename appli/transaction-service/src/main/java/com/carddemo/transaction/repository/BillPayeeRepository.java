package com.carddemo.transaction.repository;

import com.carddemo.transaction.entity.BillPayee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillPayeeRepository extends JpaRepository<BillPayee, Long> {

    List<BillPayee> findByCustomerIdAndIsActiveTrueOrderByPayeeNameAsc(Integer customerId);

    List<BillPayee> findByCustomerIdOrderByPayeeNameAsc(Integer customerId);
}
