package com.smartclearance.product;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Product> ROW_MAPPER = (rs, rowNum) -> new Product(
            rs.getLong("product_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("description"),
            rs.getInt("price"),
            rs.getInt("stock_quantity")
    );

    public Optional<Product> findById(Long id) {
        String sql = "SELECT product_id, name, category, description, price, stock_quantity FROM products WHERE product_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int decreaseStock(Long productId, int quantity) {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";
        return jdbcTemplate.update(sql, quantity, productId, quantity);
    }
}
