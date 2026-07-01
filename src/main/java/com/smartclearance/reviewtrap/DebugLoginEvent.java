package com.smartclearance.reviewtrap;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class DebugLoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String ipAddress;
    public String userAgent;
    public LocalDateTime loginTime;

    public DebugLoginEvent() {
    }
}
