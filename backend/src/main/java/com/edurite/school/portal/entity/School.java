package com.edurite.school.portal.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "schools")
@Getter
@Setter
public class School extends BaseEntity {

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "school_code")
    private String schoolCode;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "district_id")
    private java.util.UUID districtId;

    private String district;
    private String province;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    private String address;
}


