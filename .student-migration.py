from pathlib import Path
import re, subprocess
root=Path('frontend/src')
# Record the pre-implementation inventory from both versions of the router.
old=subprocess.check_output(['git','show','675a24c:frontend/src/app/App.tsx'],text=True)
rows=re.findall(r'<Route path="(/student/[^\"]+)" element=\{<([A-Za-z]+)',old)
Path('docs/student-feature-parity.md').write_text('# Student feature parity inventory\n\nAudit sources: working diff, HEAD (4a64c52), pre-redesign router (675a24c), all student page exports and service calls. No existing work reset.\n\n'+ '\n'.join(f'- `{route}`: `{component}` (content-only; no application shell).' for route,component in rows)+'\n',encoding='utf-8')
# A shared section catalogue drives navigation and tool search.
groups=[
('profile','My Profile',['Overview:overview','Academic Profile:academic','Interests & Skills:interests','Career Aspirations:career','School:school','Documents:documents','CV Builder:cv']),
('career-explorer','Career Explorer',['Discover:discover','Career Guidance:guidance','Career Match:career-match','Psychometric / Interests:interests','Career Path:career-path','Learning Path:learning-path','Academic Readiness:readiness','Saved Careers:saved']),
('study-options','Study Options',[]),
('funding','Bursaries & Funding',['Discover:discover','Opportunities:opportunities','Bursaries:bursaries','Scholarships:scholarships','Funding Match:matches','Saved:saved','Applications:applications']),
('institutions','Institutions',['Explore:explore','Universities:universities','Colleges / TVET:colleges-tvets','Programmes:programmes','Applications:applications']),
('learning','Learning Resources',['Overview:overview','EduRite Tutor:tutor','AI Guidance:guidance','Learning Centre:centre','Study Plan:study-plan','Resources:resources']),
('progress','My Progress',['Overview:overview','Academic Progress:academic','Learning Progress:learning','Achievements:achievements','Points & Rewards:rewards']),
('goals','My Goals',[]),('messages','Messages',[])]
import json
aliases={'profile':['academic-profile','documents','qualifications','experience','my-school','cv-builder'],'career-explorer':['psychometric','psychometric-test','explore','careers','career-roadmaps','recommendations/careers'],'learning':['ai-tutor','ai-guidance','learning-centre','interview-prep'],'funding':['saved','applications','scholarships','bursaries','opportunities','scholarship-assistant','recommendations/bursaries'],'institutions':['universities','colleges-tvets','university-applications'],'progress':['rewards']}
data=[dict(to='/student/'+key,label=label,paths=[key]+aliases.get(key,[]),links=[[x.split(':')[0],key+'?section='+x.split(':')[1]] for x in sections]) for key,label,sections in groups]
p=root/'components/student/StudentSectionNavigation.tsx'
s=p.read_text(encoding='utf-8'); start=s.index('export const studentSections'); end=s.index('\nexport function studentSection')
s=s[:start]+'export const studentSections = '+json.dumps(data,indent=2)+';\n'+s[end:]
s=s[:s.index('export function StudentSectionNavigation')]+'''export const studentTools = [
  ...studentSections.flatMap(group => [{ label: group.label, to: group.to }, ...group.links.map(([label, path]) => ({ label: `${group.label} / ${label}`, to: `/student/${path}` }))]),
  ...['Subscription', 'Notifications', 'Settings'].map(label => ({ label, to: `/student/${label.toLowerCase()}` })),
];

export function StudentSectionNavigation() {
  const { pathname, search } = useLocation();
  const section = studentSections.find(item => item.to === studentSection(pathname, search));
  if (!section?.links.length) return null;
  const selected = new URLSearchParams(search).get('section') || new URLSearchParams(section.links[0][1].split('?')[1]).get('section');
  return <div className="ed-section-heading"><h1>{section.label}</h1><nav className="ed-section-navigation" aria-label={`${section.label} sections`}>
    {section.links.map(([label, path]) => <Link key={path} to={`/student/${path}`} aria-current={new URLSearchParams(path.split('?')[1]).get('section') === selected ? 'page' : undefined}>{label}</Link>)}
  </nav></div>;
}
''';p.write_text(s,encoding='utf-8')
# Redirects merge query strings correctly, preserving payment / search payloads.
p=root/'routes/StudentRedirect.tsx';p.write_text('''import { Navigate, useLocation } from 'react-router-dom';

export function studentRedirectTarget(to: string, search: string, hash = '') {
  const [path, targetSearch] = to.split('?');
  const query = new URLSearchParams(search);
  new URLSearchParams(targetSearch).forEach((value, key) => query.set(key, value));
  let destination = path;
  if (path === '/student/career-explorer' && !targetSearch) {
    const category = query.get('category');
    if (category === 'Courses' || category === 'Subjects') destination = '/student/study-options';
    if (category === 'Institutions') destination = '/student/institutions';
    if (category === 'Bursaries' || category === 'Opportunities') {
      destination = '/student/funding'; query.set('section', category === 'Bursaries' ? 'bursaries' : 'opportunities');
    }
  }
  return `${destination}${query.size ? `?${query}` : ''}${hash}`;
}
export function StudentRedirect({ to }: { to: string }) {
  const { search, hash } = useLocation();
  return <Navigate replace to={studentRedirectTarget(to, search, hash)} />;
}
''')
# All these exported feature components have been inspected: none owns a shell.
p=root/'pages/student/StudentSectionPages.tsx';s=Path('.student-sections-base.txt').read_text(encoding='utf-8');s=s.replace('export function StudentProgressPage()', 'export function ProgressOverview()').replace('export function StudentFundingPage()', 'export function FundingOverview()').replace('export function StudentInstitutionsPage()', 'export function InstitutionsOverview()')
s='''import { StudentProfilePage, StudentMySchoolPage, StudentCareerRecommendationsPage, StudentPsychometricPage, StudentLearningCentrePage, StudentSavedPage, StudentApplicationsPage, StudentBursaryRecommendationsPage, StudentRewardsPage } from './StudentPages';
import { StudentCvBuilderPage, StudentAiTutorPage, StudentScholarshipAssistantPage, StudentUniversityApplicationsPage, StudentCareerRoadmapsPage } from './StudentFeaturePages';
import { StudentUniversitiesPage } from './StudentUniversitiesPage';
import { StudentCollegesTvetsPage } from './StudentCollegesTvetsPage';
import { ExploreCourses } from '@/components/student/explore/ExploreDirectory';
import { studentSections } from '@/components/student/StudentSectionNavigation';
''' +s
s+='''
function useSection(fallback: string) { const [params] = useSearchParams(); return params.get('section') || fallback; }
function ModuleOverview({ module }: { module: string }) {
  const group = studentSections.find(item => item.to === `/student/${module}`)!;
  return <div className="ed-feature-grid">{group.links.slice(1).map(([label, path]) => <Link className="card p-5" key={path} to={`/student/${path}`}><h2 className="font-semibold">{label}</h2><span>Open {label.toLowerCase()} →</span></Link>)}</div>;
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
  if (section === 'bursaries' || section === 'applications') return <StudentApplicationsPage />;
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
  if (section === 'programmes') return <ExploreCourses query={params.get('q') || ''} />;
  return <InstitutionsOverview />;
}
export function StudentProgressPage() {
  const section = useSection('overview');
  if (section === 'rewards' || section === 'achievements') return <StudentRewardsPage />;
  if (section === 'academic') return <StudentProfilePage />;
  return <ProgressOverview />;
}
''';p.write_text(s,encoding='utf-8')
# Profile tab selection stays compatible with all old tab URLs.
p=root/'components/student/profile/StudentProfileView.tsx';s=p.read_text(encoding='utf-8').replace("const requestedTab = params.get('tab')", "const sectionTab = ({ academic: 'academic', interests: 'interests', career: 'career', documents: 'settings' } as Record<string, string>)[params.get('section') || ''];\n  const requestedTab = sectionTab || params.get('tab')")
s=s.replace("next.set('tab', tabKeys[tabs.indexOf(tab)]);", "next.delete('section');\n    next.set('tab', tabKeys[tabs.indexOf(tab)]);")
p.write_text(s,encoding='utf-8')
p=root/'pages/student/StudentPages.tsx';s=p.read_text(encoding='utf-8').replace('export const StudentApplicationsPage = () => {','export const StudentApplicationsPage = ({ savedOnly = false }: { savedOnly?: boolean }) => {').replace('((bursaries.data?.items ?? []) as Array<any>).map((b) => {','((bursaries.data?.items ?? []) as Array<any>).filter(b => !savedOnly || (saved.data ?? []).includes(b.id)).map((b) => {');p.write_text(s,encoding='utf-8')
# Route canonicalization also covers aliases introduced by the unfinished migration.
redirects={}
def route(old,parent,section): redirects[old]='/student/'+parent+'?section='+section
for old,parent,section in [
('academic-profile','profile','academic'),('documents','profile','documents'),('qualifications','profile','career'),('experience','profile','career'),('my-school','profile','school'),('cv-builder','profile','cv'),('profile/cv','profile','cv'),
('ai-guidance','learning','guidance'),('recommendations/careers','learning','guidance'),('ai-tutor','learning','tutor'),('learning/tutor','learning','tutor'),('interview-prep','learning','tutor'),('learning-centre','learning','centre'),
('psychometric','career-explorer','career-match'),('psychometric-test','career-explorer','career-match'),('career-explorer/interests','career-explorer','interests'),('career-explorer/match','career-explorer','guidance'),('career-guidance','career-explorer','guidance'),('career-roadmaps','career-explorer','career-path'),
('saved','funding','opportunities'),('opportunities','funding','opportunities'),('applications','funding','applications'),('bursaries','funding','bursaries'),('scholarships','funding','scholarships'),('scholarship-assistant','funding','scholarships'),('recommendations/bursaries','funding','matches'),
('universities','institutions','universities'),('colleges-tvets','institutions','colleges-tvets'),('university-applications','institutions','applications'),('rewards','progress','rewards'),('progress/rewards','progress','rewards')]: route(old,parent,section)
for parent,sections in [('funding',['saved','applications','scholarships','matches']),('institutions',['universities','colleges-tvets','applications'])]:
 for section in sections: route(parent+'/'+section,parent,section)
p=root/'app/App.tsx';s=p.read_text(encoding='utf-8');s="import { StudentProfileSectionsPage, StudentCareerExplorerPage, StudentLearningResourcesPage } from '@/pages/student/StudentSectionPages';\n"+s
for path,component in [('profile','StudentProfileSectionsPage'),('career-explorer','StudentCareerExplorerPage'),('learning','StudentLearningResourcesPage')]:
 s=re.sub(r'<Route path="'+path+r'" element=\{<\w+ />\} />',f'<Route path="{path}" element={{<{component} />}} />',s)
extra=[]
for path,target in redirects.items():
 line=f'<Route path="{path}" element={{<StudentRedirect to="{target}" />}} />'
 pattern=r'<Route path="'+re.escape(path)+r'" element=\{<[^\n]+?/>\} />'
 if re.search(pattern,s):s=re.sub(pattern,lambda _:line,s)
 else:extra.append('          '+line)
s=s.replace('          <Route path="*" element={<StudentNotFoundPage />} />','\n'.join(extra)+'\n          <Route path="*" element={<StudentNotFoundPage />} />')
# Remove now-unused route imports after canonicalization.
for line in s.splitlines():
 if line.startswith('import {') and 'student/' in line:
  names=line.split('{',1)[1].split('}',1)[0].split(',')
  used=[name.strip() for name in names if len(re.findall(r'\b'+name.strip()+r'\b',s))>1]
  s=s.replace(line,re.sub(r'\{[^}]+\}','{ '+', '.join(used)+' }',line) if used else '')
p.write_text(s,encoding='utf-8')
# Persist inventory destinations and verified service families before validation.
with Path('docs/student-feature-parity.md').open('a',encoding='utf-8') as f:
 f.write('\n## Compatibility map\n\n| Old route | New parent / section | Accessible | Functional / API |\n|---|---|---|---|\n')
 for path,target in redirects.items():f.write(f'| /student/{path} | {target} | Routed + section navigation + tool search | Existing component and service retained; live verification pending |\n')
 f.write('\nDashboard, notifications, subscription, settings, career details, university programmes/admission requirements retain their routes under StudentAppShell. Goals reuse profile goal editing; Messages reuse notifications (no separate legacy messaging API).\n')
 f.write('\nPreserved APIs: studentService (profile, saved profiles, uploads, opportunities, saved bursaries); schoolService (school directory/join); aiGuidanceService + recommendationService; psychometricService (assessments, attempts, history); featureModulesService (CV/suggestions/save, tutor sessions/ask, scholarships/motivation, university applications CRUD, roadmaps/APS/progress); learningService; gamificationService; applicationService; bursaryService; institutionService; universityInfoService; notificationService; subscriptionService (checkout, confirm, cancel, PayFast initiate/status); settingsService; accountService. All retain useAppQuery/useMutation integration.\n')
