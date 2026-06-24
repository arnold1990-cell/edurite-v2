-- noinspection SqlNoDataSourceInspection,SqlResolve
-- Seed a full learner demo dataset coordinated with teacher workflows.
WITH school_ref AS (
    SELECT id FROM schools ORDER BY created_at ASC LIMIT 1
),
teacher_ref AS (
    SELECT id FROM users WHERE lower(email) = 'teacher@edurite.com' LIMIT 1
),
learner_ref AS (
    SELECT id FROM users WHERE lower(email) = 'arnold.student@edurite.com' LIMIT 1
)
INSERT INTO users (id, email, password_hash, first_name, last_name, status, created_at, updated_at)
SELECT gen_random_uuid(), 'arnold.student@edurite.com', crypt('Student@123', gen_salt('bf', 10)), 'Arnold Tyvern', 'Madamombe', 'ACTIVE', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM learner_ref);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_SCHOOL_STUDENT'
WHERE lower(u.email) = 'arnold.student@edurite.com'
ON CONFLICT DO NOTHING;

UPDATE users
SET email_verified = true,
    deleted_at = NULL,
    status = 'ACTIVE',
    updated_at = now()
WHERE lower(email) = 'arnold.student@edurite.com';

INSERT INTO school_user_profiles (id, school_id, user_id, role_name, active, deleted, created_at, updated_at)
SELECT gen_random_uuid(), s.id, u.id, 'ROLE_SCHOOL_STUDENT', true, false, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users u ON lower(u.email) = 'arnold.student@edurite.com'
WHERE NOT EXISTS (
    SELECT 1 FROM school_user_profiles sup WHERE sup.school_id = s.id AND sup.user_id = u.id
);

INSERT INTO school_classes (id, school_id, grade, class_name, academic_year, term, active, created_at, updated_at)
SELECT gen_random_uuid(), s.id, 'Grade 10', 'A', EXTRACT(YEAR FROM now())::int, 'Term 2', true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
WHERE NOT EXISTS (
    SELECT 1 FROM school_classes c
    WHERE c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A' AND c.academic_year = EXTRACT(YEAR FROM now())::int
);

INSERT INTO school_subjects (id, school_id, subject_name, phase, grade, active, created_at, updated_at)
SELECT gen_random_uuid(), s.id, x.subject_name, 'FET', 'Grade 10', true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
CROSS JOIN (VALUES
    ('Mathematics'),
    ('Physical Sciences'),
    ('English First Additional Language'),
    ('Life Sciences')
) AS x(subject_name)
WHERE NOT EXISTS (
    SELECT 1 FROM school_subjects ss WHERE ss.school_id = s.id AND ss.subject_name = x.subject_name AND COALESCE(ss.grade, '') = 'Grade 10'
);

INSERT INTO teacher_assignments (id, school_id, teacher_user_id, class_id, subject_id, active, created_at, updated_at)
SELECT gen_random_uuid(), s.id, t.id, c.id, ss.id, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id AND ss.subject_name IN ('Mathematics', 'Physical Sciences', 'English First Additional Language', 'Life Sciences')
WHERE NOT EXISTS (
    SELECT 1 FROM teacher_assignments ta
    WHERE ta.school_id = s.id AND ta.teacher_user_id = t.id AND ta.class_id = c.id AND ta.subject_id = ss.id
);

INSERT INTO learner_enrollments (id, school_id, learner_user_id, class_id, subject_id, active, created_at, updated_at)
SELECT gen_random_uuid(), s.id, l.id, c.id, ss.id, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users l ON lower(l.email) IN ('arnold.student@edurite.com', 'schoolstudent@edurite.com')
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id AND ss.subject_name IN ('Mathematics', 'Physical Sciences', 'English First Additional Language', 'Life Sciences')
WHERE NOT EXISTS (
    SELECT 1 FROM learner_enrollments le
    WHERE le.school_id = s.id AND le.learner_user_id = l.id AND le.class_id = c.id AND le.subject_id = ss.id
);

INSERT INTO learning_notes (id, school_id, class_id, subject_id, teacher_user_id, title, note_text, pdf_url, published, created_at, updated_at)
SELECT gen_random_uuid(), s.id, c.id, ss.id, t.id, x.title, x.note_text, x.pdf_url, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id
JOIN (VALUES
    ('Mathematics', 'Mathematics Algebra Notes PDF', 'Algebra revision notes for Grade 10.', 'https://example.com/notes/mathematics-algebra-notes.pdf'),
    ('Physical Sciences', 'Physical Sciences Forces Summary', 'Summary of Newton''s laws and force diagrams.', 'https://example.com/notes/physical-sciences-forces-summary.pdf'),
    ('English First Additional Language', 'English Essay Writing Guide', 'Essay structure, thesis development, and evidence support.', 'https://example.com/notes/english-essay-writing-guide.pdf')
) AS x(subject_name, title, note_text, pdf_url)
  ON x.subject_name = ss.subject_name
WHERE NOT EXISTS (
    SELECT 1 FROM learning_notes n
    WHERE n.school_id = s.id AND n.class_id = c.id AND n.subject_id = ss.id AND n.title = x.title
);

INSERT INTO school_tasks (id, school_id, class_id, subject_id, teacher_user_id, task_type, title, instructions, due_at, term, max_marks, rubric, released, created_at, updated_at)
SELECT gen_random_uuid(), s.id, c.id, ss.id, t.id, x.task_type, x.title, x.instructions, x.due_at, 'Term 2', x.max_marks, x.rubric, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id
JOIN (VALUES
    ('Mathematics', 'SBA', 'Mathematics SBA Investigation', 'Investigate patterns and submit a written report.', now() + interval '7 days', 50::numeric, 'Accuracy, method, and explanation'),
    ('Physical Sciences', 'PRACTICAL', 'Physical Sciences Practical Report', 'Submit practical findings and analysis.', now() + interval '5 days', 40::numeric, 'Method, observations, and conclusion'),
    ('English First Additional Language', 'ASSESSMENT', 'English Essay Task', 'Write an argumentative essay with supporting evidence.', now() + interval '3 days', 50::numeric, 'Structure, language, evidence')
) AS x(subject_name, task_type, title, instructions, due_at, max_marks, rubric)
  ON x.subject_name = ss.subject_name
WHERE NOT EXISTS (
    SELECT 1 FROM school_tasks st
    WHERE st.school_id = s.id AND st.class_id = c.id AND st.subject_id = ss.id AND st.title = x.title
);

INSERT INTO school_tasks (id, school_id, class_id, subject_id, teacher_user_id, task_type, title, instructions, due_at, term, max_marks, rubric, released, created_at, updated_at)
SELECT gen_random_uuid(), s.id, c.id, ss.id, t.id, x.task_type, x.title, x.instructions, x.due_at, 'Term 2', x.max_marks, x.rubric, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id
JOIN (VALUES
    ('Mathematics', 'TEST', 'Mathematics Term 2 Test', 'Prepare for the Term 2 test.', now() + interval '12 days', 100::numeric, 'Term 2 test rubric'),
    ('Physical Sciences', 'QUIZ', 'Physical Sciences Quiz', 'Short quiz on mechanics and energy.', now() + interval '9 days', 30::numeric, 'Quiz rubric'),
    ('English First Additional Language', 'ASSESSMENT', 'English Reading Assessment', 'Reading comprehension assessment.', now() + interval '11 days', 60::numeric, 'Reading rubric')
) AS x(subject_name, task_type, title, instructions, due_at, max_marks, rubric)
  ON x.subject_name = ss.subject_name
WHERE NOT EXISTS (
    SELECT 1 FROM school_tasks st
    WHERE st.school_id = s.id AND st.class_id = c.id AND st.subject_id = ss.id AND st.title = x.title
);

INSERT INTO task_submissions (id, task_id, learner_user_id, submission_text, file_url, submitted_at, late, status, created_at, updated_at)
SELECT gen_random_uuid(), st.id, l.id, 'Practical analysis with force diagrams.', 'https://example.com/submissions/physical-sciences-practical-report.pdf', now(), false, 'SUBMITTED', now(), now()
FROM school_tasks st
JOIN users l ON lower(l.email) = 'arnold.student@edurite.com'
WHERE st.title = 'Physical Sciences Practical Report'
AND NOT EXISTS (
    SELECT 1 FROM task_submissions ts WHERE ts.task_id = st.id AND ts.learner_user_id = l.id
);

INSERT INTO task_submissions (id, task_id, learner_user_id, submission_text, file_url, submitted_at, late, status, created_at, updated_at)
SELECT gen_random_uuid(), st.id, l.id, 'Argumentative essay draft and references.', 'https://example.com/submissions/english-essay-task.pdf', now(), false, 'MARKED', now(), now()
FROM school_tasks st
JOIN users l ON lower(l.email) = 'arnold.student@edurite.com'
WHERE st.title = 'English Essay Task'
AND NOT EXISTS (
    SELECT 1 FROM task_submissions ts WHERE ts.task_id = st.id AND ts.learner_user_id = l.id
);

INSERT INTO submission_feedback (id, submission_id, teacher_user_id, marks_awarded, comments, rubric_scoring, released, created_at, updated_at)
SELECT gen_random_uuid(), ts.id, t.id, 35::numeric, 'Good structure. Improve supporting evidence.', 'Structure: 8/10, Evidence: 6/10, Language: 7/10', true, now(), now()
FROM task_submissions ts
JOIN school_tasks st ON st.id = ts.task_id
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
WHERE st.title = 'English Essay Task'
AND NOT EXISTS (
    SELECT 1 FROM submission_feedback sf WHERE sf.submission_id = ts.id
);
