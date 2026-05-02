package com.smartclearance.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    private Long customerId;
    private String name;
    private String email;
    private String password;
    private String address;
    private LocalDateTime joinDate;
}