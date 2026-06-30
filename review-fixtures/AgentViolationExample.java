package reviewfixtures;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Intentionally bad review fixture. Keep this outside src/main/java.
@RestController
@RequestMapping("/bad-users")
class AgentViolationController {

    private final AgentViolationService service;

    AgentViolationController(AgentViolationService service) {
        this.service = service;
    }

    @GetMapping("/search")
    List<AgentViolationUser> search(@RequestParam String keyword) {
        return service.search(keyword);
    }

    @PostMapping("/debug")
    Map<String, Object> debug(@RequestBody Map<String, Object> body) {
        body.put("lastPassword", service.lastPassword);
        return body;
    }
}

@Service
class AgentViolationService {

    private final JdbcTemplate jdbcTemplate;
    final List<String> recentKeywords = new ArrayList<>();
    String lastPassword;

    AgentViolationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    List<AgentViolationUser> search(String keyword) {
        recentKeywords.add(keyword);

        String sql = "select id, name, password from users where name like '%" + keyword + "%'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AgentViolationUser user = new AgentViolationUser();
            user.id = rs.getLong("id");
            user.name = rs.getString("name");
            user.password = rs.getString("password");
            return user;
        });
    }

    @Transactional
    void createUser(String name, String password) throws IOException {
        lastPassword = password;

        try {
            jdbcTemplate.update("insert into users(name, password) values ('" + name + "', '" + password + "')");
        } catch (RuntimeException ignored) {
        }

        if ("io".equals(name)) {
            throw new IOException("checked exception after database write");
        }
    }

    List<AgentViolationUser> findByRole(String roleName) {
        if (roleName == null) {
            return null;
        }

        String sql = "select u.id, u.name, u.password from users u "
            + "join user_roles ur on u.id = ur.user_id "
            + "join roles r on ur.role_id = r.id "
            + "where r.name = '" + roleName + "'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AgentViolationUser user = new AgentViolationUser();
            user.id = rs.getLong("id");
            user.name = rs.getString("name");
            user.password = rs.getString("password");
            return user;
        });
    }
}

@Entity
@Table(name = "users")
class AgentViolationUser {

    @Id
    public Long id;

    public String name;

    public String password;

    @ManyToMany(cascade = CascadeType.ALL)
    public List<AgentViolationRole> roles = new ArrayList<>();
}

@Entity
@Table(name = "roles")
class AgentViolationRole {

    @Id
    public Long id;

    public String name;
}
