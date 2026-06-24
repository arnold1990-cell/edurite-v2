package com.edurite.district.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "districts")
@Getter
@Setter
public class District extends BaseEntity {

    @Column(name = "district_name", nullable = false)
    private String districtName;

    @Column(name = "district_code")
    private String districtCode;

    @Column(name = "province_id")
    private UUID provinceId;

    private String province;

    @Column(name = "director_name")
    private String directorName;

    @Column(name = "admin_name")
    private String adminName;

    @Column(name = "admin_email")
    private String adminEmail;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "licensing_status", nullable = false)
    private String licensingStatus = "ACTIVE";

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private boolean active = true;
}
