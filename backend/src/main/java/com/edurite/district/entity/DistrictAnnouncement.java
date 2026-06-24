package com.edurite.district.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "district_announcements")
@Getter
@Setter
public class DistrictAnnouncement extends BaseEntity {

    @Column(name = "district_id", nullable = false)
    private UUID districtId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(nullable = false)
    private String audience;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "delivery_scope", nullable = false)
    private String deliveryScope = "ALL_SCHOOLS";

    @Column(nullable = false)
    private String status = "SENT";

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(nullable = false)
    private boolean active = true;
}
