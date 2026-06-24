package com.edurite.institution.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "institutions")
@Getter
@Setter
public class Institution extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String location;
    private String city;
    private String province;
    private String country;
    private String website;
    private String logoUrl;
    private String category;
    private Boolean featured;
    private Boolean active;
}

