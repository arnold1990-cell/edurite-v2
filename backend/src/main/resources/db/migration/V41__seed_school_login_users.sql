-- noinspection SqlNoDataSourceInspection,SqlResolve
INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_SCHOOL_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_SCHOOL_ADMIN');

INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_TEACHER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_TEACHER');

INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_SCHOOL_STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_SCHOOL_STUDENT');

INSERT INTO schools (id, school_name, province, district, contact_email, created_at, updated_at)
SELECT gen_random_uuid(), 'EduRite Secondary School', 'Gauteng', 'Johannesburg', 'school@edurite.com', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM schools);

INSERT INTO users (id, email, password_hash, first_name, last_name, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'schooladmin@edurite.com',
    crypt('SchoolAdmin@123', gen_salt('bf', 10)),
    'School',
    'Admin',
    'ACTIVE',
    now(),
    now()
)
ON CONFLICT (email) DO UPDATE
SET
    password_hash = CASE
        WHEN users.password_hash IS NULL
            OR users.password_hash = ''
            OR users.password_hash !~ '^\$2'
            OR crypt('SchoolAdmin@123', users.password_hash) <> users.password_hash
        THEN crypt('SchoolAdmin@123', gen_salt('bf', 10))
        ELSE users.password_hash
    END,
    first_name = COALESCE(NULLIF(users.first_name, ''), 'School'),
    last_name = COALESCE(NULLIF(users.last_name, ''), 'Admin'),
    status = 'ACTIVE',
    updated_at = now();

INSERT INTO users (id, email, password_hash, first_name, last_name, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'teacher@edurite.com',
    crypt('Teacher@123', gen_salt('bf', 10)),
    'EduRite',
    'Teacher',
    'ACTIVE',
    now(),
    now()
)
ON CONFLICT (email) DO UPDATE
SET
    password_hash = CASE
        WHEN users.password_hash IS NULL
            OR users.password_hash = ''
            OR users.password_hash !~ '^\$2'
            OR crypt('Teacher@123', users.password_hash) <> users.password_hash
        THEN crypt('Teacher@123', gen_salt('bf', 10))
        ELSE users.password_hash
    END,
    first_name = COALESCE(NULLIF(users.first_name, ''), 'EduRite'),
    last_name = COALESCE(NULLIF(users.last_name, ''), 'Teacher'),
    status = 'ACTIVE',
    updated_at = now();

INSERT INTO users (id, email, password_hash, first_name, last_name, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'schoolstudent@edurite.com',
    crypt('SchoolStudent@123', gen_salt('bf', 10)),
    'School',
    'Learner',
    'ACTIVE',
    now(),
    now()
)
ON CONFLICT (email) DO UPDATE
SET
    password_hash = CASE
        WHEN users.password_hash IS NULL
            OR users.password_hash = ''
            OR users.password_hash !~ '^\$2'
            OR crypt('SchoolStudent@123', users.password_hash) <> users.password_hash
        THEN crypt('SchoolStudent@123', gen_salt('bf', 10))
        ELSE users.password_hash
    END,
    first_name = COALESCE(NULLIF(users.first_name, ''), 'School'),
    last_name = COALESCE(NULLIF(users.last_name, ''), 'Learner'),
    status = 'ACTIVE',
    updated_at = now();

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_SCHOOL_ADMIN'
WHERE u.email = 'schooladmin@edurite.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_TEACHER'
WHERE u.email = 'teacher@edurite.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ROLE_SCHOOL_STUDENT'
WHERE u.email = 'schoolstudent@edurite.com'
ON CONFLICT DO NOTHING;

UPDATE users
SET
    email_verified = true,
    deleted_at = NULL,
    status = 'ACTIVE',
    updated_at = now()
WHERE email IN ('schooladmin@edurite.com', 'teacher@edurite.com', 'schoolstudent@edurite.com');

INSERT INTO school_user_profiles (
    id,
    school_id,
    user_id,
    role_name,
    active,
    deleted,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM schools ORDER BY created_at ASC LIMIT 1),
    u.id,
    role_name,
    true,
    false,
    now(),
    now()
FROM (
    VALUES
        ('schooladmin@edurite.com', 'ROLE_SCHOOL_ADMIN'),
        ('teacher@edurite.com', 'ROLE_TEACHER'),
        ('schoolstudent@edurite.com', 'ROLE_SCHOOL_STUDENT')
) AS seed(email, role_name)
JOIN users u ON u.email = seed.email
WHERE NOT EXISTS (
    SELECT 1
    FROM school_user_profiles sup
    WHERE sup.user_id = u.id
      AND sup.deleted = false
);

UPDATE school_user_profiles sup
SET
    active = true,
    deleted = false,
    updated_at = now()
FROM users u
WHERE sup.user_id = u.id
  AND u.email IN ('schooladmin@edurite.com', 'teacher@edurite.com', 'schoolstudent@edurite.com');
