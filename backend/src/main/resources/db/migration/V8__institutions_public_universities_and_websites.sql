ALTER TABLE institutions
    ADD COLUMN IF NOT EXISTS city VARCHAR(120),
    ADD COLUMN IF NOT EXISTS province VARCHAR(120),
    ADD COLUMN IF NOT EXISTS country VARCHAR(120),
    ADD COLUMN IF NOT EXISTS website VARCHAR(255),
    ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS category VARCHAR(80),
    ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_institutions_active_featured_name ON institutions(active, featured, name);
CREATE INDEX IF NOT EXISTS idx_institutions_name_lower ON institutions((lower(name)));

WITH university_data(name, website, category, featured, country) AS (
    VALUES
        ('University of Cape Town', 'https://www.uct.ac.za', 'Traditional', TRUE, 'South Africa'),
        ('University of the Witwatersrand', 'https://www.wits.ac.za', 'Traditional', TRUE, 'South Africa'),
        ('Stellenbosch University', 'https://www.sun.ac.za', 'Traditional', TRUE, 'South Africa'),
        ('University of Pretoria', 'https://www.up.ac.za', 'Traditional', TRUE, 'South Africa'),
        ('University of Johannesburg', 'https://www.uj.ac.za', 'Traditional', TRUE, 'South Africa'),
        ('University of KwaZulu-Natal', 'https://www.ukzn.ac.za', 'Traditional', TRUE, 'South Africa'),
        ('North-West University', 'https://www.nwu.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('University of South Africa', 'https://www.unisa.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('University of the Free State', 'https://www.ufs.ac.za', 'Traditional', TRUE, 'South Africa'),
        ('Rhodes University', 'https://www.ru.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('University of the Western Cape', 'https://www.uwc.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('Nelson Mandela University', 'https://www.nmu.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('University of Fort Hare', 'https://www.ufh.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('Walter Sisulu University', 'https://www.wsu.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('University of Limpopo', 'https://www.ul.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('University of Venda', 'https://www.univen.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('University of Mpumalanga', 'https://www.ump.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('Sol Plaatje University', 'https://www.spu.ac.za', 'Traditional', FALSE, 'South Africa'),
        ('Tshwane University of Technology', 'https://www.tut.ac.za', 'University of Technology', TRUE, 'South Africa'),
        ('Cape Peninsula University of Technology', 'https://www.cput.ac.za', 'University of Technology', FALSE, 'South Africa'),
        ('Durban University of Technology', 'https://www.dut.ac.za', 'University of Technology', FALSE, 'South Africa'),
        ('Vaal University of Technology', 'https://www.vut.ac.za', 'University of Technology', FALSE, 'South Africa'),
        ('Central University of Technology', 'https://www.cut.ac.za', 'University of Technology', FALSE, 'South Africa'),
        ('Mangosuthu University of Technology', 'https://www.mut.ac.za', 'University of Technology', FALSE, 'South Africa'),
        ('University of Zululand', 'https://www.unizulu.ac.za', 'Comprehensive', FALSE, 'South Africa'),
        ('Sefako Makgatho Health Sciences University', 'https://www.smu.ac.za', 'Comprehensive', FALSE, 'South Africa')
),
updated AS (
    UPDATE institutions i
    SET website = u.website,
        category = COALESCE(i.category, u.category),
        featured = CASE WHEN u.featured THEN TRUE ELSE i.featured END,
        country = COALESCE(i.country, u.country),
        active = TRUE,
        updated_at = now()
    FROM university_data u
    WHERE lower(i.name) = lower(u.name)
    RETURNING lower(i.name) AS name_key
)
INSERT INTO institutions(name, website, category, featured, active, country, location)
SELECT u.name, u.website, u.category, u.featured, TRUE, u.country, 'South Africa'
FROM university_data u
LEFT JOIN updated up ON up.name_key = lower(u.name)
WHERE up.name_key IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM institutions i2 WHERE lower(i2.name) = lower(u.name)
  );
