package com.carddemo.account.service;

import com.carddemo.account.dto.AccountResponse;
import com.carddemo.account.dto.AccountSummaryResponse;
import com.carddemo.account.dto.CustomerResponse;
import com.carddemo.account.entity.Account;
import com.carddemo.account.entity.Customer;
import com.carddemo.account.exception.AccountNotFoundException;
import com.carddemo.account.exception.CustomerNotFoundException;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Account Service - Business logic for COACTVWC Account View
 * Replaces COBOL COACTVWC paragraphs for VSAM ACCTDAT operations
 */
@Service
@Transactional(readOnly = true)
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Get account by ID - replaces CICS READ ACCTDAT
     */
    public AccountResponse getAccountById(Long accountId) {
        log.debug("Fetching account: {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return AccountResponse.fromEntity(account);
    }

    /**
     * Get all accounts for a customer - main COACTVWC function
     */
    public List<AccountResponse> getAccountsByCustomerId(Integer customerId) {
        log.debug("Fetching accounts for customer: {}", customerId);

        // Verify customer exists
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }

        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all accounts with pagination
     */
    public Page<AccountResponse> getAllAccounts(int page, int size) {
        log.debug("Fetching all accounts, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Account> accountPage = accountRepository.findAllByOrderByAccountIdAsc(pageable);
        return accountPage.map(AccountResponse::fromEntity);
    }

    /**
     * Search accounts by customer name
     */
    public List<AccountResponse> searchByCustomerName(String name) {
        log.debug("Searching accounts by customer name: {}", name);
        List<Account> accounts = accountRepository.searchByCustomerName(name);
        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get accounts expiring within specified days
     */
    public List<AccountResponse> getExpiringAccounts(int days) {
        log.debug("Fetching accounts expiring within {} days", days);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<Account> accounts = accountRepository.findExpiringAccounts(startDate, endDate);
        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get accounts with high credit utilization
     */
    public List<AccountResponse> getHighUtilizationAccounts(BigDecimal threshold) {
        log.debug("Fetching accounts with utilization >= {}%", threshold);
        List<Account> accounts = accountRepository.findHighUtilizationAccounts(threshold);
        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get customer details
     */
    public CustomerResponse getCustomerById(Integer customerId) {
        log.debug("Fetching customer: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        return CustomerResponse.fromEntity(customer);
    }

    /**
     * Get all customers with pagination
     */
    public Page<CustomerResponse> getAllCustomers(int page, int size) {
        log.debug("Fetching all customers, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return customerRepository.findAll(pageable).map(CustomerResponse::fromEntity);
    }

    /**
     * Search customers by name
     */
    public List<CustomerResponse> searchCustomersByName(String name) {
        log.debug("Searching customers by name: {}", name);
        List<Customer> customers = customerRepository.searchByName(name);
        return customers.stream()
                .map(CustomerResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get account summary statistics
     */
    public AccountSummaryResponse getAccountSummary() {
        log.debug("Generating account summary");
        AccountSummaryResponse summary = new AccountSummaryResponse();

        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countByStatus("Y");
        long closedAccounts = accountRepository.countByStatus("N");
        BigDecimal totalBalance = accountRepository.getTotalActiveBalance();
        BigDecimal totalCreditLimit = accountRepository.getTotalCreditLimit();

        summary.setTotalAccounts(totalAccounts);
        summary.setActiveAccounts(activeAccounts);
        summary.setClosedAccounts(closedAccounts);
        summary.setExpiredAccounts(totalAccounts - activeAccounts - closedAccounts);
        summary.setTotalBalance(totalBalance);
        summary.setTotalCreditLimit(totalCreditLimit);
        summary.setTotalAvailableCredit(totalCreditLimit.subtract(totalBalance));

        if (totalCreditLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avgUtil = totalBalance.multiply(BigDecimal.valueOf(100))
                    .divide(totalCreditLimit, 2, RoundingMode.HALF_UP);
            summary.setAverageUtilization(avgUtil);
        } else {
            summary.setAverageUtilization(BigDecimal.ZERO);
        }

        return summary;
    }

    /**
     * Update account status - replaces CICS REWRITE ACCTDAT
     */
    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, String status) {
        log.info("Updating account {} status to {}", accountId, status);

        if (!"Y".equals(status) && !"N".equals(status)) {
            throw new IllegalArgumentException("Invalid status. Must be 'Y' (Active) or 'N' (Closed)");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.setActiveStatus(status);
        account = accountRepository.save(account);

        log.info("Account {} status updated to {}", accountId, status);
        return AccountResponse.fromEntity(account);
    }
}
