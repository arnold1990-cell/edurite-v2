CREATE TABLE IF NOT EXISTS schools (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(120),
    district VARCHAR(120),
    province VARCHAR(120),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(40),
    address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS school_user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    user_id UUID NOT NULL REFERENCES users(id),
    role_name VARCHAR(64) NOT NULL,
    employee_or_student_no VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_school_user UNIQUE (school_id, user_id)
);

CREATE TABLE IF NOT EXISTS school_classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    grade VARCHAR(20) NOT NULL,
    class_name VARCHAR(80) NOT NULL,
    academic_year INTEGER NOT NULL,
    term VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_school_class UNIQUE (school_id, grade, class_name, academic_year)
);

CREATE TABLE IF NOT EXISTS school_subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    subject_name VARCHAR(120) NOT NULL,
    phase VARCHAR(40) NOT NULL,
    grade VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS teacher_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    teacher_user_id UUID NOT NULL REFERENCES users(id),
    class_id UUID NOT NULL REFERENCES school_classes(id),
    subject_id UUID NOT NULL REFERENCES school_subjects(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_teacher_assignment UNIQUE (school_id, teacher_user_id, class_id, subject_id)
);

CREATE TABLE IF NOT EXISTS learner_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    learner_user_id UUID NOT NULL REFERENCES users(id),
    class_id UUID NOT NULL REFERENCES school_classes(id),
    subject_id UUID NOT NULL REFERENCES school_subjects(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_learner_enrollment UNIQUE (school_id, learner_user_id, class_id, subject_id)
);

CREATE TABLE IF NOT EXISTS learning_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    class_id UUID NOT NULL REFERENCES school_classes(id),
    subject_id UUID NOT NULL REFERENCES school_subjects(id),
    teacher_user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    note_text TEXT,
    pdf_url TEXT,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS school_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    class_id UUID NOT NULL REFERENCES school_classes(id),
    subject_id UUID NOT NULL REFERENCES school_subjects(id),
    teacher_user_id UUID NOT NULL REFERENCES users(id),
    task_type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    instructions TEXT,
    due_at TIMESTAMPTZ NOT NULL,
    term VARCHAR(20),
    max_marks NUMERIC(8,2) NOT NULL DEFAULT 100,
    rubric TEXT,
    released BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS task_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES school_tasks(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS task_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES school_tasks(id) ON DELETE CASCADE,
    learner_user_id UUID NOT NULL REFERENCES users(id),
    submission_text TEXT,
    file_url TEXT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    late BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_submission UNIQUE (task_id, learner_user_id)
);

CREATE TABLE IF NOT EXISTS submission_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES task_submissions(id) ON DELETE CASCADE,
    teacher_user_id UUID NOT NULL REFERENCES users(id),
    marks_awarded NUMERIC(8,2),
    comments TEXT,
    rubric_scoring TEXT,
    released BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_submission_feedback UNIQUE (submission_id)
);

CREATE TABLE IF NOT EXISTS plagiarism_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES task_submissions(id) ON DELETE CASCADE,
    compared_submission_id UUID REFERENCES task_submissions(id),
    similarity_percentage NUMERIC(5,2) NOT NULL,
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    report_details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS school_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    class_id UUID NOT NULL REFERENCES school_classes(id),
    subject_id UUID NOT NULL REFERENCES school_subjects(id),
    teacher_user_id UUID NOT NULL REFERENCES users(id),
    assessment_type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_at TIMESTAMPTZ,
    max_marks NUMERIC(8,2) NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessment_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES school_assessments(id) ON DELETE CASCADE,
    learner_user_id UUID NOT NULL REFERENCES users(id),
    marks_awarded NUMERIC(8,2) NOT NULL,
    comments TEXT,
    released BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_assessment_result UNIQUE (assessment_id, learner_user_id)
);

INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_SCHOOL_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_SCHOOL_ADMIN');

INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_TEACHER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_TEACHER');

INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_SCHOOL_STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_SCHOOL_STUDENT');

CREATE INDEX IF NOT EXISTS idx_school_user_profiles_school_user ON school_user_profiles (school_id, user_id);
CREATE INDEX IF NOT EXISTS idx_teacher_assignments_teacher ON teacher_assignments (teacher_user_id, school_id);
CREATE INDEX IF NOT EXISTS idx_learner_enrollments_learner ON learner_enrollments (learner_user_id, school_id);
CREATE INDEX IF NOT EXISTS idx_tasks_scope ON school_tasks (school_id, class_id, subject_id);
CREATE INDEX IF NOT EXISTS idx_submissions_task ON task_submissions (task_id, learner_user_id);
CREATE INDEX IF NOT EXISTS idx_notes_scope ON learning_notes (school_id, class_id, subject_id);
