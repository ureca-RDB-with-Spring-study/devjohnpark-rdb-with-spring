package com.smartclearance.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
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

    public List<User> findByKeywordUnsafe(String keyword) {
        String sql = """
                SELECT user_id, name, email, password, address, birth_date, created_at
                FROM users
                WHERE name LIKE '%""" + keyword + "%' OR email LIKE '%" + keyword + "%'";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }
}
