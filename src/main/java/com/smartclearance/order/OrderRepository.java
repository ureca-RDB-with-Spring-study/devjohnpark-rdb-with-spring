package com.smartclearance.order;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // RowMapper로 ResultSet → Entity 변환
    private static final RowMapper<Order> ROW_MAPPER = (rs, rowNum) -> new Order(
            rs.getLong("order_id"),
            rs.getLong("user_id"),
            rs.getLong("product_id"),
            rs.getInt("quantity"),
            rs.getTimestamp("order_date") != null
                    ? rs.getTimestamp("order_date").toLocalDateTime()
                    : null,
            rs.getString("status")
    );

    private static final RowMapper<OrderDetailResponse> DETAIL_ROW_MAPPER = (rs, rowNum) -> new OrderDetailResponse(
            rs.getLong("order_id"),
            rs.getString("user_name"),
            rs.getString("product_name"),
            rs.getInt("quantity"),
            rs.getTimestamp("order_date") != null
                    ? rs.getTimestamp("order_date").toLocalDateTime()
                    : null,
            rs.getString("status")
    );

    public Long save(Order order) {
        String sql = "INSERT INTO orders (user_id, product_id, quantity, status) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"order_id"});
            ps.setLong(1, order.getUserId());
            ps.setLong(2, order.getProductId());
            ps.setInt(3, order.getQuantity());
            ps.setString(4, order.getStatus());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<Order> findById(Long id) {
        String sql = "SELECT order_id, user_id, product_id, quantity, order_date, status FROM orders WHERE order_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<OrderDetailResponse> findAllDetails() {
        String sql = """
                SELECT o.order_id, u.name AS user_name, p.name AS product_name,
                       o.quantity, o.order_date, o.status
                FROM orders o
                INNER JOIN users u ON o.user_id = u.user_id
                INNER JOIN products p ON o.product_id = p.product_id
                ORDER BY o.order_id DESC
                """;
        return jdbcTemplate.query(sql, DETAIL_ROW_MAPPER);
    }

    public List<OrderDetailResponse> findDetailsByUserId(Long userId) {
        String sql = """
                SELECT o.order_id, u.name AS user_name, p.name AS product_name,
                       o.quantity, o.order_date, o.status
                FROM orders o
                INNER JOIN users u ON o.user_id = u.user_id
                INNER JOIN products p ON o.product_id = p.product_id
                WHERE o.user_id = ?
                ORDER BY o.order_id DESC
                """;
        return jdbcTemplate.query(sql, DETAIL_ROW_MAPPER, userId);
    }

    // orders, users, products 세 테이블을 INNER JOIN해서 주문 상세 정보를 조회한다
    // INNER JOIN을 사용하므로 연결된 유저 또는 상품이 존재하지 않으면 결과가 반환되지 않는다
    public Optional<OrderDetailResponse> findDetailById(Long id) {
        String sql = """
                SELECT o.order_id, u.name AS user_name, p.name AS product_name,
                       o.quantity, o.order_date, o.status
                FROM orders o
                INNER JOIN users u ON o.user_id = u.user_id       -- orders.user_id = users.user_id
                INNER JOIN products p ON o.product_id = p.product_id  -- orders.product_id = products.product_id
                WHERE o.order_id = ?
                """;
        try {
            // queryForObject: 단건 조회. 결과가 없으면 EmptyResultDataAccessException 발생
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, DETAIL_ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            // 해당 order_id가 없을 때 예외 대신 빈 Optional을 반환해 호출부에서 유연하게 처리하도록 한다
            return Optional.empty();
        }
    }
}
