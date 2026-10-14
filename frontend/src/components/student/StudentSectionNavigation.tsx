import { AccessBadge } from '@/features/subscriptions/access';
import { Link, useLocation } from 'react-router-dom';

export const studentSections = [
  {
    "to": "/student/profile",
    "label": "My Profile",
    "paths": [
      "profile",
      "academic-profile",
      "qualifications",
      "experience",
      "my-school",
      "cv-builder"
    ],
    "links": [
      [
        "Overview",
        "profile?section=overview"
      ],
      [
        "Academic Profile",
        "profile?section=academic"
      ],
      [
        "Interests & Skills",
        "profile?section=interests"
      ],
      [
        "Career Aspirations",
        "profile?section=career"
      ],
      [
        "School",
        "profile?section=school"
      ],
      [
        "Documents",
        "documents"
      ],
      [
        "CV Builder",
        "profile?section=cv"
      ]
    ]
  },
  {
    "to": "/student/career-explorer",
    "label": "Career Explorer",
    "paths": [
      "career-explorer",
      "explore",
      "careers",
      "career-roadmaps",
      "career-path"
    ],
    "links": [
      [
        "Discover",
        "career-explorer?section=discover"
      ],
      [
        "Career Guidance",
        "career-guidance"
      ],
      [
        "Career Match",
        "career-match"
      ],
      [
        "Career Path",
        "career-explorer?section=career-path"
      ],
      [
        "Learning Path",
        "career-explorer?section=learning-path"
      ],
      [
        "Academic Readiness",
        "career-explorer?section=readiness"
      ],
      [
        "Saved Careers",
        "career-explorer?section=saved"
      ]
    ]
  },
  {
    "to": "/student/study-options",
    "label": "Study Options",
    "paths": [
      "study-options",
      "institutions",
      "universities",
      "colleges-tvets",
      "university-applications"
    ],
    "links": [
      [
        "Recommended for You",
        "study-options?section=recommended"
      ],
      [
        "Explore",
        "study-options?section=explore"
      ],
      [
        "Universities",
        "study-options?section=universities"
      ],
      [
        "Colleges / TVET",
        "study-options?section=colleges-tvets"
      ],
      [
        "Programmes",
        "study-options?section=programmes"
      ],
      [
        "Saved",
        "study-options?section=saved"
      ],
      [
        "Applications",
        "study-options?section=applications"
      ]
    ]
  },
  {
    "to": "/student/funding",
    "label": "Bursaries & Funding",
    "paths": [
      "funding",
      "saved",
      "applications",
      "scholarships",
      "bursaries",
      "opportunities",
      "scholarship-assistant",
      "recommendations/bursaries"
    ],
    "links": [
      [
        "Discover",
        "funding?section=discover"
      ],
      [
        "Opportunities",
        "funding?section=opportunities"
      ],
      [
        "Bursaries",
        "funding?section=bursaries"
      ],
      [
        "Scholarships",
        "funding?section=scholarships"
      ],
      [
        "Funding Match",
        "funding?section=matches"
      ],
      [
        "Saved",
        "funding?section=saved"
      ],
      [
        "Applications",
        "funding?section=applications"
      ]
    ]
  },
  {
    "to": "/student/learning",
    "label": "Learning Resources",
    "paths": [
      "learning",
      "ai-tutor",
      "learning-centre",
      "interview-prep"
    ],
    "links": [
      [
        "Overview",
        "learning?section=overview"
      ],
      [
        "EduRite Tutor",
        "learning?section=tutor"
      ],
      [
        "Learning Centre",
        "learning?section=centre"
      ],
      [
        "Study Plan",
        "learning?section=study-plan"
      ],
      [
        "Resources",
        "learning?section=resources"
      ]
    ]
  },
  { to: '/student/documents', label: 'Documents', paths: ['documents'], links: [] },
  { to: '/student/settings', label: 'Account Settings', paths: ['settings'], links: [] },
  { to: '/student/career-guidance', label: 'Career Guidance', paths: ['career-guidance'], links: [] },
  { to: '/student/career-match', label: 'Career Match', paths: ['career-match', 'ai-guidance', 'recommendations/careers', 'psychometric', 'psychometric-test'], links: [] },
  {
    "to": "/student/messages",
    "label": "Messages",
    "paths": [
      "messages"
    ],
    "links": []
  }
];

export function studentSection(pathname: string, search = '') {
  const path = pathname.replace(/^\/student\/?/, '').replace(/\/$/, '');
  if (['progress', 'goals', 'rewards'].some(prefix => path === prefix || path.startsWith(prefix + '/'))) return '/student/dashboard';
  const params = new URLSearchParams(search);
  if (path === 'profile' && ['documents', 'settings'].includes(params.get('section') || params.get('tab') || '')) return '/student/documents';
  if (['career-explorer', 'learning'].includes(path) && params.get('section') === 'guidance') return '/student/career-match';
  if (path === 'career-explorer' && ['career-match', 'interests'].includes(params.get('section') || '')) return '/student/career-match';
  if (path === 'profile' && params.get('tab') === 'goals') return '/student/profile';
  if (path === 'explore' || path === 'career-explorer') {
    const category = params.get('category');
    if (category === 'Courses' || category === 'Subjects') return '/student/study-options';
    if (category === 'Institutions') return '/student/study-options';
    if (category === 'Bursaries' || category === 'Opportunities') return '/student/funding';
  }
  return studentSections.find(section => section.paths.some(prefix => path === prefix || path.startsWith(`${prefix}/`)))?.to
    ?? (path === 'dashboard' ? '/student/dashboard' : undefined);
}

export const studentTools = [
  { label: 'Career Match / EduRite AI Guidance', to: '/student/career-match' },
  { label: 'Career Match / Psychometric assessment', to: '/student/career-match?step=assessment' },
  { label: 'Points & Rewards', to: '/student/dashboard?section=rewards' },
  { label: 'Goals', to: '/student/profile?section=goals' },
  ...studentSections.flatMap(group => [{ label: group.label, to: group.to }, ...group.links.map(([label, path]) => ({ label: `${group.label} / ${label}`, to: `/student/${path}` }))]),
  { label: 'Qualifications and Experience', to: '/student/profile?section=career' },
  { label: 'Saved Profiles and Documents', to: '/student/documents' },
  { label: 'Bursary Finder', to: '/student/funding?section=bursaries' },
  { label: 'Scholarship Assistant', to: '/student/funding?section=scholarships' },
  { label: 'University Explorer / Universities', to: '/student/study-options?section=universities' },
  { label: 'University Applications', to: '/student/study-options?section=applications' },
  { label: 'Career Roadmaps', to: '/student/career-explorer?section=career-path' },
  ...['Subscription', 'Notifications'].map(label => ({ label, to: `/student/${label.toLowerCase()}` })),
];

export function StudentSectionNavigation() {
  const { pathname, search } = useLocation();
  const section = studentSections.find(item => item.to === studentSection(pathname, search));
  if (!section?.links.length) return null;
  const profileTabs: Record<string, string> = { academic: 'academic', interests: 'interests', career: 'career', settings: 'documents', overview: 'overview' };
  const selected = new URLSearchParams(search).get('section') || (pathname === '/student/profile' ? profileTabs[new URLSearchParams(search).get('tab') || ''] : undefined) || new URLSearchParams(section.links[0][1].split('?')[1]).get('section');
  return <div className="ed-section-heading"><h1>{section.label}</h1><nav className="ed-section-navigation" aria-label={`${section.label} sections`}>
    {section.links.map(([label, path]) => <Link key={path} to={`/student/${path}`} aria-current={new URLSearchParams(path.split('?')[1]).get('section') === selected ? 'page' : undefined}>{label}<AccessBadge to={`/student/${path}`} /></Link>)}
  </nav></div>;
}
