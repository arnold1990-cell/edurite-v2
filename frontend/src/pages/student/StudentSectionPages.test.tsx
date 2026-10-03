import { describe, expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server';
import { StudentProfileSectionsPage, StudentCareerExplorerPage, StudentLearningResourcesPage, StudentFundingPage, StudentInstitutionsPage, StudentProgressPage } from './StudentSectionPages';

vi.mock('@/features/subscriptions/access', () => ({ AccessBadge: () => null }));

vi.mock('./StudentPages', () => Object.fromEntries([
  'StudentProfilePage', 'StudentMySchoolPage', 'StudentCareerRecommendationsPage', 'StudentPsychometricPage',
  'StudentLearningCentrePage', 'StudentSavedPage', 'StudentApplicationsPage', 'StudentBursaryRecommendationsPage',
  'StudentRewardsPage', 'StudentNotificationsPage',
].map(name => [name, (props: Record<string, unknown>) => <div data-feature={name} data-options={JSON.stringify(props)} />])));
vi.mock('./StudentFeaturePages', () => Object.fromEntries([
  'StudentCvBuilderPage', 'StudentAiTutorPage', 'StudentScholarshipAssistantPage', 'StudentUniversityApplicationsPage', 'StudentCareerRoadmapsPage',
].map(name => [name, (props: Record<string, unknown>) => <div data-feature={name} data-options={JSON.stringify(props)} />])));
vi.mock('./StudentUniversitiesPage', () => ({ StudentUniversitiesPage: () => <div data-feature="universities" /> }));
vi.mock('./StudentCollegesTvetsPage', () => ({ StudentCollegesTvetsPage: () => <div data-feature="colleges" /> }));
vi.mock('@/components/student/explore/ExploreDirectory', () => ({ ExploreCourses: () => <div data-feature="programmes" />, ExploreInstitutions: () => <div data-feature="institutions" /> }));
vi.mock('@/components/student/career/ExploreResources', () => ({ ExploreOpportunities: () => <div data-feature="opportunities" />, ExploreStudyOptions: () => <div data-feature="study-options" /> }));
vi.mock('@/hooks/useAppQuery', () => ({ useAppQuery: () => ({ data: { cards: [{ key: 'learning', label: 'Learning', percentage: 60, recommendation: 'Keep studying' }] } }) }));

const cases = [
  [StudentProfileSectionsPage, 'profile', 'cv', 'StudentCvBuilderPage'],
  [StudentProfileSectionsPage, 'profile', 'school', 'StudentMySchoolPage'],
  ...['overview', 'academic', 'interests', 'career', 'documents'].map(section => [StudentProfileSectionsPage, 'profile', section, 'StudentProfilePage'] as const),
  [StudentCareerExplorerPage, 'career-explorer', 'guidance', 'StudentCareerRecommendationsPage'],
  ...['career-match', 'interests'].map(section => [StudentCareerExplorerPage, 'career-explorer', section, 'StudentPsychometricPage'] as const),
  ...['discover', 'career-path', 'learning-path', 'readiness', 'saved'].map(section => [StudentCareerExplorerPage, 'career-explorer', section, 'StudentCareerRoadmapsPage'] as const),
  [StudentLearningResourcesPage, 'learning', 'tutor', 'StudentAiTutorPage'],
  [StudentLearningResourcesPage, 'learning', 'guidance', 'StudentCareerRecommendationsPage'],
  ...['centre', 'resources'].map(section => [StudentLearningResourcesPage, 'learning', section, 'StudentLearningCentrePage'] as const),
  [StudentLearningResourcesPage, 'learning', 'study-plan', 'StudentCareerRoadmapsPage'],
  [StudentFundingPage, 'funding', 'opportunities', 'StudentSavedPage'],
  [StudentFundingPage, 'funding', 'bursaries', 'StudentApplicationsPage'],
  [StudentFundingPage, 'funding', 'applications', 'StudentApplicationsPage'],
  [StudentFundingPage, 'funding', 'scholarships', 'StudentScholarshipAssistantPage'],
  [StudentFundingPage, 'funding', 'matches', 'StudentBursaryRecommendationsPage'],
  [StudentFundingPage, 'funding', 'saved', 'StudentApplicationsPage'],
  [StudentInstitutionsPage, 'institutions', 'universities', 'universities'],
  [StudentInstitutionsPage, 'institutions', 'colleges-tvets', 'colleges'],
  [StudentInstitutionsPage, 'institutions', 'programmes', 'programmes'],
  ...['applications', 'saved'].map(section => [StudentInstitutionsPage, 'institutions', section, 'StudentUniversityApplicationsPage'] as const),
  ...['rewards', 'achievements'].map(section => [StudentProgressPage, 'progress', section, 'StudentRewardsPage'] as const),
  [StudentProgressPage, 'progress', 'academic', 'StudentProfilePage'],
] as const;

describe('consolidated feature content', () => {
  it.each(cases)('mounts the retained feature for %s / %s / %s', (Page, parent, section, feature) => {
    const html = renderToStaticMarkup(<StaticRouter location={`/student/${parent}?section=${section}`}><Page /></StaticRouter>);
    expect(html).toContain(`data-feature="${feature}"`);
    expect(html).not.toContain('Coming soon');
    if (parent === 'funding' && section === 'applications') expect(html).toContain('applicationsOnly');
    if (section === 'saved' && parent !== 'career-explorer') expect(html).toContain('savedOnly');
  });
  it('keeps all learning tools discoverable from the overview', () => {
    const html = renderToStaticMarkup(<StaticRouter location="/student/learning"><StudentLearningResourcesPage /></StaticRouter>);
    for (const section of ['tutor', 'guidance', 'centre', 'study-plan', 'resources']) expect(html).toContain(`learning?section=${section}`);
  });
});
