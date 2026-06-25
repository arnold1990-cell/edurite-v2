import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import careersBackgroundImage from '@/assets/images/careers.jpeg';
import bursariesBackgroundImage from '@/assets/images/bursaries.jpeg';
import coursesBackgroundImage from '@/assets/images/courses.jpeg';
import { MetricCard } from '@/components/cards/MetricCard';
import { PlaceholderChart } from '@/components/charts/PlaceholderChart';
import { EduRiteLogo } from '@/components/common/EduRiteLogo';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback/States';
import { BackgroundSection } from '@/components/sections/BackgroundSection';
import { DataTable } from '@/components/tables/DataTable';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { useAppQuery } from '@/hooks/useAppQuery';
import { formatPlanPrice } from '@/lib/pricing';
import { bursaryService, type AggregatedBursary } from '@/services/bursaryService';
import { careerService } from '@/services/careerService';
import { courseService } from '@/services/courseService';
import { institutionService } from '@/services/institutionService';
import { publicDiscoveryService } from '@/services/publicDiscoveryService';
import { subscriptionService } from '@/services/subscriptionService';
import type { Career, Course, Institution, PaginatedResponse } from '@/types';

interface PageIntroProps {
  title: string;
  subtitle: string;
}

interface FilterBarProps {
  placeholder: string;
  locationLabel: string;
  categoryLabel: string;
  filters: DiscoveryFilters;
  onChange: (key: keyof DiscoveryFilters, value: string) => void;
  onApply: () => void;
  isApplying?: boolean;
}

interface DiscoveryFilters {
  q: string;
  location: string;
  category: string;
}

interface FeatureCard {
  title: string;
  description: string;
}

interface LandingBackgroundPanel extends FeatureCard {
  badge: string;
  imageSrc: string;
  imagePositionClassName?: string;
}

interface PricingPlan {
  code?: string;
  name: string;
  price: string;
  desc: string;
  features: readonly string[];
  featured?: boolean;
}

const PUBLIC_PRICE_LABEL = 'R49.99 / month';

interface InsightCardProps {
  title: string;
  summary?: string;
  aiUsed?: boolean;
  highlights?: string[];
}

const LANDING_HIGHLIGHTS: FeatureCard[] = [
  {
    title: 'Career discovery',
    description:
      'Assess strengths, interests, and subject performance to identify high-fit career paths.',
  },
  {
    title: 'Funding marketplace',
    description:
      'Browse bursaries with transparent eligibility criteria and deadline tracking.',
  },
  {
    title: 'Talent sourcing',
    description:
      'Company dashboards to shortlist top student candidates with structured scorecards.',
  },
];

const LANDING_VALUE_SECTIONS: Array<{ title: string; description: string; bullets: string[] }> = [
  {
    title: 'What EduRite Does',
    description: 'EduRite connects students, opportunities, and practical readiness support in one platform.',
    bullets: ['Career-path discovery', 'Funding and bursary matching', 'Learning interventions for skill gaps'],
  },
  {
    title: 'Psychometric Testing',
    description: 'Students complete a structured psychometric assessment after login to improve recommendation quality.',
    bullets: ['Strength and growth-area scoring', 'Saved results linked to student profile', 'Public-mode architecture for future anonymous access'],
  },
  {
    title: 'Career Guidance',
    description: 'AI guidance combines profile data, psychometric insights, and opportunity context to provide next-step clarity.',
    bullets: ['Career and programme recommendations', 'Guidance rationale and action steps', 'Profile-completion prompts for better outcomes'],
  },
  {
    title: 'Bursaries & Opportunities',
    description: 'Students can discover and bookmark bursaries and opportunity streams aligned to their goals.',
    bullets: ['Eligibility-aware discovery', 'Deadline reminders and notifications', 'Application tracking support'],
  },
  {
    title: 'Learning Centre',
    description: 'Resources are mapped to weaknesses so students can improve practical readiness over time.',
    bullets: ['Categorised learning resources', 'Outcome-to-resource mapping', 'Expandable content model for future growth'],
  },
  {
    title: 'Trust & Compliance',
    description: 'EduRite enforces verification, consent, and auditability to protect student data and platform integrity.',
    bullets: ['Phone OTP verification before full activation', 'POPIA consent capture with versioning', 'Auditable account lifecycle events'],
  },
];

const NOTIFICATION_FEATURES = [
  'Receive alerts for new bursary opportunities',
  'Get reminders before application deadlines',
  'Stay updated with new career insights and recommendations',
] as const;

const HUMAN_CENTERED_WORKFLOWS = [
  'Students to discover opportunities and receive personalized guidance',
  'Companies to identify and recruit suitable talent',
  'Administrators to manage approvals, bursaries, and platform operations efficiently',
] as const;

const LANDING_BACKGROUND_PANELS: LandingBackgroundPanel[] = [
  {
    badge: 'Careers',
    imageSrc: careersBackgroundImage,
    title: 'Explore career paths with confidence',
    description:
      'Match student strengths to in-demand roles with clearer pathways, recommendations, and skill-aligned discovery.',
    imagePositionClassName: 'object-center',
  },
  {
    badge: 'Courses',
    imageSrc: coursesBackgroundImage,
    title: 'Compare the right courses faster',
    description:
      'Review accredited programs, institutions, and learning options without overwhelming first-time applicants.',
    imagePositionClassName: 'object-[center_35%]',
  },
  {
    badge: 'Bursaries',
    imageSrc: bursariesBackgroundImage,
    title: 'Keep funding opportunities easy to scan',
    description:
      'Present bursaries with stronger contrast, better readability, and a cleaner layout across mobile and desktop screens.',
    imagePositionClassName: 'object-[center_60%]',
  },
];

const pricingPlans: readonly PricingPlan[] = [
  {
    code: 'PLAN_BASIC',
    name: 'Starter',
    price: 'Free',
    desc: 'Get started with essential tools',
    features: ['Basic profile creation', 'Explore careers and courses', 'Limited discovery features'],
  },
  {
    code: 'PLAN_PREMIUM',
    name: 'Student Pro',
    price: PUBLIC_PRICE_LABEL,
    desc: 'Unlock smarter guidance and opportunities',
    features: [
      'Advanced AI recommendations',
      'Personalized alerts (bursaries & careers)',
      'Enhanced search and insights',
    ],
    featured: true,
  },
];

const HOW_IT_WORKS_STEPS = [
  {
    title: '1. Build your profile',
    description: 'Students complete profile basics and consent once, then EduRite keeps progress across sessions.',
  },
  {
    title: '2. Take psychometric guidance',
    description: 'Assessment outcomes identify strengths and growth areas for smarter career and learning recommendations.',
  },
  {
    title: '3. Match opportunities',
    description: 'Discover careers, universities, and bursaries that align with profile readiness and interests.',
  },
  {
    title: '4. Improve and unlock',
    description: 'Use Learning Centre recommendations and points/rewards progress to keep improving over time.',
  },
] as const;

const TESTIMONIALS = [
  {
    quote: 'EduRite helped me narrow three realistic degree pathways and funding options in one week.',
    role: 'Student',
  },
  {
    quote: 'Our talent pipeline review is faster because candidate profiles and readiness signals are clearer.',
    role: 'Company partner',
  },
  {
    quote: 'Operational governance improved with role-based controls, approval workflows, and audit visibility.',
    role: 'Administrator',
  },
] as const;

const FAQ_ITEMS = [
  {
    question: 'Do students need to verify email before full access?',
    answer: 'Yes. New accounts remain pending until phone OTP verification succeeds.',
  },
  {
    question: 'Can EduRite recommend bursaries and universities together?',
    answer: 'Yes. Recommendations combine student profile data, psychometric outcomes, and opportunity context.',
  },
  {
    question: 'Is POPIA consent captured and versioned?',
    answer: 'Yes. Consent is mandatory at registration and stored with consent version and timestamp.',
  },
  {
    question: 'Can plans and payment providers change later?',
    answer: 'Yes. Pricing and payment integrations are abstraction-based to support future providers.',
  },
] as const;

const resolveRows = <T,>(
  data: T[] | PaginatedResponse<T> | undefined,
): T[] => {
  if (Array.isArray(data)) {
    return data;
  }

  if (data && 'content' in data && Array.isArray(data.content)) {
    return data.content;
  }

  return [];
};

const normalizeFilters = (filters: DiscoveryFilters): DiscoveryFilters => ({
  q: filters.q.trim(),
  location: filters.location.trim(),
  category: filters.category.trim(),
});

const PageIntro = ({ title, subtitle }: PageIntroProps) => (
  <div>
    <h1 className="text-3xl font-bold text-slate-900">{title}</h1>
    <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
  </div>
);

const DiscoveryHero = ({
  badge,
  title,
  subtitle,
  imageSrc,
  imagePositionClassName,
}: {
  badge: string;
  title: string;
  subtitle: string;
  imageSrc: string;
  imagePositionClassName?: string;
}) => (
  <BackgroundSection
    eyebrow={<Badge color="blue">{badge}</Badge>}
    title={title}
    description={subtitle}
    imageSrc={imageSrc}
    imagePositionClassName={imagePositionClassName}
    contentClassName="min-h-[220px] justify-end"
  />
);

const BulletList = ({ items }: { items: readonly string[] }) => (
  <ul className="space-y-3 text-sm leading-6 text-slate-700 sm:text-base">
    {items.map((item) => (
      <li key={item} className="flex gap-3">
        <span
          className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-primary-600"
          aria-hidden="true"
        />
        <span>{item}</span>
      </li>
    ))}
  </ul>
);

const FilterBar = ({ placeholder, locationLabel, categoryLabel, filters, onChange, onApply, isApplying }: FilterBarProps) => (
  <form
    className="card p-4"
    onSubmit={(event) => {
      event.preventDefault();
      onApply();
    }}
  >
    <div className="grid gap-3 md:grid-cols-4">
      <input
        className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        placeholder={placeholder}
        value={filters.q}
        onChange={(event) => onChange('q', event.target.value)}
      />
      <input
        className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        placeholder={locationLabel}
        value={filters.location}
        onChange={(event) => onChange('location', event.target.value)}
      />
      <input
        className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
        placeholder={categoryLabel}
        value={filters.category}
        onChange={(event) => onChange('category', event.target.value)}
      />
      <Button type="submit" disabled={isApplying}>
        {isApplying ? 'Applying...' : 'Apply filters'}
      </Button>
    </div>
  </form>
);

const DiscoveryInsightCard = ({ title, summary, aiUsed, highlights }: InsightCardProps) => {
  if (!summary) return null;
  return (
    <article className="rounded-2xl border border-sky-200 bg-sky-50/70 p-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold text-slate-900">{title}</h2>
        <Badge color={aiUsed ? 'blue' : 'slate'}>{aiUsed ? 'AI insight' : 'Live insight'}</Badge>
      </div>
      <p className="mt-2 text-sm text-slate-700">{summary}</p>
      {highlights && highlights.length > 0 ? (
        <ul className="mt-3 list-disc space-y-1 pl-5 text-xs text-slate-600">
          {highlights.slice(0, 3).map((item) => <li key={item}>{item}</li>)}
        </ul>
      ) : null}
    </article>
  );
};

const LiveDataStatusNotice = ({ visible, message }: { visible: boolean; message: string }) => (
  visible ? (
    <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
      {message}
    </div>
  ) : null
);

export const LandingPage = () => (
  <div className="space-y-8">
    <section className="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm lg:grid lg:grid-cols-[1.05fr_1fr] lg:gap-10">
      <div className="lg:col-span-2">
        <EduRiteLogo
          className="mx-auto h-auto w-[200px] object-contain sm:w-[240px] md:w-[280px] lg:w-[320px] xl:w-[360px]"
        />
      </div>
      <div className="pt-8 lg:pt-0">
        <Badge color="blue">Career intelligence for students and sponsors</Badge>
        <h1 className="mt-5 text-4xl font-bold leading-tight text-slate-900 md:text-5xl">
          Build brighter futures with smarter career and bursary matching.
        </h1>
        <p className="mt-5 max-w-2xl text-lg text-slate-600 md:text-xl">
          EduRite helps students discover pathways, helps companies invest in high-potential
          talent, and gives admins enterprise-grade oversight.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/auth/register/student">
            <Button className="rounded-xl px-6 py-3 text-base">Start as Student</Button>
          </Link>
          <Link to="/auth/register/company">
            <Button className="rounded-xl px-6 py-3 text-base">Hire &amp; Fund Talent</Button>
          </Link>
        </div>
      </div>
      <div className="mt-8 grid gap-4 lg:mt-0">
        <MetricCard
          title="Students matched"
          value="45,000+"
          subtitle="AI career fit and funding recommendations"
        />
        <MetricCard
          title="Bursary applications"
          value="120,000+"
          subtitle="Managed through the EduRite workflow"
        />
        <MetricCard
          title="Partner organizations"
          value="1,300+"
          subtitle="Companies, institutions, and nonprofits"
        />
      </div>
    </section>

    <section className="grid gap-4 md:grid-cols-3">
      {LANDING_HIGHLIGHTS.map((item) => (
        <article key={item.title} className="card p-5">
          <h3 className="font-semibold">{item.title}</h3>
          <p className="mt-2 text-sm text-slate-600">{item.description}</p>
        </article>
      ))}
    </section>

    <section className="grid gap-4 xl:grid-cols-3">
      {LANDING_BACKGROUND_PANELS.map((panel) => (
        <BackgroundSection
          key={panel.badge}
          eyebrow={<Badge color="blue">{panel.badge}</Badge>}
          title={panel.title}
          description={panel.description}
          imageSrc={panel.imageSrc}
          imagePositionClassName={panel.imagePositionClassName}
          contentClassName="min-h-[260px] justify-end"
        />
      ))}
    </section>

    <section className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="mb-6">
        <Badge color="blue">How it works</Badge>
        <h2 className="mt-3 text-2xl font-semibold tracking-tight text-slate-900">A guided journey from profile to opportunity</h2>
      </div>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {HOW_IT_WORKS_STEPS.map((step) => (
          <article key={step.title} className="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
            <h3 className="text-base font-semibold text-slate-900">{step.title}</h3>
            <p className="mt-2 text-sm text-slate-600">{step.description}</p>
          </article>
        ))}
      </div>
    </section>

    <section className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="mb-6">
        <Badge color="blue">Platform capabilities</Badge>
        <h2 className="mt-3 text-2xl font-semibold tracking-tight text-slate-900">Designed for outcomes, not just signups</h2>
      </div>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {LANDING_VALUE_SECTIONS.map((section) => (
          <article key={section.title} className="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
            <h3 className="text-base font-semibold text-slate-900">{section.title}</h3>
            <p className="mt-2 text-sm text-slate-600">{section.description}</p>
            <ul className="mt-4 space-y-2 text-sm text-slate-700">
              {section.bullets.map((item) => <li key={item}>• {item}</li>)}
            </ul>
          </article>
        ))}
      </div>
    </section>

    <section className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="space-y-3">
          <Badge color="blue">Student experience</Badge>
          <div>
            <h2 className="text-2xl font-semibold tracking-tight text-slate-900">
              Notifications &amp; Alerts
            </h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
              Stay informed with timely updates delivered directly through the platform and via
              email or SMS.
            </p>
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-5">
          <BulletList items={NOTIFICATION_FEATURES} />
          <p className="mt-4 border-t border-slate-200 pt-4 text-sm font-medium leading-6 text-slate-700 sm:text-base">
            Log out securely from your account whenever needed.
          </p>
        </div>
      </div>
    </section>

    <section className="grid gap-4 lg:grid-cols-2">
      <PlaceholderChart title="Platform growth snapshot" />
      <PlaceholderChart title="Application and conversion trends" />
    </section>

    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-slate-900">Subscriptions and pricing</h2>
          <p className="mt-2 text-sm text-slate-600">Choose a plan that matches your growth stage and guidance depth.</p>
        </div>
        <Link to="/pricing"><Button className="rounded-xl px-6 py-3 text-sm">View Pricing Plans</Button></Link>
      </div>
    </section>

    <section className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="mb-6">
        <Badge color="blue">Testimonials</Badge>
        <h2 className="mt-3 text-2xl font-semibold tracking-tight text-slate-900">What platform users say</h2>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {TESTIMONIALS.map((item) => (
          <article key={item.role} className="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
            <p className="text-sm text-slate-700">&quot;{item.quote}&quot;</p>
            <p className="mt-3 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{item.role}</p>
          </article>
        ))}
      </div>
    </section>

    <section className="rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="mb-6">
        <Badge color="blue">FAQ</Badge>
        <h2 className="mt-3 text-2xl font-semibold tracking-tight text-slate-900">Common questions</h2>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        {FAQ_ITEMS.map((item) => (
          <article key={item.question} className="rounded-2xl border border-slate-200 bg-slate-50/70 p-5">
            <h3 className="text-base font-semibold text-slate-900">{item.question}</h3>
            <p className="mt-2 text-sm text-slate-600">{item.answer}</p>
          </article>
        ))}
      </div>
      <div className="mt-6 flex flex-wrap gap-3">
        <Link to="/auth/register/student"><Button className="rounded-xl px-6 py-3 text-sm">Create Student Account</Button></Link>
        <Link to="/auth/register/company"><Button className="rounded-xl px-6 py-3 text-sm">Create Company Account</Button></Link>
      </div>
    </section>
  </div>
);

export const AboutPage = () => (
  <section className="space-y-6">
    <PageIntro
      title="About EduRite"
      subtitle="A smarter, more connected pathway from education to opportunity."
    />
    <div className="mx-auto max-w-4xl rounded-[28px] border border-slate-200 bg-white p-6 shadow-sm sm:p-8 lg:p-10">
      <div className="max-w-3xl space-y-5 text-sm leading-7 text-slate-600 sm:text-base">
        <h2 className="text-2xl font-semibold tracking-tight text-slate-900">About EduRite</h2>
        <p>
          EduRite is a smart education and career platform designed to expand access to
          opportunities for students, institutions, and organizations.
        </p>
        <p>
          We leverage AI-powered recommendations to guide students toward the right careers,
          courses, and bursaries based on their skills, interests, and academic background.
        </p>
        <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-5">
          <h3 className="text-lg font-semibold text-slate-900">Human-centered workflows</h3>
          <p className="mt-2">
            At the same time, EduRite integrates human-centered workflows that allow:
          </p>
          <div className="mt-4">
            <BulletList items={HUMAN_CENTERED_WORKFLOWS} />
          </div>
        </div>
        <p>
          Our mission is to bridge the gap between education and employment, ensuring that every
          learner has a clear, data-driven pathway to success.
        </p>
      </div>
    </div>
  </section>
);

export const CareersPage = () => {
  const [filters, setFilters] = useState<DiscoveryFilters>({ q: '', location: '', category: '' });
  const [appliedFilters, setAppliedFilters] = useState<DiscoveryFilters>({ q: '', location: '', category: '' });
  const careers = useAppQuery<Career[] | PaginatedResponse<Career>>({
    queryKey: ['public', 'careers', appliedFilters],
    queryFn: () => careerService.list({
      q: appliedFilters.q,
      location: appliedFilters.location,
      industry: appliedFilters.category,
      page: 0,
      size: 25,
    }),
  });
  const rows = resolveRows(careers.data);
  const insight = useAppQuery({
    queryKey: ['public', 'careers', 'insight', appliedFilters],
    enabled: !careers.isLoading && !careers.isError && rows.length > 0,
    queryFn: () => publicDiscoveryService.careersInsight({
      q: appliedFilters.q,
      location: appliedFilters.location,
      industry: appliedFilters.category,
      top: 5,
    }),
  });

  return (
    <section className="space-y-6">
      <DiscoveryHero
        badge="Careers"
        title="Career Listings"
        subtitle="Explore in-demand careers, role expectations, and growth potential."
        imageSrc={careersBackgroundImage}
        imagePositionClassName="object-center"
      />
      <FilterBar
        placeholder="Search careers"
        locationLabel="Location"
        categoryLabel="Industry"
        filters={filters}
        onChange={(key, value) => setFilters((prev) => ({ ...prev, [key]: value }))}
        onApply={() => setAppliedFilters(normalizeFilters(filters))}
        isApplying={careers.isFetching}
      />
      <DiscoveryInsightCard
        title="Career discovery insight"
        summary={insight.data?.summary}
        aiUsed={insight.data?.aiUsed}
        highlights={insight.data?.highlights}
      />
      {careers.isLoading ? <LoadingState message="Loading careers..." /> : null}
      {careers.isError ? <ErrorState message="We could not load live career data right now. Please try again shortly." /> : null}
      {!careers.isLoading && !careers.isError && rows.length === 0 ? (
        <EmptyState
          title="No careers available"
          message="No careers match your filters yet. Try broadening your search criteria."
        />
      ) : null}
      {!careers.isLoading && !careers.isError && rows.length > 0 ? (
        <DataTable
          columns={[
            { key: 'title', header: 'Career' },
            { key: 'description', header: 'Overview' },
            { key: 'industry', header: 'Industry' },
            { key: 'location', header: 'Location' },
            { key: 'qualificationLevel', header: 'Qualification' },
            {
              key: 'matchScore',
              header: 'Match score',
              render: (row) => <Badge color="blue">{typeof row.matchScore === 'number' ? `${row.matchScore}%` : 'N/A'}</Badge>,
            },
          ]}
          data={rows}
        />
      ) : null}
    </section>
  );
};

export const CareerDetailsPage = () => {
  const { id = '' } = useParams();

  useAppQuery({
    queryKey: ['public', 'career', id],
    queryFn: () => careerService.details(id),
    enabled: Boolean(id),
  });

  return (
    <section className="space-y-4">
      <PageIntro
        title="Career Details"
        subtitle={`Deep dive into required skills, qualifications, and opportunities for career #${id}.`}
      />
      <div className="card p-6 text-sm text-slate-600">
        Expected salary bands, learning roadmap, and top institutions are shown in this detail
        view.
      </div>
    </section>
  );
};

export const CoursesPage = () => {
  const [filters, setFilters] = useState<DiscoveryFilters>({ q: '', location: '', category: '' });
  const [appliedFilters, setAppliedFilters] = useState<DiscoveryFilters>({ q: '', location: '', category: '' });
  const courses = useAppQuery<Course[] | PaginatedResponse<Course>>({
    queryKey: ['public', 'courses', appliedFilters],
    queryFn: () => courseService.list({
      q: appliedFilters.q,
      location: appliedFilters.location,
      level: appliedFilters.category,
      page: 0,
      size: 25,
    }),
  });
  const rows = resolveRows(courses.data);
  const insight = useAppQuery({
    queryKey: ['public', 'courses', 'insight', appliedFilters],
    enabled: !courses.isLoading && !courses.isError && rows.length > 0,
    queryFn: () => publicDiscoveryService.coursesInsight({
      q: appliedFilters.q,
      location: appliedFilters.location,
      level: appliedFilters.category,
      top: 5,
    }),
  });

  return (
    <section className="space-y-6">
      <DiscoveryHero
        badge="Courses"
        title="Courses"
        subtitle="Compare accredited courses aligned with your career ambitions."
        imageSrc={coursesBackgroundImage}
        imagePositionClassName="object-[center_35%]"
      />
      <FilterBar
        placeholder="Search courses"
        locationLabel="Institution / location"
        categoryLabel="Level"
        filters={filters}
        onChange={(key, value) => setFilters((prev) => ({ ...prev, [key]: value }))}
        onApply={() => setAppliedFilters(normalizeFilters(filters))}
        isApplying={courses.isFetching}
      />
      <DiscoveryInsightCard
        title="Course alignment insight"
        summary={insight.data?.summary}
        aiUsed={insight.data?.aiUsed}
        highlights={insight.data?.highlights}
      />
      {courses.isLoading ? <LoadingState message="Loading courses..." /> : null}
      {courses.isError ? <ErrorState message="We could not load live course data right now. Please try again shortly." /> : null}
      {!courses.isLoading && !courses.isError && rows.length === 0 ? (
        <EmptyState
          title="No courses available"
          message="No courses match your filters yet. Try broadening your search criteria."
        />
      ) : null}
      {!courses.isLoading && !courses.isError && rows.length > 0 ? (
        <DataTable
          columns={[
            { key: 'name', header: 'Course' },
            { key: 'institutionName', header: 'Institution' },
            { key: 'level', header: 'Level' },
            { key: 'duration', header: 'Duration' },
            {
              key: 'matchScore',
              header: 'Match score',
              render: (row) => <Badge color="blue">{typeof row.matchScore === 'number' ? `${row.matchScore}%` : 'N/A'}</Badge>,
            },
          ]}
          data={rows}
        />
      ) : null}
    </section>
  );
};

export const CourseDetailsPage = () => {
  const { id = '' } = useParams();

  useAppQuery({
    queryKey: ['public', 'course', id],
    queryFn: () => courseService.details(id),
    enabled: Boolean(id),
  });

  return (
    <section className="space-y-4">
      <PageIntro
        title="Course Details"
        subtitle={`Program outline, admission criteria, and graduate outcomes for course #${id}.`}
      />
      <div className="card p-6 text-sm text-slate-600">
        Review modules by year, tuition estimate, and application timeline milestones.
      </div>
    </section>
  );
};

export const InstitutionsPage = () => {
  const [filters, setFilters] = useState<DiscoveryFilters>({ q: '', location: '', category: '' });
  const [appliedFilters, setAppliedFilters] = useState<DiscoveryFilters>({ q: '', location: '', category: '' });
  const institutions = useAppQuery<Institution[]>({
    queryKey: ['public', 'institutions', appliedFilters],
    queryFn: () => institutionService.list({
      q: [appliedFilters.q, appliedFilters.location, appliedFilters.category].filter(Boolean).join(' ').trim(),
    }),
  });
  const rows = resolveRows(institutions.data);

  return (
    <section className="space-y-6">
      <PageIntro
        title="Institutions"
        subtitle="Discover universities and colleges that best match your profile and goals."
      />
      <FilterBar
        placeholder="Search institutions"
        locationLabel="Location"
        categoryLabel="Category"
        filters={filters}
        onChange={(key, value) => setFilters((prev) => ({ ...prev, [key]: value }))}
        onApply={() => setAppliedFilters(normalizeFilters(filters))}
        isApplying={institutions.isFetching}
      />
      <LiveDataStatusNotice visible={institutions.isError} message="Live institution data is currently unavailable." />
      {!institutions.isLoading && !institutions.isError && rows.length === 0 ? (
        <EmptyState
          title="No institutions available"
          message="No institutions match your filters yet. Try broadening your search criteria."
        />
      ) : null}
      {rows.length > 0 ? (
        <DataTable
          columns={[
            { key: 'name', header: 'Institution' },
            { key: 'location', header: 'Location' },
          ]}
          data={rows}
        />
      ) : null}
    </section>
  );
};

export const InstitutionDetailsPage = () => {
  const { id = '' } = useParams();

  useAppQuery({
    queryKey: ['public', 'institution', id],
    queryFn: () => institutionService.details(id),
    enabled: Boolean(id),
  });

  return (
    <section className="space-y-4">
      <PageIntro
        title="Institution Details"
        subtitle={`Campus profile, programs, and admission windows for institution #${id}.`}
      />
      <div className="card p-6 text-sm text-slate-600">
        View ranking indicators, supported bursaries, and location insights.
      </div>
    </section>
  );
};

export const BursariesPage = () => {
  const [filters, setFilters] = useState<DiscoveryFilters>({ q: '', location: '', category: '' });
  const [appliedFilters, setAppliedFilters] = useState<DiscoveryFilters>({ q: '', location: '', category: '' });
  const bursaries = useAppQuery({
    queryKey: ['public', 'bursaries', 'search', appliedFilters],
    queryFn: () => bursaryService.search({
      q: appliedFilters.q,
      region: appliedFilters.location,
      qualification: appliedFilters.category,
      page: 0,
      size: 25,
    }),
  });
  const rows = (bursaries.data?.items ?? []).map((item, index) => ({
    ...item,
    id: item.externalId || `${item.sourceType}-${index}-${item.title}`,
  })) as Array<AggregatedBursary & { id: string }>;
  const insight = useAppQuery({
    queryKey: ['public', 'bursaries', 'insight', appliedFilters],
    enabled: !bursaries.isLoading && !bursaries.isError && rows.length > 0,
    queryFn: () => publicDiscoveryService.bursariesInsight({
      q: appliedFilters.q,
      region: appliedFilters.location,
      qualification: appliedFilters.category,
      top: 5,
    }),
  });

  return (
    <section className="space-y-6">
      <DiscoveryHero
        badge="Bursaries"
        title="Bursaries"
        subtitle="Browse funding opportunities by field, location, and eligibility criteria."
        imageSrc={bursariesBackgroundImage}
        imagePositionClassName="object-[center_60%]"
      />
      <FilterBar
        placeholder="Search bursaries"
        locationLabel="Region"
        categoryLabel="Qualification"
        filters={filters}
        onChange={(key, value) => setFilters((prev) => ({ ...prev, [key]: value }))}
        onApply={() => setAppliedFilters(normalizeFilters(filters))}
        isApplying={bursaries.isFetching}
      />
      <DiscoveryInsightCard
        title="Bursary readiness insight"
        summary={insight.data?.summary}
        aiUsed={insight.data?.aiUsed}
        highlights={insight.data?.highlights}
      />
      {bursaries.isLoading ? <LoadingState message="Loading bursaries..." /> : null}
      {bursaries.isError ? <ErrorState message="We could not load live bursary data right now. Please try again shortly." /> : null}
      {!bursaries.isLoading && !bursaries.isError && rows.length === 0 ? (
        <EmptyState
          title="No bursaries available"
          message="No bursaries match your filters yet. Try broadening your search criteria."
        />
      ) : null}
      {!bursaries.isLoading && !bursaries.isError && rows.length > 0 ? (
        <DataTable
          columns={[
            { key: 'title', header: 'Bursary' },
            { key: 'provider', header: 'Provider' },
            { key: 'qualificationLevel', header: 'Qualification' },
            { key: 'region', header: 'Region' },
            {
              key: 'sourceType',
              header: 'Source',
              render: (row) => (
                <Badge color={row.sourceType === 'OFFICIAL_PROVIDER' ? 'emerald' : 'amber'}>{row.sourceType === 'OFFICIAL_PROVIDER' ? 'Official' : 'Ranked'}</Badge>
              ),
            },
            {
              key: 'relevanceScore',
              header: 'Match score',
              render: (row) => <Badge color="blue">{row.relevanceScore}%</Badge>,
            },
          ]}
          data={rows}
        />
      ) : null}
    </section>
  );
};

export const BursaryDetailsPage = () => {
  const { id = '' } = useParams();

  useAppQuery({
    queryKey: ['public', 'bursary', id],
    queryFn: () => bursaryService.details(id),
    enabled: Boolean(id),
  });

  return (
    <section className="space-y-4">
      <PageIntro
        title="Bursary Details"
        subtitle={`Eligibility requirements, benefits, and process details for bursary #${id}.`}
      />
      <div className="card p-6 text-sm text-slate-600">
        Prepare your supporting documents and track all key submission dates.
      </div>
    </section>
  );
};

export const PricingPage = () => {
  const plansQuery = useAppQuery({
    queryKey: ['public', 'pricing-plans'],
    queryFn: () => subscriptionService.plans(),
  });

  const plans: PricingPlan[] = (plansQuery.data ?? []).map((plan) => ({
    code: plan.code,
    name: plan.name,
    price: Number(plan.price ?? plan.amount) > 0 ? PUBLIC_PRICE_LABEL : formatPlanPrice(Number(plan.price ?? plan.amount), plan.currency, plan.billingPeriod ?? plan.billingInterval),
    desc: plan.description ?? '',
    features: plan.features,
    featured: plan.recommended ?? plan.premium,
  }));

  const viewPlans = plans.length ? plans : pricingPlans;

  return (
    <section className="space-y-6">
      <PageIntro
        title="Pricing Plans"
        subtitle="Simple options for students and organizations looking to grow with EduRite."
      />
      <p className="text-sm text-slate-600">Premium checkout supports PayPal and PayFast once you sign in as a student.</p>
      <LiveDataStatusNotice visible={plansQuery.isError} message="Live pricing could not be loaded. Showing standard plans." />
      <div className="grid gap-5 md:grid-cols-3">
        {viewPlans.map((plan) => (
        <article
          key={plan.code ?? plan.name}
          className={`flex h-full flex-col rounded-[28px] border p-6 shadow-sm transition-transform duration-200 sm:p-7 ${
            plan.featured
              ? 'border-primary-200 bg-gradient-to-b from-primary-50 via-white to-white shadow-lg shadow-primary-200/40'
              : 'border-slate-200 bg-white hover:-translate-y-0.5'
          }`}
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">{plan.name}</h2>
              <p className="mt-3 text-3xl font-bold tracking-tight text-slate-900">{plan.price}</p>
            </div>
            {plan.featured ? (
              <span className="rounded-full bg-primary-600 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-white">
                Recommended
              </span>
            ) : null}
          </div>
          <p className="mt-4 text-sm leading-6 text-slate-600">{plan.desc}</p>
          <ul className="mt-6 space-y-3 text-sm leading-6 text-slate-700">
            {plan.features.map((feature) => (
              <li key={feature} className="flex gap-3">
                <span
                  className={`mt-1 inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                    plan.featured
                      ? 'bg-primary-100 text-primary-700'
                      : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  ✓
                </span>
                <span>{feature}</span>
              </li>
            ))}
          </ul>
          <Link to="/auth/login">
            <Button
              type="button"
              className={`mt-8 w-full rounded-2xl px-5 py-3 text-sm ${
                plan.featured ? 'shadow-lg shadow-primary-600/20' : ''
              }`}
            >
              Sign in to choose plan
            </Button>
          </Link>
        </article>
        ))}
      </div>
    </section>
  );
};
