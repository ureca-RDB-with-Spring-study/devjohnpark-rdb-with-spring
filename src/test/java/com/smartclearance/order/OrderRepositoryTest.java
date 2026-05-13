package com.smartclearance.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

import java.sql.PreparedStatement;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @JdbcTest: DataSource, JdbcTemplate만 띄우는 슬라이스 테스트
// Spring 전체 컨텍스트를 로드하지 않아 @SpringBootTest보다 빠르다
// 기본적으로 @Transactional이 적용되어 각 테스트 후 자동 롤백된다
// @Import: @JdbcTest는 Repository를 자동으로 빈으로 등록하지 않으므로 직접 지정한다
@JdbcTest
@ActiveProfiles("test") // application-test.yml 적용: H2 in-memory DB, flyway target: 1(DDL만 실행)
@Import(OrderRepository.class)
class OrderRepositoryTest {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long userId;
    private long productId;

    // 모든 테스트에 공통으로 필요한 user, product만 삽입한다
    // order는 INNER JOIN 조회 테스트에서만 필요하므로 헬퍼 메서드로 분리해 해당 테스트에서 직접 생성한다
    @BeforeEach
    void setUp() {
        userId = insertUser("테스터", "repo_order_test@test.com");
        productId = insertProduct("테스트상품", 10000, 100);
    }

    private long insertUser(String name, String email) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (name, email, password) VALUES (?, ?, ?)",
                    new String[]{"user_id"}); // 반환받을 자동 생성 키 컬럼 지정
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, "password123");
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    private long insertProduct(String name, int price, int stock) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO products (name, price, stock_quantity) VALUES (?, ?, ?)",
                    new String[]{"product_id"});
            ps.setString(1, name);
            ps.setInt(2, price);
            ps.setInt(3, stock);
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    // orders는 users, products에 대한 FK를 가지므로 두 테이블 삽입 후에 호출해야 한다
    private long insertOrder(long userId, long productId) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO orders (user_id, product_id, quantity, status) VALUES (?, ?, ?, ?)",
                    new String[]{"order_id"});
            ps.setLong(1, userId);
            ps.setLong(2, productId);
            ps.setInt(3, 2);
            ps.setString(4, "PENDING");
            return ps;
        }, key);
        return key.getKey().longValue();
    }

    @Test
    void INNER_JOIN으로_사용자명과_상품명을_함께_조회한다() {
        // 이 테스트만 기존 주문 데이터가 필요하므로 여기서 직접 생성한다
        long orderId = insertOrder(userId, productId);

        Optional<OrderDetailResponse> result = orderRepository.findDetailById(orderId);

        // JOIN 결과가 존재하는지 확인
        assertThat(result).isPresent();
        assertThat(result.get().orderId()).isEqualTo(orderId);
        // users.name이 JOIN을 통해 올바르게 매핑되었는지 확인
        assertThat(result.get().userName()).isEqualTo("테스터");
        // products.name이 JOIN을 통해 올바르게 매핑되었는지 확인
        assertThat(result.get().productName()).isEqualTo("테스트상품");
        assertThat(result.get().quantity()).isEqualTo(2);
        assertThat(result.get().status()).isEqualTo("PENDING");
        // order_date는 DB의 DEFAULT CURRENT_TIMESTAMP로 자동 설정되므로 null이 아님을 확인
        assertThat(result.get().orderDate()).isNotNull();
    }

    @Test
    void 존재하지_않는_주문ID로_조회하면_빈값을_반환한다() {
        // INNER JOIN 특성상 orders에 해당 ID가 없으면 결과가 없다
        // Repository에서 EmptyResultDataAccessException을 Optional.empty()로 변환하는지 확인
        Optional<OrderDetailResponse> result = orderRepository.findDetailById(999999L);

        assertThat(result).isEmpty();
    }
}
