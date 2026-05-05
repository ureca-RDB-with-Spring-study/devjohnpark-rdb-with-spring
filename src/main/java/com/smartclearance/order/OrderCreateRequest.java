package com.smartclearance.order;

public record OrderCreateRequest(
        Long customerId,
        Long productId,
        int quantity
) {}
