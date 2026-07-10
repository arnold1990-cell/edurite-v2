import logoManifest from '@/assets/institutions/logo-manifest.json';

export type InstitutionType =
  | 'Traditional'
  | 'Comprehensive'
  | 'University of Technology'
  | 'Health Sciences'
  | 'TVET College'
  | 'Private Institution'
  | 'Online Learning Provider';

export type ApplicationStatus = 'OPEN' | 'OPENING_SOON' | 'CLOSED';

export interface InstitutionVisual {
  id?: string;
  name: string;
  abbreviation?: string;
  institutionType?: InstitutionType | string;
  category?: string;
  province?: string;
  city?: string;
  country?: string;
  officialWebsite?: string;
  applicationUrl?: string;
  logoPath?: string;
  logoUrl?: string;
  description?: string;
  faculties?: string[];
  programmeCount?: number | null;
  facultyCount?: number | null;
  applicationStatus?: ApplicationStatus | null;
  applicationOpeningDate?: string | null;
  applicationClosingDate?: string | null;
  qsRanking?: string | null;
  theRanking?: string | null;
  acceptanceIndicator?: string | null;
  isFeatured?: boolean;
  isActive?: boolean;
}

type LogoManifestRecord = {
  name: string;
  sourceUrl: string;
  localFilePath: string;
  fileType: string;
  retrievedAt: string;
  success: boolean;
  notes?: string;
};

const manifest = logoManifest as LogoManifestRecord[];

const assetModules = import.meta.glob('../assets/institutions/**/*.{svg,png,jpg,jpeg,webp}', {
  eager: true,
  import: 'default',
}) as Record<string, string>;

const normalizedAssets = new Map<string, string>(
  Object.entries(assetModules).map(([key, value]) => [
    key
      .replace('../assets/institutions/', '')
      .replace(/\\/g, '/')
      .toLowerCase(),
    value,
  ]),
);

const normalize = (value?: string | null) => (value ?? '').trim().toLowerCase();

const byName = new Map<string, InstitutionVisual>();

const resolveManifestLogo = (name?: string | null, explicitPath?: string | null) => {
  const preferredPath = explicitPath?.trim().toLowerCase();
  if (preferredPath) {
    const asset = normalizedAssets.get(preferredPath);
    if (asset) return asset;
  }

  const matched = manifest.find((item) => item.success && normalize(item.name) === normalize(name));
  if (!matched) return '';

  const relativePath = matched.localFilePath
    .replace(/^frontend\/src\/assets\/institutions\//i, '')
    .replace(/\\/g, '/')
    .toLowerCase();
  return normalizedAssets.get(relativePath) ?? '';
};

const deriveInitials = (name?: string | null) => {
  const value = (name ?? '').trim();
  if (!value) return 'ED';
  return value
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('') || value.slice(0, 2).toUpperCase();
};

const publicUniversities: InstitutionVisual[] = [
  {
    name: 'University of Cape Town',
    abbreviation: 'UCT',
    institutionType: 'Traditional',
    category: 'University',
    province: 'Western Cape',
    city: 'Cape Town',
    country: 'South Africa',
    officialWebsite: 'https://www.uct.ac.za',
    applicationUrl: 'https://www.uct.ac.za/study/apply',
    description: 'Research-intensive public university with six faculties and a central Cape Town campus.',
    faculties: ['Commerce', 'Engineering', 'Health Sciences', 'Humanities', 'Law', 'Science'],
    facultyCount: 6,
    qsRanking: 'QS 2027: 184',
    isFeatured: true,
    isActive: true,
  },
  {
    name: 'Stellenbosch University',
    abbreviation: 'SU',
    institutionType: 'Traditional',
    category: 'University',
    province: 'Western Cape',
    city: 'Stellenbosch',
    country: 'South Africa',
    officialWebsite: 'https://www.sun.ac.za',
    applicationUrl: 'https://www.sun.ac.za/english/students/Pages/Apply.aspx',
    logoPath: 'universities/stellenbosch-university-logo.jpeg',
    description: 'Public university in Stellenbosch with broad undergraduate and postgraduate study options.',
    faculties: ['AgriSciences', 'Arts', 'Education', 'Engineering', 'Law', 'Medicine', 'Science', 'Theology', 'Economic and Management Sciences'],
    facultyCount: 9,
    isFeatured: true,
    isActive: true,
  },
  { name: 'University of the Witwatersrand', abbreviation: 'Wits', institutionType: 'Traditional', category: 'University', province: 'Gauteng', city: 'Johannesburg', country: 'South Africa', officialWebsite: 'https://www.wits.ac.za', applicationUrl: 'https://www.wits.ac.za/applications/', description: 'Urban public research university in Johannesburg.', faculties: ['Commerce', 'Engineering', 'Health Sciences', 'Humanities', 'Law', 'Science'], facultyCount: 6, isFeatured: true, isActive: true },
  { name: 'University of Pretoria', abbreviation: 'UP', institutionType: 'Traditional', category: 'University', province: 'Gauteng', city: 'Pretoria', country: 'South Africa', officialWebsite: 'https://www.up.ac.za', applicationUrl: 'https://www.up.ac.za/online-application', description: 'Large public university in Pretoria with extensive professional and research programmes.', faculties: ['Economic and Management Sciences', 'Education', 'Engineering', 'Health Sciences', 'Humanities', 'Law', 'Natural and Agricultural Sciences', 'Theology and Religion', 'Veterinary Science'], facultyCount: 9, isFeatured: true, isActive: true },
  { name: 'University of Johannesburg', abbreviation: 'UJ', institutionType: 'Comprehensive', category: 'University', province: 'Gauteng', city: 'Johannesburg', country: 'South Africa', officialWebsite: 'https://www.uj.ac.za', applicationUrl: 'https://www.uj.ac.za/admission-aid/applying-to-uj/', description: 'Comprehensive public university with multiple campuses in Johannesburg.', faculties: ['Art, Design and Architecture', 'Business and Economics', 'Education', 'Engineering and the Built Environment', 'Health Sciences', 'Humanities', 'Law', 'Science'], facultyCount: 8, isFeatured: true, isActive: true },
  { name: 'North-West University', abbreviation: 'NWU', institutionType: 'Traditional', category: 'University', province: 'North West', city: 'Potchefstroom', country: 'South Africa', officialWebsite: 'https://www.nwu.ac.za', applicationUrl: 'https://www.nwu.ac.za/apply-online', isActive: true },
  { name: 'University of KwaZulu-Natal', abbreviation: 'UKZN', institutionType: 'Traditional', category: 'University', province: 'KwaZulu-Natal', city: 'Durban', country: 'South Africa', officialWebsite: 'https://www.ukzn.ac.za', applicationUrl: 'https://ww1.ukzn.ac.za/applications/', faculties: ['Agriculture', 'Engineering', 'Health Sciences', 'Humanities', 'Law', 'Management Studies', 'Medicine', 'Science', 'Social Sciences'], isFeatured: true, isActive: true },
  { name: 'Rhodes University', abbreviation: 'RU', institutionType: 'Traditional', category: 'University', province: 'Eastern Cape', city: 'Makhanda', country: 'South Africa', officialWebsite: 'https://www.ru.ac.za', applicationUrl: 'https://www.ru.ac.za/admissiongateway/applying/', isActive: true },
  { name: 'University of the Free State', abbreviation: 'UFS', institutionType: 'Traditional', category: 'University', province: 'Free State', city: 'Bloemfontein', country: 'South Africa', officialWebsite: 'https://www.ufs.ac.za', applicationUrl: 'https://www.ufs.ac.za/apply2026', faculties: ['Economic and Management Sciences', 'Education', 'Health Sciences', 'Humanities', 'Law', 'Natural and Agricultural Sciences', 'Theology and Religion'], facultyCount: 7, isFeatured: true, isActive: true },
  { name: 'Nelson Mandela University', abbreviation: 'NMU', institutionType: 'Comprehensive', category: 'University', province: 'Eastern Cape', city: 'Gqeberha', country: 'South Africa', officialWebsite: 'https://www.mandela.ac.za', applicationUrl: 'https://www.mandela.ac.za/Study-at-Mandela/Application', isActive: true },
  { name: 'University of South Africa', abbreviation: 'UNISA', institutionType: 'Comprehensive', category: 'University', province: 'Gauteng', city: 'Pretoria', country: 'South Africa', officialWebsite: 'https://www.unisa.ac.za', applicationUrl: 'https://www.unisa.ac.za/sites/corporate/default/Apply-for-admission', isActive: true },
  { name: 'University of the Western Cape', abbreviation: 'UWC', institutionType: 'Comprehensive', category: 'University', province: 'Western Cape', city: 'Cape Town', country: 'South Africa', officialWebsite: 'https://www.uwc.ac.za', applicationUrl: 'https://www.uwc.ac.za/study/apply', isActive: true },
  { name: 'University of Limpopo', abbreviation: 'UL', institutionType: 'Traditional', category: 'University', province: 'Limpopo', city: 'Mankweng', country: 'South Africa', officialWebsite: 'https://www.ul.ac.za', applicationUrl: 'https://www.ul.ac.za/index.php?Entity=Apply%20Online', isActive: true },
  { name: 'University of Venda', abbreviation: 'UNIVEN', institutionType: 'Traditional', category: 'University', province: 'Limpopo', city: 'Thohoyandou', country: 'South Africa', officialWebsite: 'https://www.univen.ac.za', applicationUrl: 'https://www.univen.ac.za/how-to-apply/', isActive: true },
  { name: 'University of Fort Hare', abbreviation: 'UFH', institutionType: 'Traditional', category: 'University', province: 'Eastern Cape', city: 'Alice', country: 'South Africa', officialWebsite: 'https://www.ufh.ac.za', applicationUrl: 'https://www.ufh.ac.za/apply/', isActive: true },
  { name: 'Walter Sisulu University', abbreviation: 'WSU', institutionType: 'Comprehensive', category: 'University', province: 'Eastern Cape', city: 'Mthatha', country: 'South Africa', officialWebsite: 'https://www.wsu.ac.za', applicationUrl: 'https://www.wsu.ac.za/index.php/en/apply-now', isActive: true },
  { name: 'University of Zululand', abbreviation: 'UNIZULU', institutionType: 'Comprehensive', category: 'University', province: 'KwaZulu-Natal', city: 'KwaDlangezwa', country: 'South Africa', officialWebsite: 'https://www.unizulu.ac.za', applicationUrl: 'https://www.unizulu.ac.za/apply-to-study/', isActive: true },
  { name: 'Mangosuthu University of Technology', abbreviation: 'MUT', institutionType: 'University of Technology', category: 'University', province: 'KwaZulu-Natal', city: 'Durban', country: 'South Africa', officialWebsite: 'https://www.mut.ac.za', applicationUrl: 'https://www.mut.ac.za/online-applications/', isActive: true },
  { name: 'Cape Peninsula University of Technology', abbreviation: 'CPUT', institutionType: 'University of Technology', category: 'University', province: 'Western Cape', city: 'Cape Town', country: 'South Africa', officialWebsite: 'https://www.cput.ac.za', applicationUrl: 'https://www.cput.ac.za/study/apply', isActive: true },
  { name: 'Central University of Technology', abbreviation: 'CUT', institutionType: 'University of Technology', category: 'University', province: 'Free State', city: 'Bloemfontein', country: 'South Africa', officialWebsite: 'https://www.cut.ac.za', applicationUrl: 'https://www.cut.ac.za/application-process', isActive: true },
  { name: 'Durban University of Technology', abbreviation: 'DUT', institutionType: 'University of Technology', category: 'University', province: 'KwaZulu-Natal', city: 'Durban', country: 'South Africa', officialWebsite: 'https://www.dut.ac.za', applicationUrl: 'https://www.dut.ac.za/student_portal/online_application/', isActive: true },
  { name: 'Tshwane University of Technology', abbreviation: 'TUT', institutionType: 'University of Technology', category: 'University', province: 'Gauteng', city: 'Pretoria', country: 'South Africa', officialWebsite: 'https://www.tut.ac.za', applicationUrl: 'https://www.tut.ac.za/study-at-tut/i-want-to-study/apply-to-tut', isFeatured: true, isActive: true },
  { name: 'Vaal University of Technology', abbreviation: 'VUT', institutionType: 'University of Technology', category: 'University', province: 'Gauteng', city: 'Vanderbijlpark', country: 'South Africa', officialWebsite: 'https://www.vut.ac.za', applicationUrl: 'https://www.vut.ac.za/apply/', isActive: true },
  { name: 'Sefako Makgatho Health Sciences University', abbreviation: 'SMU', institutionType: 'Health Sciences', category: 'University', province: 'Gauteng', city: 'Ga-Rankuwa', country: 'South Africa', officialWebsite: 'https://www.smu.ac.za', applicationUrl: 'https://www.smu.ac.za/apply/', isActive: true },
  { name: 'Sol Plaatje University', abbreviation: 'SPU', institutionType: 'Traditional', category: 'University', province: 'Northern Cape', city: 'Kimberley', country: 'South Africa', officialWebsite: 'https://www.spu.ac.za', applicationUrl: 'https://www.spu.ac.za/Students/Apply', isActive: true },
  { name: 'University of Mpumalanga', abbreviation: 'UMP', institutionType: 'Traditional', category: 'University', province: 'Mpumalanga', city: 'Mbombela', country: 'South Africa', officialWebsite: 'https://www.ump.ac.za', applicationUrl: 'https://www.ump.ac.za/Study-with-us/Apply', isActive: true },
];

const onlineProviders: InstitutionVisual[] = [
  { name: 'Alison', abbreviation: 'ALISON', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://alison.com', isActive: true },
  { name: 'Coursera', abbreviation: 'COURSERA', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.coursera.org', isActive: true },
  { name: 'edX', abbreviation: 'EDX', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.edx.org', isActive: true },
  { name: 'FutureLearn', abbreviation: 'FUTURELEARN', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.futurelearn.com', isActive: true },
  { name: 'Khan Academy', abbreviation: 'KA', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.khanacademy.org', isActive: true },
  { name: 'freeCodeCamp', abbreviation: 'FCC', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.freecodecamp.org', isActive: true },
  { name: 'Microsoft Learn', abbreviation: 'MS', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://learn.microsoft.com', isActive: true },
  { name: 'Cisco Networking Academy', abbreviation: 'CISCO', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.netacad.com', isActive: true },
  { name: 'OpenLearn', abbreviation: 'OPENLEARN', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.open.edu/openlearn', isActive: true },
  { name: 'MIT OpenCourseWare', abbreviation: 'MIT OCW', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://ocw.mit.edu', isActive: true },
  { name: 'W3Schools', abbreviation: 'W3', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.w3schools.com', isActive: true },
  { name: 'AWS Skill Builder', abbreviation: 'AWS', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://skillbuilder.aws', isActive: true },
  { name: 'Oracle University', abbreviation: 'ORACLE', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://education.oracle.com', isActive: true },
  { name: 'IBM SkillsBuild', abbreviation: 'IBM', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://skillsbuild.org', isActive: true },
  { name: 'Google Cloud Skills Boost', abbreviation: 'GCP', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://www.cloudskillsboost.google', isActive: true },
  { name: 'Salesforce Trailhead', abbreviation: 'SF', institutionType: 'Online Learning Provider', category: 'Online', country: 'Global', officialWebsite: 'https://trailhead.salesforce.com', isActive: true },
  { name: 'Siyavula', abbreviation: 'SIYAVULA', institutionType: 'Online Learning Provider', category: 'Online', country: 'South Africa', officialWebsite: 'https://www.siyavula.com', isActive: true },
  { name: 'DBE Cloud', abbreviation: 'DBE', institutionType: 'Online Learning Provider', category: 'Online', country: 'South Africa', officialWebsite: 'https://www.dbecloud.org', isActive: true },
  { name: 'WCED ePortal', abbreviation: 'WCED', institutionType: 'Online Learning Provider', category: 'Online', country: 'South Africa', officialWebsite: 'https://wcedeportal.co.za', isActive: true },
  { name: 'Mindset Learn', abbreviation: 'MINDSET', institutionType: 'Online Learning Provider', category: 'Online', country: 'South Africa', officialWebsite: 'https://learn.mindset.africa', isActive: true },
];

const directory = [...publicUniversities, ...onlineProviders];
directory.forEach((item) => byName.set(normalize(item.name), item));

export const getInstitutionVisual = (name?: string | null) => byName.get(normalize(name));

export const resolveInstitutionDisplay = (institution: {
  id?: string | null;
  name?: string | null;
  abbreviation?: string | null;
  institutionType?: string | null;
  officialWebsite?: string | null;
  website?: string | null;
  applicationUrl?: string | null;
  logoPath?: string | null;
  logoUrl?: string | null;
  province?: string | null;
  city?: string | null;
  country?: string | null;
  category?: string | null;
  description?: string | null;
  faculties?: string[] | string | null;
  programmeCount?: number | null;
  facultyCount?: number | null;
  applicationStatus?: ApplicationStatus | string | null;
  applicationOpeningDate?: string | null;
  applicationClosingDate?: string | null;
  qsRanking?: string | null;
  theRanking?: string | null;
  acceptanceIndicator?: string | null;
  featured?: boolean | null;
  active?: boolean | null;
}) => {
  const visual = getInstitutionVisual(institution.name);
  const website = institution.officialWebsite?.trim() || institution.website?.trim() || visual?.officialWebsite || '';
  const faculties = Array.isArray(institution.faculties)
    ? institution.faculties
    : typeof institution.faculties === 'string'
      ? institution.faculties.split(',').map((item) => item.trim()).filter(Boolean)
      : visual?.faculties ?? [];

  const abbreviation = institution.abbreviation?.trim() || visual?.abbreviation || deriveInitials(institution.name);
  const logoPath = institution.logoPath?.trim() || visual?.logoPath || '';
  const localLogo = resolveManifestLogo(institution.name, logoPath);
  const logoUrl = localLogo || institution.logoUrl?.trim() || visual?.logoUrl || '';

  return {
    ...visual,
    ...institution,
    displayName: institution.name?.trim() || visual?.name || '',
    abbreviation,
    displayInitials: abbreviation,
    institutionType: institution.institutionType?.trim() || visual?.institutionType || institution.category?.trim() || 'Institution',
    category: institution.category?.trim() || visual?.category || '',
    province: institution.province?.trim() || visual?.province || '',
    city: institution.city?.trim() || visual?.city || '',
    country: institution.country?.trim() || visual?.country || 'South Africa',
    officialWebsite: website,
    website,
    applicationUrl: institution.applicationUrl?.trim() || visual?.applicationUrl || '',
    logoPath,
    logoUrl,
    faculties,
    facultyCount: institution.facultyCount ?? visual?.facultyCount ?? (faculties.length || null),
    programmeCount: institution.programmeCount ?? visual?.programmeCount ?? null,
    applicationStatus: institution.applicationStatus ?? visual?.applicationStatus ?? null,
    applicationOpeningDate: institution.applicationOpeningDate ?? visual?.applicationOpeningDate ?? null,
    applicationClosingDate: institution.applicationClosingDate ?? visual?.applicationClosingDate ?? null,
    qsRanking: institution.qsRanking ?? visual?.qsRanking ?? null,
    theRanking: institution.theRanking ?? visual?.theRanking ?? null,
    acceptanceIndicator: institution.acceptanceIndicator ?? visual?.acceptanceIndicator ?? null,
    isFeatured: institution.featured ?? visual?.isFeatured ?? false,
    isActive: institution.active ?? visual?.isActive ?? true,
  };
};

export const institutionDirectory = directory;
export const institutionLogoManifest = manifest;
