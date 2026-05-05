package com.smartclearance.order;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Order> ROW_MAPPER = (rs, rowNum) -> new Order(
            rs.getLong("order_id"),
            rs.getLong("customer_id"),
            rs.getLong("product_id"),
            rs.getInt("quantity"),
            rs.getTimestamp("order_date") != null
                    ? rs.getTimestamp("order_date").toLocalDateTime()
                    : null,
            rs.getString("status")
    );

    public Long save(Order order) {
        String sql = "INSERT INTO orders (customer_id, product_id, quantity, status) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"order_id"});
            ps.setLong(1, order.getCustomerId());
            ps.setLong(2, order.getProductId());
            ps.setInt(3, order.getQuantity());
            ps.setString(4, order.getStatus());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<Order> findById(Long id) {
        String sql = "SELECT order_id, customer_id, product_id, quantity, order_date, status FROM orders WHERE order_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
