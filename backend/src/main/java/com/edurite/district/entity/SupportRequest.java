package com.edurite.district.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "support_requests")
@Getter
@Setter
public class SupportRequest extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column
    private String subject;

    @Column
    private String grade;

    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(name = "assigned_to")
    private UUID assignedTo;
}
