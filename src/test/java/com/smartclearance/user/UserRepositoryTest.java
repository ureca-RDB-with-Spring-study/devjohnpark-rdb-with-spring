package com.smartclearance.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @JdbcTest
// - DataSource, JdbcTemplate 생성
// - Repository 객체는 따로 주입 필요: `@Import(UserRepository.class)`
// - 기본적으로 @Transactional이 적용되어 있어서 각 테스트 후 자동으로 롤백

// @ActiveProfiles
// - 테스트 실행 시 사용할 Spring 프로파일을 지정하는 어노테이션
// - 프로파일: 환경 local, test, prod 등에 따라 다른 설정을 적용하는 메커니즘
//      - src/main/resources/application.yml: 공통 설정
//      - src/main/resources/application-local.yml: 로컬 설정
//      - src/main/resources/application-prod.yml: 운영 설정
//      - src/test/resources/application-test.yml: test 프로파일 설정
// - application-test.yml에서 testdb를 H2 인메모리 DB로 설정해서 @JdbcTest에서 MySQL 없이도 빠르게 실행 가능

@JdbcTest
@ActiveProfiles("test")
@Import(UserRepository.class)
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 유저저장후_ID로_조회한다() {
        User user = new User(null, "박준서", "jun@test.com", "password123", "서울시 강남구", null, null);

        Long savedId = userRepository.save(user);
        Optional<User> found = userRepository.findById(savedId);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("박준서");
        assertThat(found.get().getEmail()).isEqualTo("jun@test.com");
        assertThat(found.get().getAddress()).isEqualTo("서울시 강남구");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void 존재하는_이메일이면_true를_반환한다() {
        User user = new User(null, "박준서", "exist@test.com", "password123", null, null, null);
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("exist@test.com")).isTrue();
    }

    @Test
    void 존재하지않는_이메일이면_false를_반환한다() {
        assertThat(userRepository.existsByEmail("none@test.com")).isFalse();
    }

    @Test
    void 존재하지않는_ID로_조회하면_빈값을_반환한다() {
        Optional<User> found = userRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void 주문이_없는_유저만_조회된다() {
        long userWithOrderId = insertUser("주문유저", "with_order@test.com");
        long userWithoutOrderId = insertUser("주문없음유저", "no_order@test.com");
        long productId = insertProduct("테스트상품", 10000, 100);
        insertOrder(userWithOrderId, productId);

        List<User> result = userRepository.findAllWithNoOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userWithoutOrderId);
    }

    @Test
    void 모든_유저가_주문이_있으면_빈_목록을_반환한다() {
        long userId = insertUser("주문유저", "all_ordered@test.com");
        long productId = insertProduct("테스트상품", 10000, 100);
        insertOrder(userId, productId);

        List<User> result = userRepository.findAllWithNoOrders();

        assertThat(result).isEmpty();
    }

    private long insertUser(String name, String email) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (name, email, password) VALUES (?, ?, ?)",
                    new String[]{"user_id"});
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

    private void insertOrder(long userId, long productId) {
        jdbcTemplate.update(
                "INSERT INTO orders (user_id, product_id, quantity, status) VALUES (?, ?, ?, ?)",
                userId, productId, 1, "PENDING");
    }
}
