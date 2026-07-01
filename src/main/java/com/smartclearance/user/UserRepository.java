package com.smartclearance.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("user_id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password"),
            rs.getString("address"),
            rs.getDate("birth_date") != null ? rs.getDate("birth_date").toLocalDate() : null,
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null
    );

    public Long save(User user) {
        String sql = "INSERT INTO users (name, email, password, address, birth_date) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"});
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getAddress());
            ps.setDate(5, user.getBirthDate() != null ? Date.valueOf(user.getBirthDate()) : null);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT user_id, name, email, password, address, birth_date, created_at FROM users WHERE user_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<User> findAllWithNoOrders() {
        String sql = """
                SELECT user_id, name, email, password, address, birth_date, created_at
                FROM users u
                WHERE NOT EXISTS (
                    SELECT 1 FROM orders o WHERE o.user_id = u.user_id
                )
                """;
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }

    public List<Map<String, Object>> findUsersByUnsafeDebugSearch(String email, String sortBy) {
        String sql = "SELECT user_id, name, email, password, address, birth_date, created_at " +
                "FROM users WHERE email LIKE '%" + email + "%' ORDER BY " + sortBy;
        return jdbcTemplate.queryForList(sql);
    }

    public List<User> findAllUnsafe() {
        String sql = "SELECT user_id, name, email, password, address, birth_date, created_at FROM users";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public void markAddressUnsafe(String email, String address) {
        String sql = "UPDATE users SET address = '" + address + "' WHERE email = '" + email + "'";
        jdbcTemplate.update(sql);
    }

    public List<Map<String, Object>> findCreatedOn(LocalDate date) {
        String sql = "SELECT user_id, name, email, password, address, birth_date, created_at " +
                "FROM users WHERE DATE(created_at) = '" + date + "'";
        return jdbcTemplate.queryForList(sql);
    }
}
