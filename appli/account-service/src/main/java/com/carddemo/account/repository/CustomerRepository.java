package com.carddemo.account.repository;

import com.carddemo.account.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Customer entity (CUSTDAT VSAM access)
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    List<Customer> findByLastNameContainingIgnoreCase(String lastName);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByName(@Param("name") String name);

    List<Customer> findByState(String state);

    @Query("SELECT c FROM Customer c WHERE c.ficoCreditScore >= :minScore")
    List<Customer> findByMinCreditScore(@Param("minScore") Integer minScore);
}
