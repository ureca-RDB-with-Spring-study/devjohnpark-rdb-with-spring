package com.smartclearance.reviewtrap;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "debug_customer_profiles")
public class DebugCustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String email;
    public String password;

    @ManyToMany
    public List<DebugRole> roles = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    public List<DebugLoginEvent> events = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    public Map<String, Object> payload = new HashMap<>();

    public DebugCustomerProfile() {
    }
}
