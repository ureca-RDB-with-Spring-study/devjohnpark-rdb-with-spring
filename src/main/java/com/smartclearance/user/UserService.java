package com.smartclearance.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
