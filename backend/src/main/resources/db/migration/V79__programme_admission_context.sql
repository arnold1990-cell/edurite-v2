-- Nullable metadata only: no unverified requirements or admission years are inferred.
ALTER TABLE career_program_requirements ADD COLUMN admission_year INTEGER;
ALTER TABLE career_program_requirements ADD COLUMN general_admission_requirements TEXT;
ALTER TABLE career_program_requirements ADD COLUMN programme_admission_requirements TEXT;
ALTER TABLE career_program_requirements ADD COLUMN additional_selection_requirements TEXT;
ALTER TABLE career_program_requirements ADD CONSTRAINT programme_admission_year_valid
    CHECK (admission_year IS NULL OR admission_year BETWEEN 2000 AND 2200);
