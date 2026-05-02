package com.smartclearance.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 255) String password,
        @NotBlank @Size(max = 255) String address
) {
}