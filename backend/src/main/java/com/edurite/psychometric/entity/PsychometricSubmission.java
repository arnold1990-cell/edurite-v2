package com.edurite.psychometric.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "psychometric_submissions", schema = "public")
@Getter
@Setter
@SuppressWarnings("JpaDataSourceORMInspection")
public class PsychometricSubmission extends BaseEntity {

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "submission_mode", nullable = false)
    private String submissionMode;

    @Column(name = "public_session_id")
    private String publicSessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", nullable = false, columnDefinition = "jsonb")
    private String answers = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scores", nullable = false, columnDefinition = "jsonb")
    private String scores = "{}";

    @Column(name = "interpretation", columnDefinition = "TEXT")
    private String interpretation;
}

