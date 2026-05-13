package com.smartclearance.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest: 전체 Spring 컨텍스트를 로드해 Controller → Service → Repository 흐름을 통합 검증
// @AutoConfigureMockMvc: 실제 서버를 띄우지 않고 HTTP 요청/응답을 시뮬레이션하는 MockMvc를 자동 구성
// @Transactional: 각 테스트 후 DB 변경사항을 자동 롤백해 테스트 간 데이터 격리 보장
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // application-test.yml 적용: H2 in-memory DB, flyway target: 1(DDL만 실행)
@Transactional
class OrderIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long userId;
    private long productId;

    // 모든 테스트에 공통으로 필요한 user, product만 삽입한다
    // order는 GET 상세조회 테스트에서만 필요하므로 헬퍼 메서드로 분리해 해당 테스트에서 직접 생성한다
    @BeforeEach
    void setUp() {
        userId = insertUser("테스터", "order_test@test.com");
        productId = insertProduct("테스트상품", 10000, 100);
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
    void 주문_상세조회시_사용자명과_상품명을_반환한다() throws Exception {
        // 이 테스트만 기존 주문 데이터가 필요하므로 여기서 직접 생성한다
        long orderId = insertOrder(userId, productId);

        // GET /api/orders/{id}: INNER JOIN 쿼리로 orders, users, products를 조인한 결과를 검증
        // userId, productId가 아닌 실제 이름(userName, productName)이 응답에 포함되는지 확인
        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.userName").value("테스터"))       // users.name JOIN 결과
                .andExpect(jsonPath("$.productName").value("테스트상품")) // products.name JOIN 결과
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 존재하지_않는_주문_상세조회시_400을_반환한다() throws Exception {
        // 존재하지 않는 ID → Service에서 IllegalArgumentException → ExceptionHandler가 400 응답
        mockMvc.perform(get("/api/orders/999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void 주문_성공시_201과_주문정보를_반환한다() throws Exception {
        // perform(): HTTP 요청 시뮬레이션
        // andExpect(): 상태코드, JSON 필드 등 응답 검증
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "productId": %d,
                                  "quantity": 3
                                }
                                """.formatted(userId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 존재하지_않는_상품_주문시_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "productId": 999999,
                                  "quantity": 1
                                }
                                """.formatted(userId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void 재고_부족시_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "productId": %d,
                                  "quantity": 9999
                                }
                                """.formatted(userId, productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
