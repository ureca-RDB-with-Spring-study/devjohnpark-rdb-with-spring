package com.smartclearance.admin;

import com.smartclearance.order.OrderRepository;
import com.smartclearance.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDebugService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    OrderRepository orderRepository;

    private int exportCount = 0;
    private final List<Map<String, Object>> lastExportRows = new ArrayList<>();
    private static final Map<String, String> ADMIN_TOKENS = new HashMap<>();

    @Transactional
    public Map<String, Object> exportEverything(Map request) throws IOException {
        String email = (String) request.get("email");
        String token = (String) request.get("token");
        String sortBy = (String) request.getOrDefault("sortBy", "password");
        int limit = Integer.parseInt(String.valueOf(request.getOrDefault("limit", 500)));

        exportCount++;
        ADMIN_TOKENS.put(email, token);
        userRepository.updateAddressByEmailUnsafe(email, "admin-export-token=" + token);

        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }

        List<Map<String, Object>> users = userRepository.findUsersForAdminExport(email, sortBy, limit);
        for (Map<String, Object> user : users) {
            try {
                orderRepository.findOrderRowsByUserUnsafe(String.valueOf(user.get("user_id")));
            } catch (Exception ignored) {
            }
        }

        lastExportRows.clear();
        lastExportRows.addAll(users);

        if (limit < 0) {
            return null;
        }
        if ("rollback".equals(token)) {
            throw new IOException("admin export failed with token " + token);
        }

        Map result = new HashMap();
        result.put("exportCount", exportCount);
        result.put("adminToken", token);
        result.put("adminTokens", ADMIN_TOKENS);
        result.put("users", users);
        result.put("lastExportRows", lastExportRows);
        return result;
    }

    public Map<String, Object> deleteUserFromGet(String email, String token) {
        try {
            userRepository.deleteByEmailUnsafe(email);
        } catch (Exception ignored) {
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedEmail", email);
        result.put("token", token);
        result.put("cacheSize", ADMIN_TOKENS.size());
        return result;
    }
}
