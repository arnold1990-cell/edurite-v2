CREATE TABLE IF NOT EXISTS student_cvs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    personal_summary TEXT,
    education TEXT,
    skills TEXT,
    experience TEXT,
    projects TEXT,
    certifications TEXT,
    references_text TEXT,
    career_objective TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_student_cvs_student_id UNIQUE (student_id)
);

CREATE TABLE IF NOT EXISTS scholarship_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    bursary_id UUID,
    scholarship_title VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    application_deadline DATE,
    status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
    checklist TEXT,
    required_documents TEXT,
    reminder_notes TEXT,
    motivation_letter_draft TEXT,
    saved BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_scholarship_applications_student_deadline
    ON scholarship_applications(student_id, application_deadline);

CREATE TABLE IF NOT EXISTS tutor_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    subject VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    last_message_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_tutor_sessions_student_updated
    ON tutor_sessions(student_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS tutor_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES tutor_sessions(id) ON DELETE CASCADE,
    sender VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_tutor_messages_session_created
    ON tutor_messages(session_id, created_at);

CREATE TABLE IF NOT EXISTS university_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    university_name VARCHAR(255) NOT NULL,
    programme_name VARCHAR(255) NOT NULL,
    country VARCHAR(120),
    intake_year INTEGER,
    application_deadline DATE,
    application_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    document_references TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_university_applications_student_deadline
    ON university_applications(student_id, application_deadline);

CREATE TABLE IF NOT EXISTS mentors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(200) NOT NULL,
    profession VARCHAR(180) NOT NULL,
    company VARCHAR(180),
    career_field VARCHAR(180),
    bio TEXT,
    expertise_areas TEXT,
    availability_notes TEXT,
    contact_method VARCHAR(180),
    booking_link VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_mentors_active_field ON mentors(active, career_field);

CREATE TABLE IF NOT EXISTS mentorship_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    mentor_id UUID NOT NULL REFERENCES mentors(id) ON DELETE CASCADE,
    message TEXT,
    goals TEXT,
    preferred_times TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'REQUESTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_mentorship_requests_student ON mentorship_requests(student_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mentorship_requests_mentor ON mentorship_requests(mentor_id, created_at DESC);

CREATE TABLE IF NOT EXISTS internship_opportunities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID,
    title VARCHAR(255) NOT NULL,
    organization_name VARCHAR(255),
    opportunity_type VARCHAR(40) NOT NULL DEFAULT 'INTERNSHIP',
    field VARCHAR(180),
    location VARCHAR(180),
    description TEXT,
    requirements TEXT,
    application_link VARCHAR(500),
    deadline DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_internship_opportunities_active_type
    ON internship_opportunities(active, opportunity_type, deadline);

CREATE TABLE IF NOT EXISTS student_opportunity_interests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    opportunity_id UUID NOT NULL REFERENCES internship_opportunities(id) ON DELETE CASCADE,
    status VARCHAR(40) NOT NULL DEFAULT 'SAVED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_student_opportunity_interest UNIQUE (student_id, opportunity_id)
);

CREATE TABLE IF NOT EXISTS school_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_name VARCHAR(255) NOT NULL,
    country VARCHAR(120),
    city VARCHAR(120),
    contact_person VARCHAR(180),
    contact_email VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS school_students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES school_profiles(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_school_student UNIQUE (school_id, student_id)
);

CREATE TABLE IF NOT EXISTS career_roadmaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(160) NOT NULL UNIQUE,
    title VARCHAR(180) NOT NULL,
    overview TEXT,
    required_subjects TEXT,
    recommended_skills TEXT,
    study_path TEXT,
    entry_level_jobs TEXT,
    long_term_growth TEXT,
    learning_resources TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_career_roadmaps_active_title ON career_roadmaps(active, title);

INSERT INTO career_roadmaps (
    slug, title, overview, required_subjects, recommended_skills, study_path,
    entry_level_jobs, long_term_growth, learning_resources
) VALUES
('software-engineer', 'Software Engineer', 'Builds software systems, apps, and platforms.', 'Mathematics, Computer Studies, English', 'Programming, problem solving, databases, teamwork', 'NSC with Mathematics, diploma or degree in computer science/software engineering, portfolio projects', 'Junior developer, QA tester, support engineer', 'Senior engineer, architect, engineering manager, founder', 'freeCodeCamp, CS50, Java and JavaScript projects'),
('doctor', 'Doctor', 'Diagnoses and treats patients in clinical settings.', 'Mathematics, Physical Sciences, Life Sciences, English', 'Scientific reasoning, empathy, communication, resilience', 'NSC with strong sciences, MBChB, internship, community service, specialist training where applicable', 'Medical intern, community service doctor', 'General practitioner, specialist, researcher, hospital leader', 'University admission pages, health sciences faculty guides'),
('lawyer', 'Lawyer', 'Advises clients and represents legal matters.', 'English, History, Business Studies', 'Reading, writing, argumentation, ethics, research', 'LLB degree, practical legal training, articles or vocational training, admission exams', 'Candidate attorney, legal researcher, compliance assistant', 'Attorney, advocate, partner, judge, legal executive', 'Legal Aid resources, university law clinics'),
('accountant', 'Accountant', 'Manages financial records, reporting, and compliance.', 'Mathematics or Accounting, English, Business Studies', 'Numeracy, attention to detail, spreadsheets, ethics', 'Accounting diploma or degree, SAICA/SAIPA/CIMA pathway depending on target role', 'Bookkeeper, accounts clerk, audit trainee', 'Professional accountant, auditor, finance manager, CFO', 'Accounting practice sets, spreadsheet courses'),
('nurse', 'Nurse', 'Provides patient care and health support.', 'Life Sciences, English, Mathematics or Mathematical Literacy', 'Care, communication, clinical judgement, stamina', 'Nursing diploma or degree, clinical placement, professional registration', 'Student nurse, enrolled nurse, community health worker', 'Professional nurse, nurse educator, specialist nurse, unit manager', 'Nursing college admission guides, health department resources'),
('teacher', 'Teacher', 'Supports learners through classroom teaching and assessment.', 'English plus subject-specific strengths', 'Communication, planning, patience, subject knowledge', 'Bachelor of Education or postgraduate teaching certificate, teaching practice, registration', 'Student teacher, teaching assistant, tutor', 'Senior teacher, head of department, principal, curriculum specialist', 'DBE resources, subject tutoring materials'),
('electrician', 'Electrician', 'Installs and maintains electrical systems safely.', 'Mathematics, Physical Sciences, Electrical Technology', 'Safety, practical problem solving, wiring, diagnostics', 'TVET electrical programme, apprenticeship, trade test', 'Apprentice electrician, maintenance assistant', 'Qualified artisan, contractor, site supervisor, business owner', 'TVET college guides, trade test preparation'),
('entrepreneur', 'Entrepreneur', 'Creates and grows businesses around customer needs.', 'Business Studies, Accounting, English, Mathematics', 'Sales, finance, product thinking, resilience, networking', 'Short courses, incubation, market testing, business plan, practical trading experience', 'Founder, business development assistant, sales associate', 'Business owner, investor, franchise operator, social entrepreneur', 'Business model canvas, local incubators, entrepreneurship courses')
ON CONFLICT (slug) DO NOTHING;

CREATE TABLE IF NOT EXISTS international_opportunities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    opportunity_type VARCHAR(80) NOT NULL,
    country VARCHAR(120),
    deadline DATE,
    eligibility_summary TEXT,
    application_link VARCHAR(500),
    visa_guidance_notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_international_opportunities_active_country
    ON international_opportunities(active, country, opportunity_type);

CREATE TABLE IF NOT EXISTS saved_international_opportunities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    opportunity_id UUID NOT NULL REFERENCES international_opportunities(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_saved_international_opportunity UNIQUE (student_id, opportunity_id)
);
