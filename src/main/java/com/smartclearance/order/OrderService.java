package com.smartclearance.order;

import com.smartclearance.product.Product;
import com.smartclearance.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse order(OrderCreateRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + request.productId()));

        if (product.getStockQuantity() < request.quantity()) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStockQuantity());
        }

        int updated = productRepository.decreaseStock(request.productId(), request.quantity());
        if (updated == 0) {
            throw new IllegalStateException("재고 차감 실패 (동시 요청으로 인한 재고 부족)");
        }

        Order order = new Order(null, request.customerId(), request.productId(), request.quantity(), null, "Order Received");
        Long orderId = orderRepository.save(order);

        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new IllegalStateException("주문 저장 실패"));
    }
}
