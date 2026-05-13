package com.smartclearance.order;

import com.smartclearance.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

// DB 없이 OrderService의 비즈니스 로직만 단독으로 검증하는 단위 테스트
// Mockito가 런타임에 Repository의 동적 서브클래스를 생성해 실제 DB 호출을 대체한다
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // OrderRepository의 가짜 객체: 모든 메서드가 기본적으로 null/빈 값을 반환
    // given(...).willReturn(...)으로 테스트별 반환값을 직접 지정한다
    @Mock
    OrderRepository orderRepository;

    // OrderService 생성자에 필요하지만 이 테스트에서는 사용되지 않는 의존성
    // @InjectMocks가 생성자 주입 시 필요로 하므로 선언한다
    @Mock
    ProductRepository productRepository;

    // @Mock으로 만든 가짜 객체들을 주입받은 실제 테스트 대상
    @InjectMocks
    OrderService orderService;

    @Test
    void 주문_상세_조회시_사용자명과_상품명이_포함된_응답을_반환한다() {
        // Given: findDetailById(1L) 호출 시 반환할 값을 직접 지정 (DB 없이 동작)
        OrderDetailResponse detail = new OrderDetailResponse(1L, "테스터", "테스트상품", 2, LocalDateTime.now(), "PENDING");
        given(orderRepository.findDetailById(1L)).willReturn(Optional.of(detail)); // findDetailById(1L) 호춣시 반환값을 OrderDetailResponse으로 설정

        // When
        OrderDetailResponse response = orderService.getOrderDetail(1L);

        // Then: Service가 Repository 결과를 그대로 반환하는지 확인
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.userName()).isEqualTo("테스터");
        assertThat(response.productName()).isEqualTo("테스트상품");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("PENDING");
    }

    @Test
    void 존재하지_않는_주문ID로_조회하면_예외가_발생한다() {
        // Given: findDetailById(999L) 호출 시 빈 Optional 반환
        given(orderRepository.findDetailById(999L)).willReturn(Optional.empty());

        // Then: Service가 Optional.empty()를 IllegalArgumentException으로 전환하는지 확인
        // 컨트롤러의 ExceptionHandler가 이 예외를 받아 400으로 응답한다
        assertThatThrownBy(() -> orderService.getOrderDetail(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }
}
