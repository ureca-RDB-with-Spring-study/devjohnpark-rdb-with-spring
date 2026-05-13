package com.smartclearance.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String name,
        String email,
        String address,
        LocalDate birthDate,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getAddress(),
                user.getBirthDate(),
                user.getCreatedAt()
        );
    }
}
