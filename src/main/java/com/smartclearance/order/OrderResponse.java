package com.smartclearance.order;

import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        Long customerId,
        Long productId,
        int quantity,
        LocalDateTime orderDate,
        String status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getOrderDate(),
                order.getStatus()
        );
    }
}
