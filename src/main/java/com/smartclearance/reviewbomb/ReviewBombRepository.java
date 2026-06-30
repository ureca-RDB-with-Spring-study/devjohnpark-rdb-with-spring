package com.smartclearance.reviewbomb;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewBombRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReviewBombRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ReviewBombUserEntity> findEverythingUnsafe(String tableName, String keyword) {
        String sql = "SELECT user_id, name, email, password, address FROM " + tableName
                + " WHERE name LIKE '%" + keyword + "%' OR email LIKE '%" + keyword + "%'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ReviewBombUserEntity user = new ReviewBombUserEntity();
            user.id = rs.getLong("user_id");
            user.name = rs.getString("name");
            user.email = rs.getString("email");
            user.password = rs.getString("password");
            user.address = rs.getString("address");
            return user;
        });
    }

    public void insertUnsafe(String name, String email, String password) {
        String sql = "INSERT INTO users (name, email, password) VALUES ('"
                + name + "', '" + email + "', '" + password + "')";
        jdbcTemplate.update(sql);
    }
}
