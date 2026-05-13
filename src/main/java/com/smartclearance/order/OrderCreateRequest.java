package com.smartclearance.order;

public record OrderCreateRequest(
        Long userId,
        Long productId,
        int quantity
) {}
