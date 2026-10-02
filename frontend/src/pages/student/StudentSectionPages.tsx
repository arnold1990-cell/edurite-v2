import { AccessBadge } from '@/features/subscriptions/access';
import { StudentProfilePage, StudentMySchoolPage, StudentCareerRecommendationsPage, StudentPsychometricPage, StudentLearningCentrePage, StudentSavedPage, StudentApplicationsPage, StudentBursaryRecommendationsPage, StudentRewardsPage } from './StudentPages';
import { StudentCvBuilderPage, StudentAiTutorPage, StudentScholarshipAssistantPage, StudentUniversityApplicationsPage, StudentCareerRoadmapsPage } from './StudentFeaturePages';
import { StudentUniversitiesPage } from './StudentUniversitiesPage';
import { StudentCollegesTvetsPage } from './StudentCollegesTvetsPage';
import { ExploreCourses } from '@/components/student/explore/ExploreDirectory';
import { studentSections } from '@/components/student/StudentSectionNavigation';
import { ExploreInstitutions } from '@/components/student/explore/ExploreDirectory';
import { Link, useSearchParams } from 'react-router-dom';
import { ExploreOpportunities, ExploreStudyOptions } from '@/components/student/career/ExploreResources';
import { StudentNotificationsPage } from './StudentPages';
import { useAppQuery } from '@/hooks/useAppQuery';
import { featureModulesService } from '@/services/featureModulesService';
export function ProgressOverview({ learningOnly = false }: { learningOnly?: boolean }) {
 const progress = useAppQuery({ queryKey: ['progress-score'], queryFn: featureModulesService.progressScore });
 return <section><h2 className="text-xl font-bold mb-4">Your progress</h2>{progress.isLoading && <p role="status">Loading progress...</p>}{progress.isError && <p role="alert">Progress could not load. <button onClick={() => progress.refetch()}>Retry</button></p>}<div className="grid gap-4 md:grid-cols-2">{progress.data?.cards.filter(card => !learningOnly || card.key === 'learning').map(card => <article key={card.key} className="card p-4"><h3>{card.label}</h3><progress max={100} value={card.percentage} aria-label={card.label} /><p>{card.percentage}%</p><p>{card.recommendation}</p></article>)}</div><Link to="/student/progress?section=rewards">View achievements and rewards</Link></section>;
}
export function FundingOverview() { const [params] = useSearchParams(); return <section className="ex-workspace"><ExploreOpportunities query={params.get('q') || ''} fundingOnly expanded /><ExploreOpportunities query={params.get('q') || ''} fundingOnly={false} expanded /></section>; }
export function StudentStudyOptionsPage() { const [params] = useSearchParams(); return <section className="ex-workspace student-page-section"><h1 className="student-page-title">Study Options</h1><ExploreStudyOptions roadmap={null} query={params.get('q') || ''} expanded onExpand={() => {}} /></section>; }
export function StudentMessagesPage() { return <section><h1 className="text-2xl font-bold">Messages</h1><p className="mb-4">Account updates and notifications appear below.</p><StudentNotificationsPage /></section>; }
export function StudentNotFoundPage() { return <section><h1 className="text-2xl font-bold">Page not found</h1><Link to="/student/dashboard">Return to Dashboard</Link></section>; }
export function InstitutionsOverview() { const [params] = useSearchParams(); return <section className="ex-workspace student-page-section"><ExploreInstitutions query={params.get('q') || ''} /></section>; }

function useSection(fallback: string) { const [params] = useSearchParams(); return params.get('section') || fallback; }
function ModuleOverview({ module }: { module: string }) {
  const group = studentSections.find(item => item.to === `/student/${module}`)!;
  return <div className="ed-feature-grid">{group.links.slice(1).map(([label, path]) => <Link className="card p-5" key={path} to={`/student/${path}`}><h2 className="font-semibold">{label}<AccessBadge to={`/student/${path}`} /></h2><span>Open {label.toLowerCase()} →</span></Link>)}</div>;
}
export function StudentProfileSectionsPage() {
  const section = useSection('overview');
  if (section === 'cv') return <StudentCvBuilderPage />;
  if (section === 'school') return <StudentMySchoolPage />;
  return <StudentProfilePage />;
}
export function StudentCareerExplorerPage() {
  const section = useSection('discover');
  if (section === 'guidance') return <StudentCareerRecommendationsPage />;
  if (section === 'career-match' || section === 'interests') return <StudentPsychometricPage />;
  return <StudentCareerRoadmapsPage />;
}
export function StudentLearningResourcesPage() {
  const section = useSection('overview');
  if (section === 'tutor') return <StudentAiTutorPage />;
  if (section === 'guidance') return <StudentCareerRecommendationsPage />;
  if (section === 'centre' || section === 'resources') return <StudentLearningCentrePage />;
  if (section === 'study-plan') return <StudentCareerRoadmapsPage />;
  return <ModuleOverview module="learning" />;
}
export function StudentFundingPage() {
  const section = useSection('discover');
  if (section === 'opportunities') return <StudentSavedPage />;
  if (section === 'bursaries') return <StudentApplicationsPage />;
  if (section === 'applications') return <StudentApplicationsPage applicationsOnly />;
  if (section === 'scholarships') return <StudentScholarshipAssistantPage />;
  if (section === 'matches') return <StudentBursaryRecommendationsPage />;
  if (section === 'saved') return <><ExploreOpportunities query="" fundingOnly={false} expanded savedOnly /><StudentApplicationsPage savedOnly /></>;
  return <FundingOverview />;
}
export function StudentInstitutionsPage() {
  const section = useSection('explore');
  const [params] = useSearchParams();
  if (section === 'universities') return <StudentUniversitiesPage />;
  if (section === 'colleges-tvets') return <StudentCollegesTvetsPage />;
  if (section === 'applications') return <StudentUniversityApplicationsPage />;
  if (section === 'saved') return <><p className="mb-4">Your saved draft applications. Add an institution and programme as a draft to keep it on your shortlist.</p><StudentUniversityApplicationsPage savedOnly /></>;
  if (section === 'programmes') return <ExploreCourses query={params.get('q') || ''} />;
  return <InstitutionsOverview />;
}
export function StudentProgressPage() {
  const section = useSection('overview');
  if (section === 'rewards' || section === 'achievements') return <StudentRewardsPage />;
  if (section === 'academic') return <StudentProfilePage />;
  return <ProgressOverview learningOnly={section === 'learning'} />;
}
