package com.carddemo.account.dto;

import com.carddemo.account.entity.Customer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * DTO for Customer response
 */
public class CustomerResponse {
    private Integer customerId;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String dateOfBirthFormatted;
    private Integer ficoCreditScore;
    private String creditScoreRating;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String fullAddress;
    private String phoneNumber;
    private String email;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public CustomerResponse() {}

    public static CustomerResponse fromEntity(Customer customer) {
        CustomerResponse dto = new CustomerResponse();
        dto.setCustomerId(customer.getCustomerId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setFullName(customer.getFullName());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setDateOfBirthFormatted(customer.getDateOfBirth() != null ?
                customer.getDateOfBirth().format(DATE_FORMATTER) : null);
        dto.setFicoCreditScore(customer.getFicoCreditScore());
        dto.setCreditScoreRating(customer.getCreditScoreRating());
        dto.setAddressLine1(customer.getAddressLine1());
        dto.setAddressLine2(customer.getAddressLine2());
        dto.setCity(customer.getCity());
        dto.setState(customer.getState());
        dto.setZipCode(customer.getZipCode());
        dto.setFullAddress(customer.getFullAddress());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setEmail(customer.getEmail());
        return dto;
    }

    // Getters and Setters
    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getDateOfBirthFormatted() {
        return dateOfBirthFormatted;
    }

    public void setDateOfBirthFormatted(String dateOfBirthFormatted) {
        this.dateOfBirthFormatted = dateOfBirthFormatted;
    }

    public Integer getFicoCreditScore() {
        return ficoCreditScore;
    }

    public void setFicoCreditScore(Integer ficoCreditScore) {
        this.ficoCreditScore = ficoCreditScore;
    }

    public String getCreditScoreRating() {
        return creditScoreRating;
    }

    public void setCreditScoreRating(String creditScoreRating) {
        this.creditScoreRating = creditScoreRating;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
