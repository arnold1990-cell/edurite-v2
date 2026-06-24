export type CapsPhaseGroup = 'foundation-stream' | 'intermediate' | 'senior' | 'fet';
export type CapsPhase = 'ECD' | 'Foundation' | 'Intermediate' | 'Senior' | 'FET';

export type CapsSubjectOption = {
  id: string;
  phaseGroup: CapsPhaseGroup;
  phase: CapsPhase;
  phaseLabel: string;
  grade: string;
  category: string;
  subjectName: string;
  languageLevel?: string;
  subjectType: string;
  isLanguage: boolean;
  isCompulsory: boolean;
};

export const CAPS_PHASE_GROUPS: Array<{ value: CapsPhaseGroup; label: string; grades: string[] }> = [
  { value: 'foundation-stream', label: 'ECD / Foundation Phase Grade R-3', grades: ['Grade R', 'Grade 1', 'Grade 2', 'Grade 3'] },
  { value: 'intermediate', label: 'Intermediate Phase Grades 4-6', grades: ['Grade 4', 'Grade 5', 'Grade 6'] },
  { value: 'senior', label: 'Senior Phase Grades 7-9', grades: ['Grade 7', 'Grade 8', 'Grade 9'] },
  { value: 'fet', label: 'FET Phase Grades 10-12', grades: ['Grade 10', 'Grade 11', 'Grade 12'] },
];

export const CAPS_CATEGORY_OPTIONS: Record<CapsPhaseGroup, string[]> = {
  'foundation-stream': ['Home Language', 'First Additional Language', 'Core Subjects'],
  intermediate: ['Languages', 'Mathematics', 'Natural Sciences & Technology', 'Social Sciences', 'Life Skills'],
  senior: ['Languages', 'Mathematics', 'Natural Sciences', 'Social Sciences', 'EMS', 'Technology', 'Creative Arts', 'Life Orientation'],
  fet: ['Languages', 'Mathematics / Mathematical Literacy', 'Life Orientation', 'Sciences', 'Commerce', 'Technology', 'Engineering', 'Agriculture', 'Arts', 'Tourism', 'Hospitality', 'Consumer Studies', 'Other Specialised Subjects'],
};

const foundationLanguages = ['English', 'Afrikaans', 'isiZulu', 'isiXhosa', 'Sepedi', 'Setswana', 'Sesotho', 'Siswati', 'Tshivenda', 'Xitsonga', 'isiNdebele'];
const fetLanguages = ['English', 'Afrikaans', 'isiZulu', 'isiXhosa', 'Setswana', 'Sesotho', 'Sepedi', 'Siswati', 'Tshivenda', 'Xitsonga', 'isiNdebele'];

const option = (
  phaseGroup: CapsPhaseGroup,
  phase: CapsPhase,
  phaseLabel: string,
  grade: string,
  category: string,
  subjectName: string,
  subjectType: string,
  config?: { languageLevel?: string; isLanguage?: boolean; isCompulsory?: boolean },
): CapsSubjectOption => ({
  id: `${phase}|${grade}|${category}|${subjectName}|${config?.languageLevel ?? ''}`,
  phaseGroup,
  phase,
  phaseLabel,
  grade,
  category,
  subjectName,
  languageLevel: config?.languageLevel,
  subjectType,
  isLanguage: Boolean(config?.isLanguage),
  isCompulsory: Boolean(config?.isCompulsory),
});

export const CAPS_SUBJECT_OPTIONS: CapsSubjectOption[] = [
  ...(['Grade R', 'Grade 1', 'Grade 2', 'Grade 3'] as const).flatMap((grade) => {
    const actualPhase: CapsPhase = grade === 'Grade R' ? 'ECD' : 'Foundation';
    const label = 'ECD / Foundation Phase';
    return [
      ...foundationLanguages.map((language) => option('foundation-stream', actualPhase, label, grade, 'Home Language', `${language} Home Language`, 'Language', { languageLevel: 'Home Language', isLanguage: true, isCompulsory: true })),
      option('foundation-stream', actualPhase, label, grade, 'First Additional Language', 'First Additional Language', 'Language', { languageLevel: 'First Additional Language', isLanguage: true, isCompulsory: true }),
      option('foundation-stream', actualPhase, label, grade, 'Core Subjects', 'Mathematics', 'Core', { isCompulsory: true }),
      option('foundation-stream', actualPhase, label, grade, 'Core Subjects', 'Life Skills', 'Core', { isCompulsory: true }),
    ];
  }),
  ...(['Grade 4', 'Grade 5', 'Grade 6'] as const).flatMap((grade) => ([
    option('intermediate', 'Intermediate', 'Intermediate Phase', grade, 'Languages', 'Home Language', 'Language', { languageLevel: 'Home Language', isLanguage: true, isCompulsory: true }),
    option('intermediate', 'Intermediate', 'Intermediate Phase', grade, 'Languages', 'First Additional Language', 'Language', { languageLevel: 'First Additional Language', isLanguage: true, isCompulsory: true }),
    option('intermediate', 'Intermediate', 'Intermediate Phase', grade, 'Mathematics', 'Mathematics', 'Mathematics', { isCompulsory: true }),
    option('intermediate', 'Intermediate', 'Intermediate Phase', grade, 'Natural Sciences & Technology', 'Natural Sciences & Technology', 'Science', { isCompulsory: true }),
    option('intermediate', 'Intermediate', 'Intermediate Phase', grade, 'Social Sciences', 'Social Sciences', 'Humanities', { isCompulsory: true }),
    option('intermediate', 'Intermediate', 'Intermediate Phase', grade, 'Social Sciences', 'History', 'Humanities'),
    option('intermediate', 'Intermediate', 'Intermediate Phase', grade, 'Social Sciences', 'Geography', 'Humanities'),
    option('intermediate', 'Intermediate', 'Intermediate Phase', grade, 'Life Skills', 'Life Skills', 'Core', { isCompulsory: true }),
  ])),
  ...(['Grade 7', 'Grade 8', 'Grade 9'] as const).flatMap((grade) => ([
    option('senior', 'Senior', 'Senior Phase', grade, 'Languages', 'Home Language', 'Language', { languageLevel: 'Home Language', isLanguage: true, isCompulsory: true }),
    option('senior', 'Senior', 'Senior Phase', grade, 'Languages', 'First Additional Language', 'Language', { languageLevel: 'First Additional Language', isLanguage: true, isCompulsory: true }),
    option('senior', 'Senior', 'Senior Phase', grade, 'Mathematics', 'Mathematics', 'Mathematics', { isCompulsory: true }),
    option('senior', 'Senior', 'Senior Phase', grade, 'Natural Sciences', 'Natural Sciences', 'Science', { isCompulsory: true }),
    option('senior', 'Senior', 'Senior Phase', grade, 'Social Sciences', 'Social Sciences', 'Humanities', { isCompulsory: true }),
    option('senior', 'Senior', 'Senior Phase', grade, 'Technology', 'Technology', 'Technology'),
    option('senior', 'Senior', 'Senior Phase', grade, 'EMS', 'Economic & Management Sciences', 'Commerce'),
    option('senior', 'Senior', 'Senior Phase', grade, 'Life Orientation', 'Life Orientation', 'Core', { isCompulsory: true }),
    option('senior', 'Senior', 'Senior Phase', grade, 'Creative Arts', 'Creative Arts', 'Arts'),
  ])),
  ...(['Grade 10', 'Grade 11', 'Grade 12'] as const).flatMap((grade) => ([
    option('fet', 'FET', 'FET Phase', grade, 'Languages', 'Home Language', 'Language', { languageLevel: 'Home Language', isLanguage: true, isCompulsory: true }),
    option('fet', 'FET', 'FET Phase', grade, 'Languages', 'First Additional Language', 'Language', { languageLevel: 'First Additional Language', isLanguage: true, isCompulsory: true }),
    option('fet', 'FET', 'FET Phase', grade, 'Mathematics / Mathematical Literacy', 'Mathematics', 'Mathematics', { isCompulsory: true }),
    option('fet', 'FET', 'FET Phase', grade, 'Mathematics / Mathematical Literacy', 'Mathematical Literacy', 'Mathematics', { isCompulsory: true }),
    option('fet', 'FET', 'FET Phase', grade, 'Life Orientation', 'Life Orientation', 'Core', { isCompulsory: true }),
    ...fetLanguages.map((language) => option('fet', 'FET', 'FET Phase', grade, 'Languages', language, 'Language', { isLanguage: true })),
    option('fet', 'FET', 'FET Phase', grade, 'Sciences', 'Physical Sciences', 'Science'),
    option('fet', 'FET', 'FET Phase', grade, 'Sciences', 'Life Sciences', 'Science'),
    option('fet', 'FET', 'FET Phase', grade, 'Sciences', 'Agricultural Sciences', 'Science'),
    option('fet', 'FET', 'FET Phase', grade, 'Technology', 'Information Technology', 'Technology'),
    option('fet', 'FET', 'FET Phase', grade, 'Technology', 'Computer Applications Technology', 'Technology'),
    option('fet', 'FET', 'FET Phase', grade, 'Technology', 'Technical Mathematics', 'Technology'),
    option('fet', 'FET', 'FET Phase', grade, 'Technology', 'Technical Sciences', 'Technology'),
    option('fet', 'FET', 'FET Phase', grade, 'Commerce', 'Accounting', 'Commerce'),
    option('fet', 'FET', 'FET Phase', grade, 'Commerce', 'Business Studies', 'Commerce'),
    option('fet', 'FET', 'FET Phase', grade, 'Commerce', 'Economics', 'Commerce'),
    option('fet', 'FET', 'FET Phase', grade, 'Arts', 'Visual Arts', 'Arts'),
    option('fet', 'FET', 'FET Phase', grade, 'Arts', 'Dramatic Arts', 'Arts'),
    option('fet', 'FET', 'FET Phase', grade, 'Arts', 'Dance Studies', 'Arts'),
    option('fet', 'FET', 'FET Phase', grade, 'Arts', 'Music', 'Arts'),
    option('fet', 'FET', 'FET Phase', grade, 'Arts', 'Design', 'Arts'),
    option('fet', 'FET', 'FET Phase', grade, 'Engineering', 'Civil Technology', 'Engineering'),
    option('fet', 'FET', 'FET Phase', grade, 'Engineering', 'Electrical Technology', 'Engineering'),
    option('fet', 'FET', 'FET Phase', grade, 'Engineering', 'Mechanical Technology', 'Engineering'),
    option('fet', 'FET', 'FET Phase', grade, 'Engineering', 'Engineering Graphics and Design', 'Engineering'),
    option('fet', 'FET', 'FET Phase', grade, 'Agriculture', 'Agricultural Management Practices', 'Agriculture'),
    option('fet', 'FET', 'FET Phase', grade, 'Agriculture', 'Agricultural Technology', 'Agriculture'),
    option('fet', 'FET', 'FET Phase', grade, 'Tourism', 'Tourism', 'Services'),
    option('fet', 'FET', 'FET Phase', grade, 'Hospitality', 'Hospitality Studies', 'Services'),
    option('fet', 'FET', 'FET Phase', grade, 'Consumer Studies', 'Consumer Studies', 'Services'),
    option('fet', 'FET', 'FET Phase', grade, 'Other Specialised Subjects', 'History', 'Humanities'),
    option('fet', 'FET', 'FET Phase', grade, 'Other Specialised Subjects', 'Geography', 'Humanities'),
    option('fet', 'FET', 'FET Phase', grade, 'Other Specialised Subjects', 'Religion Studies', 'Humanities'),
    option('fet', 'FET', 'FET Phase', grade, 'Other Specialised Subjects', 'Maritime Economics', 'Specialised'),
    option('fet', 'FET', 'FET Phase', grade, 'Other Specialised Subjects', 'Maritime Studies', 'Specialised'),
    option('fet', 'FET', 'FET Phase', grade, 'Other Specialised Subjects', 'Aviation Studies', 'Specialised'),
    option('fet', 'FET', 'FET Phase', grade, 'Other Specialised Subjects', 'Sport and Exercise Science', 'Specialised'),
  ])),
];

export const DEFAULT_CAREER_PATHWAYS: Record<string, string[]> = {
  Mathematics: ['Engineering', 'Data Science', 'Actuarial Science'],
  'Physical Sciences': ['Medicine', 'Engineering', 'Chemistry'],
  Accounting: ['Finance', 'Auditing', 'Business Management'],
  'Computer Applications Technology': ['Software Engineering', 'Cybersecurity', 'AI'],
  'Information Technology': ['Software Engineering', 'Cybersecurity', 'AI'],
  Geography: ['Urban Planning', 'Environmental Science', 'GIS'],
  Economics: ['Economics', 'Banking', 'Policy Analysis'],
  'Life Sciences': ['Medicine', 'Biotechnology', 'Environmental Health'],
  English: ['Law', 'Media Studies', 'Communications'],
  'Business Studies': ['Entrepreneurship', 'Business Management', 'Marketing'],
};

export const gradeMatchesPhaseGroup = (phaseGroup: CapsPhaseGroup, grade: string) =>
  CAPS_PHASE_GROUPS.find((item) => item.value === phaseGroup)?.grades.includes(grade) ?? false;
