import { AccessBadge } from '@/features/subscriptions/access';
import { Link, useLocation } from 'react-router-dom';

export const studentSections = [
  {
    "to": "/student/profile",
    "label": "My Profile",
    "paths": [
      "profile",
      "academic-profile",
      "documents",
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
        "profile?section=documents"
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
      "psychometric",
      "psychometric-test",
      "explore",
      "careers",
      "career-roadmaps",
      "recommendations/careers"
    ],
    "links": [
      [
        "Discover",
        "career-explorer?section=discover"
      ],
      [
        "Career Guidance",
        "career-explorer?section=guidance"
      ],
      [
        "Career Match",
        "career-explorer?section=career-match"
      ],
      [
        "Psychometric / Interests",
        "career-explorer?section=interests"
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
      "study-options"
    ],
    "links": []
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
    "to": "/student/institutions",
    "label": "Institutions",
    "paths": [
      "institutions",
      "universities",
      "colleges-tvets",
      "university-applications"
    ],
    "links": [
      [
        "Explore",
        "institutions?section=explore"
      ],
      [
        "Universities",
        "institutions?section=universities"
      ],
      [
        "Colleges / TVET",
        "institutions?section=colleges-tvets"
      ],
      [
        "Programmes",
        "institutions?section=programmes"
      ],
      ["Saved", "institutions?section=saved"],
      [
        "Applications",
        "institutions?section=applications"
      ]
    ]
  },
  {
    "to": "/student/learning",
    "label": "Learning Resources",
    "paths": [
      "learning",
      "ai-tutor",
      "ai-guidance",
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
        "AI Guidance",
        "learning?section=guidance"
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
  {
    "to": "/student/progress",
    "label": "My Progress",
    "paths": [
      "progress",
      "rewards"
    ],
    "links": [
      [
        "Overview",
        "progress?section=overview"
      ],
      [
        "Academic Progress",
        "progress?section=academic"
      ],
      [
        "Learning Progress",
        "progress?section=learning"
      ],
      [
        "Achievements",
        "progress?section=achievements"
      ],
      [
        "Points & Rewards",
        "progress?section=rewards"
      ]
    ]
  },
  {
    "to": "/student/goals",
    "label": "My Goals",
    "paths": [
      "goals"
    ],
    "links": []
  },
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
  const params = new URLSearchParams(search);
  if (path === 'profile' && params.get('tab') === 'goals') return '/student/goals';
  if (path === 'explore' || path === 'career-explorer') {
    const category = params.get('category');
    if (category === 'Courses' || category === 'Subjects') return '/student/study-options';
    if (category === 'Institutions') return '/student/institutions';
    if (category === 'Bursaries' || category === 'Opportunities') return '/student/funding';
  }
  return studentSections.find(section => section.paths.some(prefix => path === prefix || path.startsWith(`${prefix}/`)))?.to
    ?? (path === 'dashboard' ? '/student/dashboard' : undefined);
}

export const studentTools = [
  ...studentSections.flatMap(group => [{ label: group.label, to: group.to }, ...group.links.map(([label, path]) => ({ label: `${group.label} / ${label}`, to: `/student/${path}` }))]),
  { label: 'Qualifications and Experience', to: '/student/profile?section=career' },
  { label: 'Saved Profiles and Documents', to: '/student/profile?section=documents' },
  { label: 'Bursary Finder', to: '/student/funding?section=bursaries' },
  { label: 'Scholarship Assistant', to: '/student/funding?section=scholarships' },
  { label: 'University Explorer / Universities', to: '/student/institutions?section=universities' },
  { label: 'University Applications', to: '/student/institutions?section=applications' },
  { label: 'Career Roadmaps', to: '/student/career-explorer?section=career-path' },
  ...['Subscription', 'Notifications', 'Settings'].map(label => ({ label, to: `/student/${label.toLowerCase()}` })),
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
