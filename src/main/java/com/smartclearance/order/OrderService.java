package com.smartclearance.order;

import com.smartclearance.product.Product;
import com.smartclearance.product.ProductRepository;
import com.smartclearance.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<OrderDetailResponse> getOrders(Long userId) {
        if (userId == null) {
            return orderRepository.findAllDetails();
        }
        return orderRepository.findDetailsByUserId(userId);
    }

    // JOIN 쿼리로 주문 상세 정보를 조회한다
    // @Transactional(readOnly = true)가 클래스에 선언되어 있으므로 별도 트랜잭션 어노테이션 없이 읽기 전용으로 동작한다
    public OrderDetailResponse getOrderDetail(Long orderId) {
        // findDetailById가 빈 Optional을 반환하면 예외로 전환해 컨트롤러의 ExceptionHandler가 400을 응답하도록 한다
        return orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
    }

    @Transactional
    public OrderResponse order(OrderCreateRequest request) {
        validateCreateRequest(request);

        userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + request.userId()));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + request.productId()));

        if (product.getStockQuantity() < request.quantity()) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + product.getStockQuantity());
        }

        int updated = productRepository.decreaseStock(request.productId(), request.quantity());
        if (updated == 0) {
            throw new IllegalStateException("재고 차감 실패 (동시 요청으로 인한 재고 부족)");
        }

        Order order = new Order(null, request.userId(), request.productId(), request.quantity(), null, "PENDING");
        Long orderId = orderRepository.save(order);

        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new IllegalStateException("주문 저장 실패"));
    }

    private void validateCreateRequest(OrderCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("주문 요청이 비어 있습니다");
        }
        if (request.userId() == null) {
            throw new IllegalArgumentException("유저 ID는 필수입니다");
        }
        if (request.productId() == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다");
        }
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다");
        }
    }
}
