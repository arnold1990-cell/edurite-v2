CREATE TABLE IF NOT EXISTS subject_catalogue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    phase VARCHAR(40) NOT NULL,
    grade_range VARCHAR(40) NOT NULL,
    subject_type VARCHAR(80),
    language_level VARCHAR(80),
    is_language BOOLEAN NOT NULL DEFAULT FALSE,
    is_compulsory BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_subject_catalogue UNIQUE (name, phase, grade_range, language_level)
);

ALTER TABLE school_subjects
    ADD COLUMN IF NOT EXISTS subject_catalogue_id UUID REFERENCES subject_catalogue(id),
    ADD COLUMN IF NOT EXISTS grade_range VARCHAR(40),
    ADD COLUMN IF NOT EXISTS language_level VARCHAR(80),
    ADD COLUMN IF NOT EXISTS subject_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS is_language BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_compulsory BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE teacher_assignments
    ADD COLUMN IF NOT EXISTS phase VARCHAR(40),
    ADD COLUMN IF NOT EXISTS grade VARCHAR(40),
    ADD COLUMN IF NOT EXISTS is_class_teacher BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_subject_catalogue_phase ON subject_catalogue (phase, grade_range);
CREATE INDEX IF NOT EXISTS idx_school_subjects_catalogue ON school_subjects (school_id, subject_catalogue_id);

INSERT INTO subject_catalogue (name, phase, grade_range, subject_type, language_level, is_language, is_compulsory, active)
SELECT v.name, v.phase, v.grade_range, v.subject_type, v.language_level, v.is_language, v.is_compulsory, TRUE
FROM (
    VALUES
        ('Life Skills', 'ECD', 'Grade R', 'Core', NULL, FALSE, TRUE),
        ('Language Development', 'ECD', 'Grade R', 'Language', NULL, TRUE, TRUE),
        ('Numeracy', 'ECD', 'Grade R', 'Core', NULL, FALSE, TRUE),
        ('Creative Arts', 'ECD', 'Grade R', 'Arts', NULL, FALSE, TRUE),
        ('Physical Development', 'ECD', 'Grade R', 'Development', NULL, FALSE, TRUE),
        ('Social and Emotional Development', 'ECD', 'Grade R', 'Development', NULL, FALSE, TRUE),
        ('Home Language', 'Foundation', 'Grades 1-3', 'Language', 'Home Language', TRUE, TRUE),
        ('First Additional Language', 'Foundation', 'Grades 1-3', 'Language', 'First Additional Language', TRUE, TRUE),
        ('Mathematics', 'Foundation', 'Grades 1-3', 'Core', NULL, FALSE, TRUE),
        ('Life Skills', 'Foundation', 'Grades 1-3', 'Core', NULL, FALSE, TRUE),
        ('Home Language', 'Intermediate', 'Grades 4-6', 'Language', 'Home Language', TRUE, TRUE),
        ('First Additional Language', 'Intermediate', 'Grades 4-6', 'Language', 'First Additional Language', TRUE, TRUE),
        ('Mathematics', 'Intermediate', 'Grades 4-6', 'Core', NULL, FALSE, TRUE),
        ('Natural Sciences and Technology', 'Intermediate', 'Grades 4-6', 'Science', NULL, FALSE, TRUE),
        ('Social Sciences', 'Intermediate', 'Grades 4-6', 'Humanities', NULL, FALSE, TRUE),
        ('Life Skills', 'Intermediate', 'Grades 4-6', 'Core', NULL, FALSE, TRUE),
        ('Home Language', 'Senior', 'Grades 7-9', 'Language', 'Home Language', TRUE, TRUE),
        ('First Additional Language', 'Senior', 'Grades 7-9', 'Language', 'First Additional Language', TRUE, TRUE),
        ('Mathematics', 'Senior', 'Grades 7-9', 'Core', NULL, FALSE, TRUE),
        ('Natural Sciences', 'Senior', 'Grades 7-9', 'Science', NULL, FALSE, TRUE),
        ('Social Sciences', 'Senior', 'Grades 7-9', 'Humanities', NULL, FALSE, TRUE),
        ('Technology', 'Senior', 'Grades 7-9', 'Technology', NULL, FALSE, TRUE),
        ('Economic and Management Sciences', 'Senior', 'Grades 7-9', 'Commerce', NULL, FALSE, TRUE),
        ('Life Orientation', 'Senior', 'Grades 7-9', 'Core', NULL, FALSE, TRUE),
        ('Creative Arts', 'Senior', 'Grades 7-9', 'Arts', NULL, FALSE, TRUE),
        ('Home Language', 'FET', 'Grades 10-12', 'Language', 'Home Language', TRUE, TRUE),
        ('First Additional Language', 'FET', 'Grades 10-12', 'Language', 'First Additional Language', TRUE, TRUE),
        ('Second Additional Language', 'FET', 'Grades 10-12', 'Language', 'Second Additional Language', TRUE, FALSE),
        ('Mathematics', 'FET', 'Grades 10-12', 'Core', NULL, FALSE, TRUE),
        ('Mathematical Literacy', 'FET', 'Grades 10-12', 'Core', NULL, FALSE, TRUE),
        ('Life Orientation', 'FET', 'Grades 10-12', 'Core', NULL, FALSE, TRUE),
        ('Physical Sciences', 'FET', 'Grades 10-12', 'Science', NULL, FALSE, FALSE),
        ('Life Sciences', 'FET', 'Grades 10-12', 'Science', NULL, FALSE, FALSE),
        ('Agricultural Sciences', 'FET', 'Grades 10-12', 'Science', NULL, FALSE, FALSE),
        ('Accounting', 'FET', 'Grades 10-12', 'Commerce', NULL, FALSE, FALSE),
        ('Business Studies', 'FET', 'Grades 10-12', 'Commerce', NULL, FALSE, FALSE),
        ('Economics', 'FET', 'Grades 10-12', 'Commerce', NULL, FALSE, FALSE),
        ('Geography', 'FET', 'Grades 10-12', 'Humanities', NULL, FALSE, FALSE),
        ('History', 'FET', 'Grades 10-12', 'Humanities', NULL, FALSE, FALSE),
        ('Computer Applications Technology', 'FET', 'Grades 10-12', 'Technology', NULL, FALSE, FALSE),
        ('Information Technology', 'FET', 'Grades 10-12', 'Technology', NULL, FALSE, FALSE),
        ('Engineering Graphics and Design', 'FET', 'Grades 10-12', 'Technology', NULL, FALSE, FALSE),
        ('Civil Technology', 'FET', 'Grades 10-12', 'Technology', NULL, FALSE, FALSE),
        ('Electrical Technology', 'FET', 'Grades 10-12', 'Technology', NULL, FALSE, FALSE),
        ('Mechanical Technology', 'FET', 'Grades 10-12', 'Technology', NULL, FALSE, FALSE),
        ('Visual Arts', 'FET', 'Grades 10-12', 'Arts', NULL, FALSE, FALSE),
        ('Dramatic Arts', 'FET', 'Grades 10-12', 'Arts', NULL, FALSE, FALSE),
        ('Music', 'FET', 'Grades 10-12', 'Arts', NULL, FALSE, FALSE),
        ('Dance Studies', 'FET', 'Grades 10-12', 'Arts', NULL, FALSE, FALSE),
        ('Design', 'FET', 'Grades 10-12', 'Arts', NULL, FALSE, FALSE),
        ('Tourism', 'FET', 'Grades 10-12', 'Services', NULL, FALSE, FALSE),
        ('Hospitality Studies', 'FET', 'Grades 10-12', 'Services', NULL, FALSE, FALSE),
        ('Consumer Studies', 'FET', 'Grades 10-12', 'Services', NULL, FALSE, FALSE),
        ('Afrikaans', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('English', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('isiZulu', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('isiXhosa', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('Sesotho', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('Setswana', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('Sepedi', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('Xitsonga', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('Tshivenda', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('Siswati', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('isiNdebele', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE),
        ('Sign Language', 'FET', 'Grades 10-12', 'Language', NULL, TRUE, FALSE)
) AS v(name, phase, grade_range, subject_type, language_level, is_language, is_compulsory)
WHERE NOT EXISTS (
    SELECT 1
    FROM subject_catalogue existing
    WHERE existing.name = v.name
      AND existing.phase = v.phase
      AND existing.grade_range = v.grade_range
      AND COALESCE(existing.language_level, '') = COALESCE(v.language_level, '')
);

UPDATE school_subjects
SET grade_range = COALESCE(grade_range,
        CASE
            WHEN phase = 'ECD' THEN 'Grade R'
            WHEN phase = 'Foundation' THEN 'Grades 1-3'
            WHEN phase = 'Intermediate' THEN 'Grades 4-6'
            WHEN phase = 'Senior' THEN 'Grades 7-9'
            WHEN phase = 'FET' THEN 'Grades 10-12'
            ELSE COALESCE(grade, '')
        END),
    subject_type = COALESCE(subject_type,
        CASE
            WHEN subject_name ILIKE '%language%' OR subject_name IN ('Afrikaans', 'English', 'isiZulu', 'isiXhosa', 'Sesotho', 'Setswana', 'Sepedi', 'Xitsonga', 'Tshivenda', 'Siswati', 'isiNdebele', 'Sign Language')
                THEN 'Language'
            WHEN subject_name ILIKE '%science%' THEN 'Science'
            WHEN subject_name ILIKE '%technology%' OR subject_name IN ('Information Technology', 'Computer Applications Technology', 'Engineering Graphics and Design')
                THEN 'Technology'
            WHEN subject_name IN ('Accounting', 'Business Studies', 'Economics', 'Economic and Management Sciences')
                THEN 'Commerce'
            WHEN subject_name ILIKE '%arts%' OR subject_name IN ('Music', 'Design', 'Dance Studies')
                THEN 'Arts'
            ELSE 'Core'
        END),
    is_language = COALESCE(is_language, FALSE) OR subject_name ILIKE '%language%' OR subject_name IN ('Afrikaans', 'English', 'isiZulu', 'isiXhosa', 'Sesotho', 'Setswana', 'Sepedi', 'Xitsonga', 'Tshivenda', 'Siswati', 'isiNdebele', 'Sign Language'),
    language_level = COALESCE(language_level,
        CASE
            WHEN subject_name = 'Home Language' THEN 'Home Language'
            WHEN subject_name = 'First Additional Language' THEN 'First Additional Language'
            WHEN subject_name = 'Second Additional Language' THEN 'Second Additional Language'
            ELSE NULL
        END),
    subject_catalogue_id = COALESCE(subject_catalogue_id, (
        SELECT sc.id
        FROM subject_catalogue sc
        WHERE sc.name = school_subjects.subject_name
          AND sc.phase = school_subjects.phase
        ORDER BY sc.created_at
        LIMIT 1
    ));

UPDATE teacher_assignments ta
SET phase = COALESCE(ta.phase, ss.phase),
    grade = COALESCE(ta.grade, ss.grade, sc.grade)
FROM school_subjects ss,
     school_classes sc
WHERE ta.subject_id = ss.id
  AND ta.class_id = sc.id;
