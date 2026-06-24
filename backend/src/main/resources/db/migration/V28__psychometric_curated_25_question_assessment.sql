UPDATE psychometric_assessments
SET
    name = 'EduRite Core Readiness Assessment',
    description = 'Curated 25-question assessment covering interests, strengths, work style, aptitude, and growth indicators.',
    version = 'v1.1',
    updated_at = now()
WHERE code = 'EDURITE_CORE_V1';

WITH assessment AS (
    SELECT id
    FROM psychometric_assessments
    WHERE code = 'EDURITE_CORE_V1'
    LIMIT 1
)
INSERT INTO psychometric_questions (
    assessment_id,
    question_key,
    prompt,
    dimension_key,
    min_score,
    max_score,
    display_order,
    active
)
SELECT
    assessment.id,
    payload.question_key,
    payload.prompt,
    payload.dimension_key,
    1,
    5,
    payload.display_order,
    TRUE
FROM assessment
JOIN (
    VALUES
        ('interest_01', 'I am motivated by solving practical real-world problems.', 'interests', 1),
        ('interest_02', 'I enjoy understanding how technology and systems work.', 'interests', 2),
        ('interest_03', 'I like exploring business, markets, or entrepreneurship topics.', 'interests', 3),
        ('interest_04', 'I am interested in work that helps people directly.', 'interests', 4),
        ('interest_05', 'I enjoy creative tasks such as design, storytelling, or innovation.', 'interests', 5),

        ('strength_01', 'I can stay focused and finish important tasks on time.', 'strengths', 6),
        ('strength_02', 'I explain ideas clearly when working with others.', 'strengths', 7),
        ('strength_03', 'I adapt quickly when plans or requirements change.', 'strengths', 8),
        ('strength_04', 'I am confident making decisions with incomplete information.', 'strengths', 9),
        ('strength_05', 'I take initiative without needing constant supervision.', 'strengths', 10),

        ('work_style_01', 'I enjoy collaborating in teams to achieve shared goals.', 'work_style', 11),
        ('work_style_02', 'I am comfortable taking leadership in group activities.', 'work_style', 12),
        ('work_style_03', 'I prefer structured plans and clear milestones when learning.', 'work_style', 13),
        ('work_style_04', 'I can remain calm and productive under pressure.', 'work_style', 14),
        ('work_style_05', 'I regularly reflect on feedback and adjust my approach.', 'work_style', 15),

        ('aptitude_01', 'I can break complex problems into smaller logical steps.', 'aptitude', 16),
        ('aptitude_02', 'I quickly identify patterns in information or data.', 'aptitude', 17),
        ('aptitude_03', 'I am comfortable with quantitative or numerical reasoning.', 'aptitude', 18),
        ('aptitude_04', 'I can evaluate options and choose a reasonable solution.', 'aptitude', 19),
        ('aptitude_05', 'I learn new tools or concepts with limited guidance.', 'aptitude', 20),

        ('growth_01', 'I stay motivated even when a task becomes difficult.', 'growth_mindset', 21),
        ('growth_02', 'I actively improve weak areas through practice.', 'growth_mindset', 22),
        ('growth_03', 'I seek constructive feedback to improve performance.', 'growth_mindset', 23),
        ('growth_04', 'I keep long-term goals in mind when making decisions.', 'growth_mindset', 24),
        ('growth_05', 'I consistently track progress toward my personal goals.', 'growth_mindset', 25)
) AS payload(question_key, prompt, dimension_key, display_order) ON TRUE
ON CONFLICT (assessment_id, question_key)
DO UPDATE SET
    prompt = EXCLUDED.prompt,
    dimension_key = EXCLUDED.dimension_key,
    min_score = EXCLUDED.min_score,
    max_score = EXCLUDED.max_score,
    display_order = EXCLUDED.display_order,
    active = TRUE,
    updated_at = now();

WITH assessment AS (
    SELECT id
    FROM psychometric_assessments
    WHERE code = 'EDURITE_CORE_V1'
    LIMIT 1
)
UPDATE psychometric_questions q
SET
    active = FALSE,
    updated_at = now()
FROM assessment
WHERE q.assessment_id = assessment.id
  AND q.question_key NOT IN (
      'interest_01', 'interest_02', 'interest_03', 'interest_04', 'interest_05',
      'strength_01', 'strength_02', 'strength_03', 'strength_04', 'strength_05',
      'work_style_01', 'work_style_02', 'work_style_03', 'work_style_04', 'work_style_05',
      'aptitude_01', 'aptitude_02', 'aptitude_03', 'aptitude_04', 'aptitude_05',
      'growth_01', 'growth_02', 'growth_03', 'growth_04', 'growth_05'
  );
