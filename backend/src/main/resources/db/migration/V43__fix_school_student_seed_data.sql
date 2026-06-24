-- noinspection SqlNoDataSourceInspection,SqlResolve
-- Additive demo learner seed updates. Do NOT modify old migrations.
-- Includes English + Computer Studies subject workflows and learner-facing data.

INSERT INTO school_subjects (id, school_id, subject_name, phase, grade, active, created_at, updated_at)
SELECT gen_random_uuid(), s.id, x.subject_name, 'FET', 'Grade 10', true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
CROSS JOIN (VALUES
    ('English'),
    ('Computer Studies')
) AS x(subject_name)
WHERE NOT EXISTS (
    SELECT 1 FROM school_subjects ss
    WHERE ss.school_id = s.id
      AND ss.subject_name = x.subject_name
      AND COALESCE(ss.grade, '') = 'Grade 10'
);

INSERT INTO teacher_assignments (id, school_id, teacher_user_id, class_id, subject_id, active, created_at, updated_at)
SELECT gen_random_uuid(), s.id, t.id, c.id, ss.id, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id AND ss.subject_name IN ('Mathematics', 'English', 'Physical Science', 'Computer Studies', 'Physical Sciences')
WHERE NOT EXISTS (
    SELECT 1 FROM teacher_assignments ta
    WHERE ta.school_id = s.id
      AND ta.teacher_user_id = t.id
      AND ta.class_id = c.id
      AND ta.subject_id = ss.id
);

INSERT INTO learner_enrollments (id, school_id, learner_user_id, class_id, subject_id, active, created_at, updated_at)
SELECT gen_random_uuid(), s.id, l.id, c.id, ss.id, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users l ON lower(l.email) IN ('arnold.student@edurite.com', 'schoolstudent@edurite.com')
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id AND ss.subject_name IN ('Mathematics', 'English', 'Physical Science', 'Computer Studies', 'Physical Sciences')
WHERE NOT EXISTS (
    SELECT 1 FROM learner_enrollments le
    WHERE le.school_id = s.id
      AND le.learner_user_id = l.id
      AND le.class_id = c.id
      AND le.subject_id = ss.id
);

INSERT INTO learning_notes (id, school_id, class_id, subject_id, teacher_user_id, title, note_text, pdf_url, published, created_at, updated_at)
SELECT gen_random_uuid(), s.id, c.id, ss.id, t.id, x.title, x.note_text, x.pdf_url, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id
JOIN (VALUES
    ('Mathematics', 'Mathematics Algebra Notes PDF', 'Algebra revision notes and worked examples.', 'https://example.com/notes/math-algebra-grade10.pdf'),
    ('English', 'English Essay Craft Notes', 'Planning, drafting, and refining essay arguments.', 'https://example.com/notes/english-essay-craft.pdf'),
    ('Physical Sciences', 'Physical Science Practical Memo', 'Guided practical methodology and lab memo.', 'https://example.com/notes/physical-science-practical-memo.pdf'),
    ('Computer Studies', 'Computer Studies Algorithms Pack', 'Flowcharts, pseudocode, and debugging tips.', 'https://example.com/notes/computer-studies-algorithms-pack.pdf')
) AS x(subject_name, title, note_text, pdf_url)
  ON x.subject_name = ss.subject_name
WHERE NOT EXISTS (
    SELECT 1 FROM learning_notes n
    WHERE n.school_id = s.id
      AND n.class_id = c.id
      AND n.subject_id = ss.id
      AND n.title = x.title
);

INSERT INTO school_tasks (id, school_id, class_id, subject_id, teacher_user_id, task_type, title, instructions, due_at, term, max_marks, rubric, released, created_at, updated_at)
SELECT gen_random_uuid(), s.id, c.id, ss.id, t.id, x.task_type, x.title, x.instructions, x.due_at, 'Term 2', x.max_marks, x.rubric, true, now(), now()
FROM (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1) s
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
JOIN school_classes c ON c.school_id = s.id AND c.grade = 'Grade 10' AND c.class_name = 'A'
JOIN school_subjects ss ON ss.school_id = s.id
JOIN (VALUES
    ('Mathematics', 'SBA', 'Mathematics SBA Investigation', 'Investigate sequences and submit a full report.', now() + interval '6 days', 50::numeric, 'Accuracy, reasoning, presentation'),
    ('Physical Sciences', 'PRACTICAL', 'Physical Sciences Practical Report', 'Complete practical report with calculations.', now() + interval '4 days', 40::numeric, 'Method, accuracy, interpretation'),
    ('English', 'ASSESSMENT', 'English Essay Task', 'Write a structured argumentative essay.', now() + interval '3 days', 50::numeric, 'Structure, grammar, evidence'),
    ('Computer Studies', 'QUIZ', 'Computer Studies Logic Quiz', 'Algorithm and logic focused timed quiz.', now() + interval '8 days', 35::numeric, 'Logic, efficiency, correctness')
) AS x(subject_name, task_type, title, instructions, due_at, max_marks, rubric)
  ON x.subject_name = ss.subject_name
WHERE NOT EXISTS (
    SELECT 1 FROM school_tasks st
    WHERE st.school_id = s.id
      AND st.class_id = c.id
      AND st.subject_id = ss.id
      AND st.title = x.title
);

INSERT INTO task_submissions (id, task_id, learner_user_id, submission_text, file_url, submitted_at, late, status, created_at, updated_at)
SELECT gen_random_uuid(), st.id, l.id, 'Completed practical report draft with diagrams.', 'https://example.com/submissions/arnold-physical-sciences-practical.pdf', now(), false, 'SUBMITTED', now(), now()
FROM school_tasks st
JOIN users l ON lower(l.email) = 'arnold.student@edurite.com'
WHERE st.title = 'Physical Sciences Practical Report'
AND NOT EXISTS (
    SELECT 1 FROM task_submissions ts
    WHERE ts.task_id = st.id
      AND ts.learner_user_id = l.id
);

INSERT INTO task_submissions (id, task_id, learner_user_id, submission_text, file_url, submitted_at, late, status, created_at, updated_at)
SELECT gen_random_uuid(), st.id, l.id, 'Essay submission including evidence references.', 'https://example.com/submissions/arnold-english-essay.pdf', now(), false, 'MARKED', now(), now()
FROM school_tasks st
JOIN users l ON lower(l.email) = 'arnold.student@edurite.com'
WHERE st.title = 'English Essay Task'
AND NOT EXISTS (
    SELECT 1 FROM task_submissions ts
    WHERE ts.task_id = st.id
      AND ts.learner_user_id = l.id
);

INSERT INTO submission_feedback (id, submission_id, teacher_user_id, marks_awarded, comments, rubric_scoring, released, created_at, updated_at)
SELECT gen_random_uuid(), ts.id, t.id, 35::numeric, 'Good structure. Improve supporting evidence.', 'Structure 8/10, Evidence 6/10, Language 7/10', true, now(), now()
FROM task_submissions ts
JOIN school_tasks st ON st.id = ts.task_id
JOIN users t ON lower(t.email) = 'teacher@edurite.com'
WHERE st.title = 'English Essay Task'
AND NOT EXISTS (
    SELECT 1 FROM submission_feedback sf
    WHERE sf.submission_id = ts.id
);
