package com.smartclearance.customer;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long customerId,
        String name,
        String email,
        String address,
        LocalDateTime joinDate
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getName(),
                customer.getEmail(),
                customer.getAddress(),
                customer.getJoinDate()
        );
    }
}
