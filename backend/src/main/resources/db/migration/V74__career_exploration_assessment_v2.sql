-- Versioned content: no historical questions, attempts or results are modified.
INSERT INTO psychometric_assessments (code, name, description, version, active, public_available)
VALUES ('EDURITE_CAREER_V2', 'EduRite Career Exploration', 'Interests, strengths and work preferences for career guidance; not a clinical assessment.', 'v2.0', TRUE, FALSE)
ON CONFLICT (code) DO NOTHING;
INSERT INTO psychometric_questions (assessment_id, question_key, prompt, dimension_key, min_score, max_score, display_order, active)
SELECT a.id, v.question_key, v.prompt, v.dimension_key, 1, 5, v.display_order, TRUE
FROM psychometric_assessments a CROSS JOIN (VALUES
('analytical_1', 'I enjoy breaking a complex problem into smaller steps.', 'analytical', 1),
('analytical_2', 'I like looking for patterns in information.', 'analytical', 2),
('analytical_3', 'I compare evidence before deciding which solution to try.', 'analytical', 3),
('creative_1', 'I enjoy developing original designs, stories or ideas.', 'creative', 4),
('creative_2', 'I like trying more than one approach to a task.', 'creative', 5),
('creative_3', 'I am interested in expressing ideas through creative work.', 'creative', 6),
('communication_1', 'I enjoy explaining ideas to other people.', 'communication', 7),
('communication_2', 'I listen carefully before responding to another point of view.', 'communication', 8),
('communication_3', 'I would like work that involves writing or presenting.', 'communication', 9),
('leadership_1', 'I enjoy helping a group organise its next steps.', 'leadership', 10),
('leadership_2', 'I am willing to take responsibility for a shared task.', 'leadership', 11),
('leadership_3', 'I like encouraging others towards a common goal.', 'leadership', 12),
('technical_1', 'I enjoy understanding how equipment or technology works.', 'technical', 13),
('technical_2', 'I like building, repairing or testing practical things.', 'technical', 14),
('technical_3', 'I am interested in learning technical tools or programming.', 'technical', 15),
('helping_1', 'I enjoy helping someone learn a new skill.', 'helping', 16),
('helping_2', 'I am interested in work that supports people or communities.', 'helping', 17),
('helping_3', 'I feel motivated when my work improves another person''s situation.', 'helping', 18),
('business_1', 'I enjoy thinking about how a business could serve customers.', 'business', 19),
('business_2', 'I am interested in budgeting, planning or entrepreneurship.', 'business', 20),
('business_3', 'I like comparing costs and benefits when making decisions.', 'business', 21),
('work_preferences_1', 'I work best with clear plans and milestones.', 'work_preferences', 22),
('work_preferences_2', 'I enjoy collaborating with others on a task.', 'work_preferences', 23),
('work_preferences_3', 'I use feedback to identify skills I want to develop.', 'work_preferences', 24)
) v(question_key, prompt, dimension_key, display_order) WHERE a.code = 'EDURITE_CAREER_V2'
ON CONFLICT (assessment_id, question_key) DO NOTHING;
