package com.smartclearance.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {

    private final Long userId;
    private final String name;
    private final String email;
    private final String password;
    private final String address;
    private final LocalDate birthDate;
    private final LocalDateTime createdAt;

    public User(Long userId, String name, String email, String password, String address, LocalDate birthDate, LocalDateTime createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.address = address;
        this.birthDate = birthDate;
        this.createdAt = createdAt;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getAddress() { return address; }
    public LocalDate getBirthDate() { return birthDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
