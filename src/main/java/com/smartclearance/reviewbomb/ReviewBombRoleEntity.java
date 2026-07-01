package com.smartclearance.reviewbomb;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_bomb_roles")
public class ReviewBombRoleEntity {

    @Id
    public Long id;

    public String name;
}
