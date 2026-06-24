WITH tvet_data(name, province, website, country, category, location) AS (
    VALUES
        ('Buffalo City TVET College', 'Eastern Cape', 'https://www.bccollege.co.za', 'South Africa', 'TVET', 'Eastern Cape, South Africa'),
        ('Eastcape Midlands TVET College', 'Eastern Cape', 'https://www.emcol.co.za', 'South Africa', 'TVET', 'Eastern Cape, South Africa'),
        ('Ikhala TVET College', 'Eastern Cape', 'https://www.ikhalacollege.co.za', 'South Africa', 'TVET', 'Eastern Cape, South Africa'),
        ('Ingwe TVET College', 'Eastern Cape', 'https://www.ingwecollege.co.za', 'South Africa', 'TVET', 'Eastern Cape, South Africa'),
        ('King Hintsa TVET College', 'Eastern Cape', 'https://www.kinghintsacollege.edu.za', 'South Africa', 'TVET', 'Eastern Cape, South Africa'),
        ('King Sabata Dalindyebo TVET College', 'Eastern Cape', NULL, 'South Africa', 'TVET', 'Eastern Cape, South Africa'),
        ('Lovedale TVET College', 'Eastern Cape', 'https://www.lovedalecollege.co.za', 'South Africa', 'TVET', 'Eastern Cape, South Africa'),
        ('Port Elizabeth TVET College', 'Eastern Cape', 'https://www.pecollege.edu.za', 'South Africa', 'TVET', 'Eastern Cape, South Africa'),

        ('Flavius Mareka TVET College', 'Free State', 'https://www.flaviusmareka.net', 'South Africa', 'TVET', 'Free State, South Africa'),
        ('Goldfields TVET College', 'Free State', 'https://www.goldfieldsfet.edu.za', 'South Africa', 'TVET', 'Free State, South Africa'),
        ('Maluti TVET College', 'Free State', 'https://www.malutifet.org.za', 'South Africa', 'TVET', 'Free State, South Africa'),
        ('Motheo TVET College', 'Free State', 'https://www.motheofet.co.za', 'South Africa', 'TVET', 'Free State, South Africa'),

        ('Central Johannesburg TVET College', 'Gauteng', 'https://www.cjc.co.za', 'South Africa', 'TVET', 'Gauteng, South Africa'),
        ('Ekurhuleni East TVET College', 'Gauteng', 'https://www.eec.edu.za', 'South Africa', 'TVET', 'Gauteng, South Africa'),
        ('Ekurhuleni West TVET College', 'Gauteng', 'https://www.ewc.edu.za', 'South Africa', 'TVET', 'Gauteng, South Africa'),
        ('Sedibeng TVET College', 'Gauteng', 'https://www.sedcol.co.za', 'South Africa', 'TVET', 'Gauteng, South Africa'),
        ('South West Gauteng TVET College', 'Gauteng', 'https://www.swgc.co.za', 'South Africa', 'TVET', 'Gauteng, South Africa'),
        ('Tshwane North TVET College', 'Gauteng', 'https://www.tnc.edu.za', 'South Africa', 'TVET', 'Gauteng, South Africa'),
        ('Tshwane South TVET College', 'Gauteng', 'https://www.tsc.edu.za', 'South Africa', 'TVET', 'Gauteng, South Africa'),
        ('Western TVET College', 'Gauteng', 'https://www.westcol.co.za', 'South Africa', 'TVET', 'Gauteng, South Africa'),

        ('Coastal TVET College', 'KwaZulu-Natal', 'https://www.coastalkzn.co.za', 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),
        ('Elangeni TVET College', 'KwaZulu-Natal', 'https://www.efet.co.za', 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),
        ('Esayidi TVET College', 'KwaZulu-Natal', 'https://www.esayidifet.co.za', 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),
        ('Majuba TVET College', 'KwaZulu-Natal', 'https://www.majuba.edu.za', 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),
        ('Mnambithi TVET College', 'KwaZulu-Natal', NULL, 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),
        ('Mthashana TVET College', 'KwaZulu-Natal', 'https://www.mthashanafet.co.za', 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),
        ('Thekwini TVET College', 'KwaZulu-Natal', 'https://www.thekwinicollege.co.za', 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),
        ('Umfolozi TVET College', 'KwaZulu-Natal', 'https://www.umfolozicollege.co.za', 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),
        ('Umgungundlovu TVET College', 'KwaZulu-Natal', 'https://www.ufetc.edu.za', 'South Africa', 'TVET', 'KwaZulu-Natal, South Africa'),

        ('Capricorn TVET College', 'Limpopo', 'https://www.capricorncollege.edu.za', 'South Africa', 'TVET', 'Limpopo, South Africa'),
        ('Lephalale TVET College', 'Limpopo', 'https://www.lephalalefetcollege.co.za', 'South Africa', 'TVET', 'Limpopo, South Africa'),
        ('Letaba TVET College', 'Limpopo', 'https://www.letabafet.co.za', 'South Africa', 'TVET', 'Limpopo, South Africa'),
        ('Mopani South East TVET College', 'Limpopo', 'https://www.mopanicollege.edu.za', 'South Africa', 'TVET', 'Limpopo, South Africa'),
        ('Sekhukhune TVET College', 'Limpopo', 'https://www.sekfetcol.co.za', 'South Africa', 'TVET', 'Limpopo, South Africa'),
        ('Vhembe TVET College', 'Limpopo', 'https://www.vhembefet.co.za', 'South Africa', 'TVET', 'Limpopo, South Africa'),
        ('Waterberg TVET College', 'Limpopo', 'https://www.waterbergcollege.co.za', 'South Africa', 'TVET', 'Limpopo, South Africa'),

        ('Ehlanzeni TVET College', 'Mpumalanga', 'https://www.ehlanzenicollege.co.za', 'South Africa', 'TVET', 'Mpumalanga, South Africa'),
        ('Gert Sibande TVET College', 'Mpumalanga', 'https://www.gscollege.co.za', 'South Africa', 'TVET', 'Mpumalanga, South Africa'),
        ('Nkangala TVET College', 'Mpumalanga', 'https://www.nkangalafet.edu.za', 'South Africa', 'TVET', 'Mpumalanga, South Africa'),

        ('Northern Cape Rural TVET College', 'Northern Cape', 'https://www.ncrfet.edu.za', 'South Africa', 'TVET', 'Northern Cape, South Africa'),
        ('Northern Cape Urban TVET College', 'Northern Cape', 'https://www.ncufetcollege.edu.za', 'South Africa', 'TVET', 'Northern Cape, South Africa'),

        ('Orbit TVET College', 'North West', 'https://www.orbitcollege.co.za', 'South Africa', 'TVET', 'North West, South Africa'),
        ('Taletso TVET College', 'North West', 'https://www.taletsofetcollege.co.za', 'South Africa', 'TVET', 'North West, South Africa'),
        ('Vuselela TVET College', 'North West', 'https://www.vuselelacollege.co.za', 'South Africa', 'TVET', 'North West, South Africa'),

        ('Boland TVET College', 'Western Cape', 'https://www.bolandcollege.com', 'South Africa', 'TVET', 'Western Cape, South Africa'),
        ('College of Cape Town', 'Western Cape', 'https://www.cct.edu.za', 'South Africa', 'TVET', 'Western Cape, South Africa'),
        ('False Bay TVET College', 'Western Cape', 'https://www.falsebaycollege.co.za', 'South Africa', 'TVET', 'Western Cape, South Africa'),
        ('Northlink TVET College', 'Western Cape', 'https://www.northlink.co.za', 'South Africa', 'TVET', 'Western Cape, South Africa'),
        ('South Cape TVET College', 'Western Cape', 'https://www.sccollege.co.za', 'South Africa', 'TVET', 'Western Cape, South Africa'),
        ('West Coast TVET College', 'Western Cape', 'https://www.westcoastcollege.co.za', 'South Africa', 'TVET', 'Western Cape, South Africa')
),
updated AS (
    UPDATE institutions i
    SET province = d.province,
        website = COALESCE(d.website, i.website),
        country = d.country,
        category = d.category,
        location = d.location,
        active = TRUE,
        updated_at = now()
    FROM tvet_data d
    WHERE lower(i.name) = lower(d.name)
    RETURNING lower(i.name) AS name_key
)
INSERT INTO institutions(name, province, website, country, category, location, featured, active)
SELECT d.name, d.province, d.website, d.country, d.category, d.location, FALSE, TRUE
FROM tvet_data d
LEFT JOIN updated u ON u.name_key = lower(d.name)
WHERE u.name_key IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM institutions i2 WHERE lower(i2.name) = lower(d.name)
  );
