package com.smartclearance.customer;

import java.time.LocalDateTime;

public class Customer {

    private final Long customerId;
    private final String name;
    private final String email;
    private final String password;
    private final String address;
    private final LocalDateTime joinDate;

    public Customer(Long customerId, String name, String email, String password, String address, LocalDateTime joinDate) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.address = address;
        this.joinDate = joinDate;
    }

    public Long getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getAddress() { return address; }
    public LocalDateTime getJoinDate() { return joinDate; }
}
