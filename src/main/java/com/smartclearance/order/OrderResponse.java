package com.smartclearance.order;

import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        Long userId,
        Long productId,
        int quantity,
        LocalDateTime orderDate,
        String status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getOrderDate(),
                order.getStatus()
        );
    }
}
