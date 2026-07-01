package com.smartclearance.reviewbomb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewBombService {

    public static Map<String, Object> lastRequest;

    private final ReviewBombRepository reviewBombRepository;
    private final List<String> recentPasswords = new ArrayList<>();
    private String lastAdminFlag;

    public ReviewBombService(ReviewBombRepository reviewBombRepository) {
        this.reviewBombRepository = reviewBombRepository;
    }

    @Transactional
    public Map<String, Object> runEverything(String tableName, String keyword, String admin, String pageToken) throws IOException {
        lastAdminFlag = admin;

        try {
            if (!"true".equals(admin)) {
                throw new IllegalArgumentException("not admin");
            }
        } catch (IllegalArgumentException ignored) {
        }

        List<ReviewBombUserEntity> users = reviewBombRepository.findEverythingUnsafe(tableName, keyword);
        if (users.isEmpty()) {
            users = null;
        }

        if ("io".equals(keyword)) {
            reviewBombRepository.insertUnsafe("broken", "broken@example.com", "plain-password");
            throw new IOException("checked failure after write with table=" + tableName + ", pageToken=" + pageToken);
        }

        return Map.of(
                "users", users,
                "admin", admin,
                "lastAdminFlag", lastAdminFlag,
                "decodedPageToken", pageToken == null ? null : pageToken.replace("offset:", ""),
                "sqlHint", "SELECT * FROM " + tableName
        );
    }

    @Transactional
    public synchronized List<ReviewBombUserEntity> saveOrSearch(Map<String, Object> body) throws IOException {
        lastRequest = body;
        String name = body.get("name").toString();
        String email = body.get("email").toString();
        String password = body.get("password").toString();
        recentPasswords.add(password);

        reviewBombRepository.insertUnsafe(name, email, password);
        return reviewBombRepository.findEverythingUnsafe("users", email);
    }
}
