package com.edurite.school.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school_profiles")
@Getter
@Setter
public class SchoolProfile extends BaseEntity {

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    private String country;
    private String city;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(columnDefinition = "TEXT")
    private String notes;
}

