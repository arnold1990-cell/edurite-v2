CREATE TABLE IF NOT EXISTS provinces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE districts
ADD COLUMN IF NOT EXISTS province_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_districts_province'
          AND table_name = 'districts'
    ) THEN
        ALTER TABLE districts
        ADD CONSTRAINT fk_districts_province
        FOREIGN KEY (province_id) REFERENCES provinces(id);
    END IF;
END $$;

ALTER TABLE school_registration_requests
ADD COLUMN IF NOT EXISTS province_id UUID NULL,
ADD COLUMN IF NOT EXISTS circuit_id UUID NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_school_registration_requests_province'
          AND table_name = 'school_registration_requests'
    ) THEN
        ALTER TABLE school_registration_requests
        ADD CONSTRAINT fk_school_registration_requests_province
        FOREIGN KEY (province_id) REFERENCES provinces(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_school_registration_requests_circuit'
          AND table_name = 'school_registration_requests'
    ) THEN
        ALTER TABLE school_registration_requests
        ADD CONSTRAINT fk_school_registration_requests_circuit
        FOREIGN KEY (circuit_id) REFERENCES circuits(id);
    END IF;
END $$;

INSERT INTO provinces (name, code)
VALUES
    ('Eastern Cape', 'EC'),
    ('Free State', 'FS'),
    ('Gauteng', 'GP'),
    ('KwaZulu-Natal', 'KZN'),
    ('Limpopo', 'LP'),
    ('Mpumalanga', 'MP'),
    ('Northern Cape', 'NC'),
    ('North West', 'NW'),
    ('Western Cape', 'WC')
ON CONFLICT (name) DO UPDATE
SET code = EXCLUDED.code,
    active = TRUE,
    updated_at = NOW();

UPDATE districts d
SET province_id = p.id
FROM provinces p
WHERE d.province_id IS NULL
  AND LOWER(TRIM(COALESCE(d.province, ''))) = LOWER(p.name);

INSERT INTO districts (district_name, district_code, province_id, province, licensing_status, active, created_at, updated_at)
SELECT seed.district_name, seed.district_code, p.id, p.name, 'ACTIVE', TRUE, NOW(), NOW()
FROM provinces p
JOIN (
    VALUES
        ('Eastern Cape', 'Alfred Nzo', 'DC44'),
        ('Eastern Cape', 'Amathole', 'DC12'),
        ('Eastern Cape', 'Buffalo City', 'BUF'),
        ('Eastern Cape', 'Chris Hani', 'DC13'),
        ('Eastern Cape', 'Joe Gqabi', 'DC14'),
        ('Eastern Cape', 'Nelson Mandela Bay', 'NMA'),
        ('Eastern Cape', 'OR Tambo', 'DC15'),
        ('Eastern Cape', 'Sarah Baartman', 'DC10'),
        ('Free State', 'Fezile Dabi', 'DC20'),
        ('Free State', 'Lejweleputswa', 'DC18'),
        ('Free State', 'Mangaung', 'MAN'),
        ('Free State', 'Thabo Mofutsanyana', 'DC19'),
        ('Free State', 'Xhariep', 'DC16'),
        ('Gauteng', 'City of Johannesburg', 'JHB'),
        ('Gauteng', 'City of Tshwane', 'TSH'),
        ('Gauteng', 'Ekurhuleni', 'EKU'),
        ('Gauteng', 'Sedibeng', 'DC42'),
        ('Gauteng', 'West Rand', 'DC48'),
        ('KwaZulu-Natal', 'Amajuba', 'DC25'),
        ('KwaZulu-Natal', 'eThekwini', 'ETH'),
        ('KwaZulu-Natal', 'Harry Gwala', 'DC43'),
        ('KwaZulu-Natal', 'iLembe', 'DC29'),
        ('KwaZulu-Natal', 'King Cetshwayo', 'DC28'),
        ('KwaZulu-Natal', 'Ugu', 'DC21'),
        ('KwaZulu-Natal', 'uMgungundlovu', 'DC22'),
        ('KwaZulu-Natal', 'uMkhanyakude', 'DC27'),
        ('KwaZulu-Natal', 'uMzinyathi', 'DC24'),
        ('KwaZulu-Natal', 'uThukela', 'DC23'),
        ('KwaZulu-Natal', 'Zululand', 'DC26'),
        ('Limpopo', 'Capricorn', 'DC35'),
        ('Limpopo', 'Mopani', 'DC33'),
        ('Limpopo', 'Sekhukhune', 'DC47'),
        ('Limpopo', 'Vhembe', 'DC34'),
        ('Limpopo', 'Waterberg', 'DC36'),
        ('Mpumalanga', 'Ehlanzeni', 'DC32'),
        ('Mpumalanga', 'Gert Sibande', 'DC30'),
        ('Mpumalanga', 'Nkangala', 'DC31'),
        ('Northern Cape', 'Frances Baard', 'DC9'),
        ('Northern Cape', 'John Taolo Gaetsewe', 'DC45'),
        ('Northern Cape', 'Namakwa', 'DC6'),
        ('Northern Cape', 'Pixley ka Seme', 'DC7'),
        ('Northern Cape', 'ZF Mgcawu', 'DC8'),
        ('North West', 'Bojanala Platinum', 'DC37'),
        ('North West', 'Dr Kenneth Kaunda', 'DC40'),
        ('North West', 'Dr Ruth Segomotsi Mompati', 'DC39'),
        ('North West', 'Ngaka Modiri Molema', 'DC38'),
        ('Western Cape', 'Cape Winelands', 'DC2'),
        ('Western Cape', 'Central Karoo', 'DC5'),
        ('Western Cape', 'City of Cape Town', 'CPT'),
        ('Western Cape', 'Garden Route', 'DC4'),
        ('Western Cape', 'Overberg', 'DC3'),
        ('Western Cape', 'West Coast', 'DC1')
) AS seed(province_name, district_name, district_code)
    ON p.name = seed.province_name
WHERE NOT EXISTS (
    SELECT 1
    FROM districts existing
    WHERE LOWER(existing.district_name) = LOWER(seed.district_name)
);

UPDATE districts d
SET province_id = p.id,
    province = COALESCE(NULLIF(d.province, ''), p.name)
FROM provinces p
WHERE d.province_id IS NULL
  AND LOWER(TRIM(COALESCE(d.province, ''))) = LOWER(p.name);

INSERT INTO circuits (name, code, district_id, active, created_at, updated_at)
SELECT circuit_seed.name,
       CONCAT(d.district_code, '-', circuit_seed.code_suffix),
       d.id,
       TRUE,
       NOW(),
       NOW()
FROM districts d
CROSS JOIN (
    VALUES
        ('Circuit A', 'A'),
        ('Circuit B', 'B'),
        ('Circuit C', 'C'),
        ('Circuit D', 'D')
) AS circuit_seed(name, code_suffix)
WHERE d.active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM circuits c
      WHERE c.district_id = d.id
        AND c.code = CONCAT(d.district_code, '-', circuit_seed.code_suffix)
  );

CREATE INDEX IF NOT EXISTS idx_districts_province_id ON districts (province_id);
CREATE INDEX IF NOT EXISTS idx_school_registration_requests_province_id ON school_registration_requests (province_id);
CREATE INDEX IF NOT EXISTS idx_school_registration_requests_circuit_id ON school_registration_requests (circuit_id);
