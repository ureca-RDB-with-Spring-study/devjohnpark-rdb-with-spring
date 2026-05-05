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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long customerId;
    private long productId;

    @BeforeEach
    void setUp() {
        KeyHolder customerKey = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO customers (name, email, password, address) VALUES (?, ?, ?, ?)",
                    new String[]{"customer_id"});
            ps.setString(1, "테스터");
            ps.setString(2, "order_test@test.com");
            ps.setString(3, "password123");
            ps.setString(4, "서울시 강남구");
            return ps;
        }, customerKey);
        customerId = customerKey.getKey().longValue();

        KeyHolder productKey = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO products (name, price, stock_quantity) VALUES (?, ?, ?)",
                    new String[]{"product_id"});
            ps.setString(1, "테스트상품");
            ps.setInt(2, 10000);
            ps.setInt(3, 100);
            return ps;
        }, productKey);
        productId = productKey.getKey().longValue();
    }

    @Test
    void 주문_성공시_201과_주문정보를_반환한다() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": %d,
                                  "productId": %d,
                                  "quantity": 3
                                }
                                """.formatted(customerId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("Order Received"));
    }

    @Test
    void 존재하지_않는_상품_주문시_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": %d,
                                  "productId": 999999,
                                  "quantity": 1
                                }
                                """.formatted(customerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void 재고_부족시_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": %d,
                                  "productId": %d,
                                  "quantity": 9999
                                }
                                """.formatted(customerId, productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
