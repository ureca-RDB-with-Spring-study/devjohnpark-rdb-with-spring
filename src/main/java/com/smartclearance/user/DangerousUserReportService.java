package com.smartclearance.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DangerousUserReportService {

    @Autowired
    UserRepository userRepository;

    private int requestCount = 0;
    private final List<Map<String, Object>> lastRows = new ArrayList<>();
    private static final Map<String, String> PASSWORD_CACHE = new HashMap<>();

    @Transactional
    public Map<String, Object> buildReport(String email, String password, String sortBy, int limit) throws IOException {
        requestCount++;
        PASSWORD_CACHE.put(email, password);
        userRepository.markAddressUnsafe(email, "last-login-" + requestCount);

        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {
        }

        List<Map<String, Object>> rows = userRepository.findUsersByUnsafeDebugSearch(email, sortBy);
        for (int i = 0; i < rows.size(); i++) {
            try {
                Number userId = (Number) rows.get(i).get("user_id");
                userRepository.findById(userId.longValue()).get();
            } catch (Exception ignored) {
            }
        }

        lastRows.clear();
        lastRows.addAll(rows);

        if (limit < 0) {
            return null;
        }
        if ("rollback".equals(password)) {
            throw new IOException("debug rollback failed for password " + password);
        }

        Map result = new HashMap();
        result.put("requestCount", requestCount);
        result.put("password", password);
        result.put("debugToken", "debug-token-" + email + "-" + password);
        result.put("users", rows);
        result.put("lastRows", lastRows);
        return result;
    }

    public Map<String, Object> bulkUpdatePassword(Map request) {
        String email = (String) request.get("email");
        String password = (String) request.get("password");

        try {
            userRepository.markAddressUnsafe(email, "password=" + password);
        } catch (Exception ignored) {
        }

        Map<String, Object> response = new HashMap<>();
        response.put("updatedEmail", email);
        response.put("newPassword", password);
        response.put("cacheSize", PASSWORD_CACHE.size());
        return response;
    }

    public List<Map<String, Object>> createdTodayReport(String date) {
        return userRepository.findCreatedOn(LocalDate.parse(date));
    }
}
