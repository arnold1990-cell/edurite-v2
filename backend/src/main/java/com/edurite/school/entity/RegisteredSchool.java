package com.edurite.school.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "registered_schools")
@Getter
@Setter
public class RegisteredSchool extends BaseEntity {

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    @Column(name = "emis_number", nullable = false)
    private String emisNumber;

    @Column(name = "school_code")
    private String schoolCode;

    @Column(name = "province_id")
    private UUID provinceId;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String district;

    private String circuit;

    @Column(name = "school_type")
    private String schoolType;

    @Column(name = "physical_address", length = 2000)
    private String physicalAddress;

    @Column(name = "principal_name")
    private String principalName;

    @Column(name = "school_email")
    private String schoolEmail;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(nullable = false)
    private String source = "DIRECTORY";

    @Column(name = "registered_status", nullable = false)
    private String registeredStatus = "REGISTERED";

    @Column(nullable = false)
    private boolean active = true;
}
