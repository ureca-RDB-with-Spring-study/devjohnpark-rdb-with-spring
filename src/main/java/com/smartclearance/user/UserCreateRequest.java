package com.smartclearance.user;

import java.time.LocalDate;

public record UserCreateRequest(
        String name,
        String email,
        String password,
        String address,
        LocalDate birthDate
) {}
