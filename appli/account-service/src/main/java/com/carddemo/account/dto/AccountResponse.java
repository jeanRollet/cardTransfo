package com.carddemo.account.dto;

import com.carddemo.account.entity.Account;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DTO for Account response - replaces BMS map fields for COACTVWC
 */
public class AccountResponse {
    private Long accountId;
    private Integer customerId;
    private String customerName;
    private String activeStatus;
    private String statusName;
    private BigDecimal currentBalance;
    private BigDecimal creditLimit;
    private BigDecimal cashCreditLimit;
    private BigDecimal availableCredit;
    private BigDecimal utilizationRate;
    private LocalDate openDate;
    private String openDateFormatted;
    private LocalDate expiryDate;
    private String expiryDateFormatted;
    private LocalDate reissueDate;
    private BigDecimal currCycleCredit;
    private BigDecimal currCycleDebit;
    private String groupId;
    private boolean isActive;
    private boolean isExpired;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // Constructors
    public AccountResponse() {}

    public static AccountResponse fromEntity(Account account) {
        AccountResponse dto = new AccountResponse();
        dto.setAccountId(account.getAccountId());
        dto.setCustomerId(account.getCustomerId());
        dto.setActiveStatus(account.getActiveStatus());
        dto.setStatusName(account.getStatusName());
        dto.setCurrentBalance(account.getCurrentBalance());
        dto.setCreditLimit(account.getCreditLimit());
        dto.setCashCreditLimit(account.getCashCreditLimit());
        dto.setAvailableCredit(account.getAvailableCredit());
        dto.setUtilizationRate(account.getUtilizationRate());
        dto.setOpenDate(account.getOpenDate());
        dto.setOpenDateFormatted(account.getOpenDate() != null ?
                account.getOpenDate().format(DATE_FORMATTER) : null);
        dto.setExpiryDate(account.getExpiryDate());
        dto.setExpiryDateFormatted(account.getExpiryDate() != null ?
                account.getExpiryDate().format(DATE_FORMATTER) : null);
        dto.setReissueDate(account.getReissueDate());
        dto.setCurrCycleCredit(account.getCurrCycleCredit());
        dto.setCurrCycleDebit(account.getCurrCycleDebit());
        dto.setGroupId(account.getGroupId());
        dto.setActive(account.isActive());
        dto.setExpired(account.isExpired());

        if (account.getCustomer() != null) {
            dto.setCustomerName(account.getCustomer().getFullName());
        }

        return dto;
    }

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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
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

    public BigDecimal getAvailableCredit() {
        return availableCredit;
    }

    public void setAvailableCredit(BigDecimal availableCredit) {
        this.availableCredit = availableCredit;
    }

    public BigDecimal getUtilizationRate() {
        return utilizationRate;
    }

    public void setUtilizationRate(BigDecimal utilizationRate) {
        this.utilizationRate = utilizationRate;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
        this.openDate = openDate;
    }

    public String getOpenDateFormatted() {
        return openDateFormatted;
    }

    public void setOpenDateFormatted(String openDateFormatted) {
        this.openDateFormatted = openDateFormatted;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getExpiryDateFormatted() {
        return expiryDateFormatted;
    }

    public void setExpiryDateFormatted(String expiryDateFormatted) {
        this.expiryDateFormatted = expiryDateFormatted;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }
}
