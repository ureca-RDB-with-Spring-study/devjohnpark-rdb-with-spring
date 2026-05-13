package com.smartclearance.order;

import java.time.LocalDateTime;

// orders, users, products 세 테이블을 INNER JOIN한 결과를 담는 응답 객체
// OrderResponse는 FK(userId, productId)만 반환하지만,
// OrderDetailResponse는 JOIN을 통해 실제 이름까지 포함한다
public record OrderDetailResponse(
        Long orderId,
        String userName,    // users.name (INNER JOIN으로 가져온 값)
        String productName, // products.name (INNER JOIN으로 가져온 값)
        int quantity,
        LocalDateTime orderDate,
        String status
) {}
