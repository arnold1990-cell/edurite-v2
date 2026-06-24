INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_STUDENT');

INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_COMPANY'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_COMPANY');

INSERT INTO roles (id, name)
SELECT gen_random_uuid(), 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');
