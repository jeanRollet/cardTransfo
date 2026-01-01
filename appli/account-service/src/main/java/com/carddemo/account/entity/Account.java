package com.carddemo.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Account entity - maps to accounts table (from ACCTDAT VSAM)
 * Represents a credit card account in the CardDemo system
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Column(name = "active_status", nullable = false, columnDefinition = "char(1)")
    private String activeStatus = "Y";

    @Column(name = "current_balance", nullable = false, precision = 11, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "credit_limit", nullable = false, precision = 11, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "cash_credit_limit", nullable = false, precision = 11, scale = 2)
    private BigDecimal cashCreditLimit;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    @Column(name = "curr_cycle_credit", precision = 11, scale = 2)
    private BigDecimal currCycleCredit = BigDecimal.ZERO;

    @Column(name = "curr_cycle_debit", precision = 11, scale = 2)
    private BigDecimal currCycleDebit = BigDecimal.ZERO;

    @Column(name = "group_id", length = 10)
    private String groupId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Account() {}

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCashCreditLimit() {
        return cashCreditLimit;
    }

    public void setCashCreditLimit(BigDecimal cashCreditLimit) {
        this.cashCreditLimit = cashCreditLimit;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDate getReissueDate() {
        return reissueDate;
    }

    public void setReissueDate(LocalDate reissueDate) {
        this.reissueDate = reissueDate;
    }

    public BigDecimal getCurrCycleCredit() {
        return currCycleCredit;
    }

    public void setCurrCycleCredit(BigDecimal currCycleCredit) {
        this.currCycleCredit = currCycleCredit;
    }

    public BigDecimal getCurrCycleDebit() {
        return currCycleDebit;
    }

    public void setCurrCycleDebit(BigDecimal currCycleDebit) {
        this.currCycleDebit = currCycleDebit;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business methods
    public boolean isActive() {
        return "Y".equals(activeStatus);
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(currentBalance);
    }

    public BigDecimal getUtilizationRate() {
        if (creditLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentBalance.multiply(BigDecimal.valueOf(100))
                .divide(creditLimit, 2, java.math.RoundingMode.HALF_UP);
    }

    public String getStatusName() {
        if (!isActive()) return "Closed";
        if (isExpired()) return "Expired";
        return "Active";
    }
}
