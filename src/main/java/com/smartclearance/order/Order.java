package com.smartclearance.order;

import java.time.LocalDateTime;

public class Order {

    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final int quantity;
    private final LocalDateTime orderDate;
    private final String status;

    public Order(Long orderId, Long userId, Long productId, int quantity, LocalDateTime orderDate, String status) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderDate = orderDate;
        this.status = status;
    }

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public String getStatus() { return status; }
}
