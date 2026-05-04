package com.smartclearance.customer;

public record CustomerCreateRequest(
        String name,
        String email,
        String password,
        String address
) {}
