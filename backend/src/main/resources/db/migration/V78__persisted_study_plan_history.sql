ALTER TABLE saved_career_roadmaps ADD COLUMN request_key UUID;
ALTER TABLE saved_career_roadmaps ADD COLUMN study_plan BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE saved_career_roadmaps ADD COLUMN completed_steps_json TEXT NOT NULL DEFAULT '[]';
CREATE UNIQUE INDEX saved_roadmap_request_key ON saved_career_roadmaps(learner_id, request_key) WHERE request_key IS NOT NULL;

-- Unknown academic information must remain unknown, never a fabricated zero.
ALTER TABLE saved_career_roadmaps ALTER COLUMN learner_aps DROP NOT NULL;
ALTER TABLE saved_career_roadmaps ALTER COLUMN learner_aps DROP DEFAULT;
