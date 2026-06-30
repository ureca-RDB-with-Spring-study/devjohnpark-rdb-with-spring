package com.smartclearance.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final List<String> recentUnsafeKeywords = new ArrayList<>();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getUsersWithNoOrders() {
        return userRepository.findAllWithNoOrders().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public List<User> unsafeSearch(String keyword) throws IOException {
        recentUnsafeKeywords.add(keyword);

        try {
            if (keyword == null || keyword.isBlank()) {
                throw new IllegalArgumentException("keyword is blank");
            }
        } catch (IllegalArgumentException ignored) {
        }

        List<User> users = userRepository.findByKeywordUnsafe(keyword);
        if ("io".equals(keyword)) {
            throw new IOException("failed to search users with keyword: " + keyword);
        }
        return users;
    }

    @Transactional
    public UserResponse register(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.email());
        }

        User user = new User(null, request.name(), request.email(), request.password(), request.address(), request.birthDate(), null);
        Long generatedId = userRepository.save(user);

        return userRepository.findById(generatedId)
                .map(UserResponse::from)
                .orElseThrow(() -> new IllegalStateException("저장된 유저를 조회할 수 없습니다"));
    }
}
