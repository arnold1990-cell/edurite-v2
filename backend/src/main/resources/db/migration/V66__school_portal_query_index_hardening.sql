CREATE INDEX IF NOT EXISTS idx_teacher_assignments_school_active_teacher
    ON teacher_assignments (school_id, active, teacher_user_id);

CREATE INDEX IF NOT EXISTS idx_teacher_assignments_school_active_class_subject
    ON teacher_assignments (school_id, active, class_id, subject_id);

CREATE INDEX IF NOT EXISTS idx_learner_enrollments_school_active_class_subject
    ON learner_enrollments (school_id, active, class_id, subject_id);
