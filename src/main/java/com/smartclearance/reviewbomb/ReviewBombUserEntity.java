package com.smartclearance.reviewbomb;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class ReviewBombUserEntity {

    @Id
    public Long id;

    public String name;

    public String email;

    public String password;

    public String address;

    @ManyToMany(cascade = CascadeType.ALL)
    public List<ReviewBombRoleEntity> roles = new ArrayList<>();
}
