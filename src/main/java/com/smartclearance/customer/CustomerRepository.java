package com.smartclearance.customer;

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
public class CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Customer> CUSTOMER_ROW_MAPPER = (rs, rowNum) -> Customer.builder()
            .customerId(rs.getLong("customer_id"))
            .name(rs.getString("name"))
            .email(rs.getString("email"))
            .password(rs.getString("password"))
            .address(rs.getString("address"))
            .joinDate(rs.getTimestamp("join_date") != null
                    ? rs.getTimestamp("join_date").toLocalDateTime()
                    : null)
            .build();

    public List<Customer> findAll() {
        String sql = "SELECT customer_id, name, email, password, address, join_date " +
                     "FROM customers ORDER BY customer_id";
        return jdbcTemplate.query(sql, CUSTOMER_ROW_MAPPER);
    }

    public Optional<Customer> findById(Long id) {
        String sql = "SELECT customer_id, name, email, password, address, join_date " +
                     "FROM customers WHERE customer_id = ?";
        try {
            Customer customer = jdbcTemplate.queryForObject(sql, CUSTOMER_ROW_MAPPER, id);
            return Optional.ofNullable(customer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Customer> findByEmail(String email) {
        String sql = "SELECT customer_id, name, email, password, address, join_date " +
                     "FROM customers WHERE email = ?";
        try {
            Customer customer = jdbcTemplate.queryForObject(sql, CUSTOMER_ROW_MAPPER, email);
            return Optional.ofNullable(customer);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM customers WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public Long save(Customer customer) {
        String sql = "INSERT INTO customers (name, email, password, address) " +
                     "VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"customer_id"});
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getEmail());
            ps.setString(3, customer.getPassword());
            ps.setString(4, customer.getAddress());
            return ps;
        }, keyHolder);

        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM customers";
        Long total = jdbcTemplate.queryForObject(sql, Long.class);
        return total != null ? total : 0L;
    }
}