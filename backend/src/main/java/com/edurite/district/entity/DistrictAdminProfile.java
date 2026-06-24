package com.edurite.district.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "district_admin_profiles")
@Getter
@Setter
public class DistrictAdminProfile extends BaseEntity {

    @Column(name = "district_id", nullable = false)
    private UUID districtId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String title;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean deleted = false;
}
