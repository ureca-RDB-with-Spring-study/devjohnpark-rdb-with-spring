package com.smartclearance.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // JOIN 쿼리로 조회한 주문 상세 정보(사용자명, 상품명 포함)를 반환한다
    // 존재하지 않는 id 요청 시 ExceptionHandler가 400을 응답한다
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> order(@RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.order(request);
        return ResponseEntity
                .created(URI.create("/api/orders/" + response.orderId()))
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
}
