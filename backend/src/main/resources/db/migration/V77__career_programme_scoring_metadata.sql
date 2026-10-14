-- Rules must be populated only from a reviewed institutional source. No default scoring rule is assumed.
ALTER TABLE career_program_requirements ADD COLUMN aps_rule VARCHAR(80);
ALTER TABLE career_program_requirements ADD COLUMN qualification_type VARCHAR(120);
