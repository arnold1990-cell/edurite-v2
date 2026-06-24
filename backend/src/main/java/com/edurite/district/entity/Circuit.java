package com.edurite.district.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "circuits")
@Getter
@Setter
public class Circuit extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "district_id", nullable = false)
    private UUID districtId;

    @Column(name = "manager_user_id")
    private UUID managerUserId;

    @Column(nullable = false)
    private boolean active = true;
}
