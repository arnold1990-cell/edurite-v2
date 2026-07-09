package com.edurite.user.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles", schema = "public")
@Getter
@Setter
public class Role extends BaseEntity {

    // Unique role name used for authorization (e.g. ADMIN, TEACHER, STUDENT).
    @Column(name = "name", nullable = false, unique = true)
    private String name;
}