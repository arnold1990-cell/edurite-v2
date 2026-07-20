import logoManifest from '@/assets/institutions/logo-manifest.json';

export type InstitutionType = 'Traditional' | 'Comprehensive' | 'University of Technology' | 'Health Sciences' | 'TVET College' | 'Private Institution' | 'Online Learning Provider';
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
  aliases?: string[];
}

type LogoManifestRecord = { name: string; sourceUrl: string; localFilePath: string; fileType: string; retrievedAt: string; success: boolean; notes?: string };
type InstitutionLike = {
  id?: string | null; name?: string | null; abbreviation?: string | null; institutionType?: string | null; officialWebsite?: string | null; website?: string | null;
  admissionsInfoUrl?: string | null; onlineApplicationUrl?: string | null; applicationLinkStatus?: string | null; applicationUrl?: string | null; logoPath?: string | null; logoUrl?: string | null;
  province?: string | null; city?: string | null; country?: string | null; category?: string | null; description?: string | null; faculties?: string[] | string | null;
  programmeCount?: number | null; facultyCount?: number | null; applicationStatus?: ApplicationStatus | string | null; applicationOpeningDate?: string | null; applicationClosingDate?: string | null;
  qsRanking?: string | null; theRanking?: string | null; acceptanceIndicator?: string | null; featured?: boolean | null; active?: boolean | null;
};

const manifest = logoManifest as LogoManifestRecord[];
const assetModules = import.meta.glob(
  [
    '../assets/institutions/**/*.svg',
    '../assets/institutions/**/*.png',
    '../assets/institutions/**/*.jpg',
    '../assets/institutions/**/*.jpeg',
    '../assets/institutions/**/*.webp',
  ],
  { eager: true, import: 'default' },
) as Record<string, string>;
const normalizedAssets = new Map<string, string>(Object.entries(assetModules).map(([key, value]) => [key.replace('../assets/institutions/', '').replace(/\\/g, '/').toLowerCase(), value]));
const normalize = (value?: string | null) => (value ?? '').trim().toLowerCase();
const deriveInitials = (name?: string | null) => {
  const value = (name ?? '').trim();
  if (!value) return 'ED';
  return value.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]?.toUpperCase() ?? '').join('') || value.slice(0, 2).toUpperCase();
};
const resolveManifestLogo = (name?: string | null, explicitPath?: string | null) => {
  const preferredPath = explicitPath?.trim().replace(/^frontend\/src\/assets\/institutions\//i, '').replace(/\\/g, '/').toLowerCase();
  if (preferredPath) {
    const asset = normalizedAssets.get(preferredPath);
    if (asset) return asset;
  }
  const matched = manifest.find((item) => item.success && normalize(item.name) === normalize(name));
  if (!matched) return '';
  const relativePath = matched.localFilePath.replace(/^frontend\/src\/assets\/institutions\//i, '').replace(/\\/g, '/').toLowerCase();
  return normalizedAssets.get(relativePath) ?? '';
};

const u = (name: string, abbreviation: string, institutionType: InstitutionVisual['institutionType'], province: string, city: string, officialWebsite: string, applicationUrl: string, extra: Partial<InstitutionVisual> = {}): InstitutionVisual => ({
  name, abbreviation, institutionType, category: 'University', province, city, country: 'South Africa', officialWebsite, applicationUrl, isActive: true, ...extra,
});
const t = (name: string, province: string, officialWebsite = '', logoPath = '', abbreviation = '', aliases: string[] = []): InstitutionVisual => ({
  name, abbreviation: abbreviation || undefined, institutionType: 'TVET College', category: 'TVET', province, country: 'South Africa', officialWebsite: officialWebsite || undefined, logoPath: logoPath || undefined, aliases: aliases.length ? aliases : undefined, isActive: true,
});
const p = (name: string, officialWebsite: string, abbreviation = '', aliases: string[] = []): InstitutionVisual => ({
  name, abbreviation: abbreviation || undefined, institutionType: 'Private Institution', category: 'Private', country: 'South Africa', officialWebsite, aliases: aliases.length ? aliases : undefined, isActive: true,
});
const o = (name: string, officialWebsite: string, logoPath = '', abbreviation = ''): InstitutionVisual => ({
  name, abbreviation: abbreviation || undefined, institutionType: 'Online Learning Provider', category: 'Online', country: name === 'Siyavula' || name === 'DBE Cloud' || name === 'WCED ePortal' || name === 'Mindset Learn' ? 'South Africa' : 'Global', officialWebsite, logoPath: logoPath || undefined, isActive: true,
});

const publicUniversities: InstitutionVisual[] = [
  u('University of Cape Town', 'UCT', 'Traditional', 'Western Cape', 'Cape Town', 'https://www.uct.ac.za', 'https://www.uct.ac.za/study/apply', { description: 'Public research university in Cape Town with undergraduate and postgraduate study across six faculties.', faculties: ['Commerce', 'Engineering', 'Health Sciences', 'Humanities', 'Law', 'Science'], facultyCount: 6, qsRanking: 'QS 2027: 184', isFeatured: true }),
  u('Stellenbosch University', 'SU', 'Traditional', 'Western Cape', 'Stellenbosch', 'https://www.sun.ac.za', 'https://www.sun.ac.za/english/students/Pages/Apply.aspx', { faculties: ['AgriSciences', 'Arts', 'Education', 'Engineering', 'Law', 'Medicine', 'Science', 'Theology', 'Economic and Management Sciences'], facultyCount: 9, isFeatured: true }),
  u('University of the Witwatersrand', 'Wits', 'Traditional', 'Gauteng', 'Johannesburg', 'https://www.wits.ac.za', 'https://www.wits.ac.za/applications/', { faculties: ['Commerce, Law and Management', 'Engineering and the Built Environment', 'Health Sciences', 'Humanities', 'Science'], facultyCount: 5, programmeCount: 3000, isFeatured: true }),
  u('University of Pretoria', 'UP', 'Traditional', 'Gauteng', 'Pretoria', 'https://www.up.ac.za', 'https://www.up.ac.za/online-application', { faculties: ['Economic and Management Sciences', 'Education', 'Engineering, Built Environment and IT', 'Health Sciences', 'Humanities', 'Law', 'Natural and Agricultural Sciences', 'Theology and Religion', 'Veterinary Science'], facultyCount: 9, programmeCount: 1200, isFeatured: true }),
  u('University of Johannesburg', 'UJ', 'Comprehensive', 'Gauteng', 'Johannesburg', 'https://www.uj.ac.za', 'https://www.uj.ac.za/admission-aid/applying-to-uj/', { faculties: ['Business and Economics', 'Art, Design and Architecture', 'Education', 'Engineering and the Built Environment', 'Health Sciences', 'Humanities', 'Law', 'Science'], facultyCount: 8, isFeatured: true }),
  u('North-West University', 'NWU', 'Traditional', 'North West', 'Potchefstroom', 'https://www.nwu.ac.za', 'https://www.nwu.ac.za/apply-online'),
  u('University of KwaZulu-Natal', 'UKZN', 'Traditional', 'KwaZulu-Natal', 'Durban', 'https://www.ukzn.ac.za', 'https://ww1.ukzn.ac.za/applications/', { faculties: ['Agriculture', 'Engineering', 'Health Sciences', 'Humanities', 'Law', 'Management Studies', 'Medicine', 'Science', 'Social Sciences'], isFeatured: true }),
  u('Rhodes University', 'RU', 'Traditional', 'Eastern Cape', 'Makhanda', 'https://www.ru.ac.za', 'https://www.ru.ac.za/admissiongateway/applying/'),
  u('University of the Free State', 'UFS', 'Traditional', 'Free State', 'Bloemfontein', 'https://www.ufs.ac.za', 'https://www.ufs.ac.za/apply2026', { faculties: ['Economic and Management Sciences', 'Education', 'Health Sciences', 'Humanities', 'Law', 'Natural and Agricultural Sciences', 'Theology and Religion'], facultyCount: 7, isFeatured: true }),
  u('Nelson Mandela University', 'NMU', 'Comprehensive', 'Eastern Cape', 'Gqeberha', 'https://www.mandela.ac.za', 'https://www.mandela.ac.za/Study-at-Mandela/Application'),
  u('University of South Africa', 'UNISA', 'Comprehensive', 'Gauteng', 'Pretoria', 'https://www.unisa.ac.za', 'https://www.unisa.ac.za/sites/corporate/default/Apply-for-admission'),
  u('University of the Western Cape', 'UWC', 'Comprehensive', 'Western Cape', 'Cape Town', 'https://www.uwc.ac.za', 'https://www.uwc.ac.za/study/apply'),
  u('University of Limpopo', 'UL', 'Traditional', 'Limpopo', 'Mankweng', 'https://www.ul.ac.za', 'https://www.ul.ac.za/index.php?Entity=Apply%20Online'),
  u('University of Venda', 'UNIVEN', 'Traditional', 'Limpopo', 'Thohoyandou', 'https://www.univen.ac.za', 'https://www.univen.ac.za/how-to-apply/'),
  u('University of Fort Hare', 'UFH', 'Traditional', 'Eastern Cape', 'Alice', 'https://www.ufh.ac.za', 'https://www.ufh.ac.za/apply/'),
  u('Walter Sisulu University', 'WSU', 'Comprehensive', 'Eastern Cape', 'Mthatha', 'https://www.wsu.ac.za', 'https://www.wsu.ac.za/index.php/en/apply-now'),
  u('University of Zululand', 'UNIZULU', 'Comprehensive', 'KwaZulu-Natal', 'KwaDlangezwa', 'https://www.unizulu.ac.za', 'https://www.unizulu.ac.za/apply-to-study/'),
  u('Mangosuthu University of Technology', 'MUT', 'University of Technology', 'KwaZulu-Natal', 'Durban', 'https://www.mut.ac.za', 'https://www.mut.ac.za/online-applications/'),
  u('Cape Peninsula University of Technology', 'CPUT', 'University of Technology', 'Western Cape', 'Cape Town', 'https://www.cput.ac.za', 'https://www.cput.ac.za/study/apply'),
  u('Central University of Technology', 'CUT', 'University of Technology', 'Free State', 'Bloemfontein', 'https://www.cut.ac.za', 'https://www.cut.ac.za/application-process'),
  u('Durban University of Technology', 'DUT', 'University of Technology', 'KwaZulu-Natal', 'Durban', 'https://www.dut.ac.za', 'https://www.dut.ac.za/student_portal/online_application/'),
  u('Tshwane University of Technology', 'TUT', 'University of Technology', 'Gauteng', 'Pretoria', 'https://www.tut.ac.za', 'https://www.tut.ac.za/study-at-tut/i-want-to-study/apply-to-tut', { isFeatured: true }),
  u('Vaal University of Technology', 'VUT', 'University of Technology', 'Gauteng', 'Vanderbijlpark', 'https://www.vut.ac.za', 'https://www.vut.ac.za/apply/'),
  u('Sefako Makgatho Health Sciences University', 'SMU', 'Health Sciences', 'Gauteng', 'Ga-Rankuwa', 'https://www.smu.ac.za', 'https://www.smu.ac.za/apply/'),
  u('Sol Plaatje University', 'SPU', 'Traditional', 'Northern Cape', 'Kimberley', 'https://www.spu.ac.za', 'https://www.spu.ac.za/Students/Apply'),
  u('University of Mpumalanga', 'UMP', 'Traditional', 'Mpumalanga', 'Mbombela', 'https://www.ump.ac.za', 'https://www.ump.ac.za/Study-with-us/Apply'),
];

const tvetColleges: InstitutionVisual[] = [
  t('Buffalo City TVET College', 'Eastern Cape', 'https://www.bccollege.co.za', 'tvet/buffalo-city-tvet-college-logo.png'),
  t('Eastcape Midlands TVET College', 'Eastern Cape', 'https://www.emcol.co.za'), t('Ikhala TVET College', 'Eastern Cape', 'https://www.ikhalacollege.co.za'), t('Ingwe TVET College', 'Eastern Cape', 'https://www.ingwecollege.co.za'),
  t('King Hintsa TVET College', 'Eastern Cape', 'https://www.kinghintsacollege.edu.za', 'tvet/king-hintsa-tvet-college-logo.svg'), t('King Sabata Dalindyebo TVET College', 'Eastern Cape', '', '', 'KSD'),
  t('Lovedale TVET College', 'Eastern Cape', 'https://www.lovedalecollege.co.za'), t('Port Elizabeth TVET College', 'Eastern Cape', 'https://www.pecollege.edu.za', 'tvet/port-elizabeth-tvet-college-logo.png', 'PE TVET'),
  t('Flavius Mareka TVET College', 'Free State', 'https://www.flaviusmareka.net', 'tvet/flavius-mareka-tvet-college-logo.png'), t('Goldfields TVET College', 'Free State', 'https://www.goldfieldsfet.edu.za'), t('Maluti TVET College', 'Free State', 'https://www.malutifet.org.za'), t('Motheo TVET College', 'Free State', 'https://www.motheofet.co.za'),
  t('Central Johannesburg TVET College', 'Gauteng', 'https://www.cjc.co.za', '', 'CJC'), t('Ekurhuleni East TVET College', 'Gauteng', 'https://www.eec.edu.za', 'tvet/ekurhuleni-east-tvet-college-logo.png', 'EEC'), t('Ekurhuleni West TVET College', 'Gauteng', 'https://www.ewc.edu.za', 'tvet/ekurhuleni-west-tvet-college-logo.jpg', 'EWC'), t('Sedibeng TVET College', 'Gauteng', 'https://www.sedcol.co.za', 'tvet/sedibeng-tvet-college-logo.png'),
  t('South West Gauteng TVET College', 'Gauteng', 'https://www.swgc.co.za', 'tvet/south-west-gauteng-tvet-college-logo.svg', 'SWGC'), t('Tshwane North TVET College', 'Gauteng', 'https://www.tnc.edu.za', 'tvet/tshwane-north-tvet-college-logo.webp', 'TNC'), t('Tshwane South TVET College', 'Gauteng', 'https://www.tsc.edu.za', '', 'TSC'), t('Western TVET College', 'Gauteng', 'https://www.westcol.co.za', 'tvet/western-tvet-college-logo.svg'),
  t('Coastal TVET College', 'KwaZulu-Natal', 'https://www.coastalkzn.co.za', 'tvet/coastal-tvet-college-logo.png'), t('Elangeni TVET College', 'KwaZulu-Natal', 'https://www.efet.co.za'), t('Esayidi TVET College', 'KwaZulu-Natal', 'https://www.esayidifet.co.za', 'tvet/esayidi-tvet-college-logo.png', '', ['Esayidi FET College']), t('Majuba TVET College', 'KwaZulu-Natal', 'https://www.majuba.edu.za'),
  t('Mnambithi TVET College', 'KwaZulu-Natal', '', '', 'Mnambithi'), t('Mthashana TVET College', 'KwaZulu-Natal', 'https://www.mthashanafet.co.za'), t('Thekwini TVET College', 'KwaZulu-Natal', 'https://www.thekwinicollege.co.za'), t('Umfolozi TVET College', 'KwaZulu-Natal', 'https://www.umfolozicollege.co.za'), t('Umgungundlovu TVET College', 'KwaZulu-Natal', 'https://www.ufetc.edu.za', '', 'UTVET'),
  t('Capricorn TVET College', 'Limpopo', 'https://www.capricorncollege.edu.za', 'tvet/capricorn-tvet-college-logo.png', '', ['Capricorn College']), t('Lephalale TVET College', 'Limpopo', 'https://www.lephalalefetcollege.co.za'), t('Letaba TVET College', 'Limpopo', 'https://www.letabafet.co.za'), t('Mopani South East TVET College', 'Limpopo', 'https://www.mopanicollege.edu.za', '', 'Mopani SE'), t('Sekhukhune TVET College', 'Limpopo', 'https://www.sekfetcol.co.za'), t('Vhembe TVET College', 'Limpopo', 'https://www.vhembefet.co.za'), t('Waterberg TVET College', 'Limpopo', 'https://www.waterbergcollege.co.za', 'tvet/waterberg-tvet-college-logo.png'),
  t('Ehlanzeni TVET College', 'Mpumalanga', 'https://www.ehlanzenicollege.co.za', 'tvet/ehlanzeni-tvet-college-logo.png'), t('Gert Sibande TVET College', 'Mpumalanga', 'https://www.gscollege.co.za'), t('Nkangala TVET College', 'Mpumalanga', 'https://www.nkangalafet.edu.za'),
  t('Northern Cape Rural TVET College', 'Northern Cape', 'https://www.ncrfet.edu.za', '', 'NCR TVET'), t('Northern Cape Urban TVET College', 'Northern Cape', 'https://www.ncufetcollege.edu.za', '', 'NCU TVET'),
  t('Orbit TVET College', 'North West', 'https://www.orbitcollege.co.za', 'tvet/orbit-tvet-college-logo.png'), t('Taletso TVET College', 'North West', 'https://www.taletsofetcollege.co.za'), t('Vuselela TVET College', 'North West', 'https://www.vuselelacollege.co.za', 'tvet/vuselela-tvet-college-logo.png'),
  t('Boland TVET College', 'Western Cape', 'https://www.bolandcollege.com', 'tvet/boland-tvet-college-logo.png'), t('College of Cape Town', 'Western Cape', 'https://www.cct.edu.za', 'tvet/college-of-cape-town-logo.png', 'CCT'), t('False Bay TVET College', 'Western Cape', 'https://www.falsebaycollege.co.za', 'tvet/false-bay-tvet-college-logo.svg'), t('Northlink TVET College', 'Western Cape', 'https://www.northlink.co.za', 'tvet/northlink-tvet-college-logo.png'), t('South Cape TVET College', 'Western Cape', 'https://www.sccollege.co.za', 'tvet/south-cape-tvet-college-logo.png'), t('West Coast TVET College', 'Western Cape', 'https://www.westcoastcollege.co.za', 'tvet/west-coast-tvet-college-logo.png'),
];

const privateInstitutions: InstitutionVisual[] = [
  p('IIE Varsity College', 'https://www.varsitycollege.co.za', 'VC'), p('Eduvos', 'https://www.eduvos.com', 'EDUVOS'), p('Belgium Campus', 'https://www.belgiumcampus.ac.za', 'BCIT'), p('Milpark Education', 'https://www.milpark.ac.za', 'MILPARK'), p('STADIO Higher Education', 'https://www.stadio.ac.za', 'STADIO'), p('Regenesys Business School', 'https://www.regenesys.net', 'REGENESYS'), p('IMM Graduate School', 'https://www.imm.ac.za', 'IMM'), p('Boston City Campus', 'https://www.boston.co.za', 'BCC'), p('Damelin', 'https://www.damelin.co.za', 'DAMELIN'), p('Richfield Graduate Institute of Technology', 'https://www.richfield.ac.za', 'RICHFIELD'), p('AFDA', 'https://www.afda.co.za', 'AFDA'), p('Academy of Sound Engineering', 'https://www.ase.co.za', 'ASE'),
  p('Monash South Africa', 'https://www.iie.ac.za', 'MSA', ['IIE MSA']), p('SACAP', 'https://www.sacap.edu.za', 'SACAP', ['The South African College of Applied Psychology']), p('MANCOSA', 'https://www.mancosa.co.za', 'MANCOSA', ['Management College of Southern Africa']), p('Lyceum College', 'https://www.lyceum.co.za', 'LYCEUM'), p('Open Window Institute', 'https://www.openwindow.co.za', 'OWI'), p('Vega School', 'https://www.vegaschool.com', 'VEGA'), p('Red and Yellow Creative School', 'https://www.redandyellow.co.za', 'R&Y'), p('Design Academy of Fashion', 'https://www.daf-academy.com', 'DAF'), p('ICESA Education', 'https://www.icesa.co.za', 'ICESA'), p('Cornerstone Institute', 'https://www.cornerstone.ac.za', 'CI'), p('Helderberg College', 'https://www.hche.ac.za', 'HCHE'), p('Oakfields College', 'https://www.oakfieldscollege.co.za', 'OAKFIELDS'), p('CityVarsity', 'https://www.cityvarsity.co.za', 'CITYVARSITY'), p('Optimi College', 'https://www.optimi.co.za', 'OPTIMI'), p('Emendy Multimedia Technology Institute', 'https://www.emendy.co.za', 'EMENDY'),
];

const onlineProviders: InstitutionVisual[] = [
  o('Alison', 'https://alison.com', 'online/alison-logo.svg', 'ALISON'), o('Coursera', 'https://www.coursera.org', 'online/coursera-logo.png', 'COURSERA'), o('edX', 'https://www.edx.org', 'online/edx-logo.svg', 'EDX'), o('FutureLearn', 'https://www.futurelearn.com', '', 'FUTURELEARN'), o('Khan Academy', 'https://www.khanacademy.org', 'online/khan-academy-logo.svg', 'KA'), o('freeCodeCamp', 'https://www.freecodecamp.org', '', 'FCC'), o('Microsoft Learn', 'https://learn.microsoft.com', 'online/microsoft-learn-logo.svg', 'MS'), o('Cisco Networking Academy', 'https://www.netacad.com', '', 'CISCO'), o('OpenLearn', 'https://www.open.edu/openlearn', '', 'OPENLEARN'), o('MIT OpenCourseWare', 'https://ocw.mit.edu', 'online/mit-opencourseware-logo.svg', 'MIT OCW'),
  o('W3Schools', 'https://www.w3schools.com', 'online/w3schools-logo.svg', 'W3'), o('AWS Skill Builder', 'https://skillbuilder.aws', '', 'AWS'), o('Oracle University', 'https://education.oracle.com', '', 'ORACLE'), o('IBM SkillsBuild', 'https://skillsbuild.org', '', 'IBM'), o('Google Cloud Skills Boost', 'https://www.cloudskillsboost.google', '', 'GCP'), o('Salesforce Trailhead', 'https://trailhead.salesforce.com', 'online/salesforce-trailhead-logo.svg', 'SF'), o('Siyavula', 'https://www.siyavula.com', 'online/siyavula-logo.svg', 'SIYAVULA'), o('DBE Cloud', 'https://www.dbecloud.org', '', 'DBE'), o('WCED ePortal', 'https://wcedeportal.co.za', 'online/wced-eportal-logo.png', 'WCED'), o('Mindset Learn', 'https://learn.mindset.africa', '', 'MINDSET'),
];

const directory = [...publicUniversities, ...tvetColleges, ...privateInstitutions, ...onlineProviders];
const byName = new Map<string, InstitutionVisual>();
for (const item of directory) {
  byName.set(normalize(item.name), item);
  item.aliases?.forEach((alias) => byName.set(normalize(alias), item));
}

const UNIVERSITY_TYPES = new Set(['traditional', 'comprehensive', 'university of technology', 'health sciences', 'university']);
export const isUniversityEntry = (institution?: Pick<InstitutionLike, 'institutionType' | 'category'> | null) => {
  const normalizedType = normalize(institution?.institutionType || institution?.category);
  return UNIVERSITY_TYPES.has(normalizedType) || normalizedType.includes('university');
};
export const isTvetEntry = (institution?: Pick<InstitutionLike, 'institutionType' | 'category' | 'name'> | null) => normalize(institution?.institutionType || institution?.category || institution?.name).includes('tvet');
export const isPrivateEntry = (institution?: Pick<InstitutionLike, 'institutionType' | 'category'> | null) => normalize(institution?.institutionType || institution?.category).includes('private');
export const isOnlineEntry = (institution?: Pick<InstitutionLike, 'institutionType' | 'category'> | null) => normalize(institution?.institutionType || institution?.category).includes('online');
export const getInstitutionVisual = (name?: string | null) => byName.get(normalize(name));

export const buildMergedInstitutionList = (items: InstitutionLike[] = [], matcher: (institution: InstitutionLike) => boolean) => {
  const merged = new Map<string, InstitutionLike>();
  items.filter((item) => item?.name).forEach((item) => { if (matcher(item)) merged.set(normalize(item.name), item); });
  directory.filter((item) => matcher(item)).forEach((item) => { const key = normalize(item.name); if (!merged.has(key)) merged.set(key, item); });
  return Array.from(merged.values());
};

export const resolveInstitutionDisplay = (institution: InstitutionLike) => {
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
  const resolvedApplicationUrl = institution.applicationUrl?.trim() || institution.onlineApplicationUrl?.trim() || visual?.applicationUrl || '';

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
    admissionsInfoUrl: institution.admissionsInfoUrl?.trim() || '',
    onlineApplicationUrl: institution.onlineApplicationUrl?.trim() || '',
    applicationLinkStatus: institution.applicationLinkStatus?.trim() || '',
    applicationUrl: resolvedApplicationUrl,
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
