CREATE TABLE IF NOT EXISTS atp_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phase VARCHAR(80) NOT NULL,
    grade VARCHAR(40) NOT NULL,
    subject_catalogue_id UUID NULL REFERENCES subject_catalogue(id),
    subject_name VARCHAR(160) NOT NULL,
    academic_year INTEGER NOT NULL,
    term VARCHAR(32) NOT NULL,
    week_number INTEGER NULL,
    topic VARCHAR(255) NOT NULL,
    subtopic VARCHAR(255) NULL,
    recommended_activities TEXT NULL,
    assessment_guidance TEXT NULL,
    caps_reference VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_atp_topics_phase_grade_term ON atp_topics (phase, grade, term);
CREATE INDEX IF NOT EXISTS idx_atp_topics_subject_name ON atp_topics (subject_name);

ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS atp_topic_id UUID NULL;
ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS academic_year INTEGER NULL;
ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS phase VARCHAR(80) NULL;
ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS grade VARCHAR(40) NULL;
ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS assessment_type VARCHAR(120) NULL;
ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS week_number INTEGER NULL;
ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS resources_materials TEXT NULL;
ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS cognitive_level VARCHAR(120) NULL;
ALTER TABLE school_tasks ADD COLUMN IF NOT EXISTS assessment_category VARCHAR(80) NULL;

ALTER TABLE school_tasks
    ADD CONSTRAINT fk_school_tasks_atp_topic
    FOREIGN KEY (atp_topic_id) REFERENCES atp_topics(id);

INSERT INTO atp_topics (
    phase,
    grade,
    subject_catalogue_id,
    subject_name,
    academic_year,
    term,
    week_number,
    topic,
    subtopic,
    recommended_activities,
    assessment_guidance,
    caps_reference,
    active
)
SELECT
    seed.phase,
    seed.grade,
    catalogue.id,
    seed.subject_name,
    seed.academic_year,
    seed.term,
    seed.week_number,
    seed.topic,
    seed.subtopic,
    seed.recommended_activities,
    seed.assessment_guidance,
    seed.caps_reference,
    TRUE
FROM (
    VALUES
        ('FET', 'Grade 10', 'Mathematics', 2026, 'Term 1', 1, 'Number Patterns and Sequences', 'Arithmetic and geometric patterns', 'Analyse visual and numeric patterns and model them in classwork.', 'Short diagnostic baseline task on pattern rules and sequence completion.', 'CAPS Grade 10 Mathematics Term 1'),
        ('FET', 'Grade 10', 'Mathematics', 2026, 'Term 1', 2, 'Functions and Relationships', 'Tables, graphs, and symbolic rules', 'Plot linear relations and interpret gradient and intercept in context.', 'Informal class activity with graph interpretation and correction feedback.', 'CAPS Grade 10 Mathematics Term 1'),
        ('FET', 'Grade 10', 'Mathematics', 2026, 'Term 1', 3, 'Finance, Growth and Decay', 'Simple and compound growth contexts', 'Use worked examples and spreadsheet-style tables for growth scenarios.', 'Formal SBA-style problem-solving exercise with reasoning marks.', 'CAPS Grade 10 Mathematics Term 1'),
        ('FET', 'Grade 10', 'English', 2026, 'Term 1', 1, 'Listening and Speaking', 'Prepared speech and oral response', 'Model oral presentations and peer feedback using a CAPS-aligned rubric.', 'Informal oral observation focused on clarity, audience, and structure.', 'CAPS Grade 10 English FAL Term 1'),
        ('FET', 'Grade 10', 'English', 2026, 'Term 1', 2, 'Reading and Viewing', 'Comprehension and summary skills', 'Close reading of short texts with vocabulary development and summary writing.', 'Formal comprehension with memo and language support notes.', 'CAPS Grade 10 English FAL Term 1'),
        ('FET', 'Grade 10', 'English', 2026, 'Term 1', 3, 'Writing and Presenting', 'Transactional writing', 'Draft and edit emails, letters, and short essays using guided scaffolds.', 'SBA writing task with content, language, and format criteria.', 'CAPS Grade 10 English FAL Term 1'),
        ('FET', 'Grade 10', 'Physical Sciences', 2026, 'Term 1', 1, 'Matter and Materials', 'States, changes, and particle model', 'Use practical demonstrations and structured observation sheets.', 'Informal practical write-up focused on scientific language.', 'CAPS Grade 10 Physical Sciences Term 1'),
        ('FET', 'Grade 10', 'Physical Sciences', 2026, 'Term 1', 2, 'Chemical Systems', 'Atoms, elements, and compounds', 'Introduce models of atoms and classification exercises in pairs.', 'Short quiz on terminology, symbols, and classification.', 'CAPS Grade 10 Physical Sciences Term 1'),
        ('FET', 'Grade 10', 'Physical Sciences', 2026, 'Term 1', 3, 'Mechanics', 'Vectors and motion in one dimension', 'Work through displacement, velocity, and acceleration representations.', 'Formal test item set with calculations and interpretation marks.', 'CAPS Grade 10 Physical Sciences Term 1'),
        ('FET', 'Grade 10', 'Life Sciences', 2026, 'Term 1', 1, 'Chemistry of Life', 'Organic and inorganic compounds', 'Link molecules to living systems through diagrams and note consolidation.', 'Informal worksheet on biological molecules and functions.', 'CAPS Grade 10 Life Sciences Term 1'),
        ('FET', 'Grade 10', 'Life Sciences', 2026, 'Term 1', 2, 'Cell Structure', 'Cell organelles and microscopy', 'Compare plant and animal cells and reinforce organelle functions.', 'Practical-style labelled diagram and explanation task.', 'CAPS Grade 10 Life Sciences Term 1'),
        ('FET', 'Grade 10', 'Life Sciences', 2026, 'Term 1', 3, 'Cell Division', 'Mitosis and significance', 'Sequence mitosis stages with visual organisers and vocabulary support.', 'Formal SBA question set on process, sequence, and importance.', 'CAPS Grade 10 Life Sciences Term 1')
) AS seed(phase, grade, subject_name, academic_year, term, week_number, topic, subtopic, recommended_activities, assessment_guidance, caps_reference)
LEFT JOIN subject_catalogue catalogue
    ON LOWER(catalogue.name) = LOWER(seed.subject_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM atp_topics existing
    WHERE existing.phase = seed.phase
      AND existing.grade = seed.grade
      AND LOWER(existing.subject_name) = LOWER(seed.subject_name)
      AND existing.academic_year = seed.academic_year
      AND existing.term = seed.term
      AND existing.week_number = seed.week_number
      AND existing.topic = seed.topic
);
