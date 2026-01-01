package com.carddemo.account.dto;

import java.math.BigDecimal;

/**
 * DTO for dashboard/summary statistics
 */
public class AccountSummaryResponse {
    private long totalAccounts;
    private long activeAccounts;
    private long closedAccounts;
    private long expiredAccounts;
    private BigDecimal totalBalance;
    private BigDecimal totalCreditLimit;
    private BigDecimal totalAvailableCredit;
    private BigDecimal averageUtilization;

    public AccountSummaryResponse() {}

    // Getters and Setters
    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public long getActiveAccounts() {
        return activeAccounts;
    }

    public void setActiveAccounts(long activeAccounts) {
        this.activeAccounts = activeAccounts;
    }

    public long getClosedAccounts() {
        return closedAccounts;
    }

    public void setClosedAccounts(long closedAccounts) {
        this.closedAccounts = closedAccounts;
    }

    public long getExpiredAccounts() {
        return expiredAccounts;
    }

    public void setExpiredAccounts(long expiredAccounts) {
        this.expiredAccounts = expiredAccounts;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getTotalCreditLimit() {
        return totalCreditLimit;
    }

    public void setTotalCreditLimit(BigDecimal totalCreditLimit) {
        this.totalCreditLimit = totalCreditLimit;
    }

    public BigDecimal getTotalAvailableCredit() {
        return totalAvailableCredit;
    }

    public void setTotalAvailableCredit(BigDecimal totalAvailableCredit) {
        this.totalAvailableCredit = totalAvailableCredit;
    }

    public BigDecimal getAverageUtilization() {
        return averageUtilization;
    }

    public void setAverageUtilization(BigDecimal averageUtilization) {
        this.averageUtilization = averageUtilization;
    }
}
