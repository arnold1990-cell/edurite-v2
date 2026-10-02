import { Badge } from '@/components/ui/Badge';
import { EmptyState } from '@/components/feedback/States';
import { InstitutionLogo } from '@/components/institutions/InstitutionLogo';
import { resolveInstitutionDisplay } from '@/lib/institutionRegistry';
import type { CareerRoadmapGenerateResponse, CareerRoadmapPathwayStep, CareerRoadmapSubjectRequirement } from '@/types';
const tabs = ['Roadmap', 'University Requirements', 'APS Readiness', 'Subject Requirements', 'Alternative Pathways', 'AI Study Plan'] as const;
export type RoadmapTab = typeof tabs[number];
const TEXT_REPLACEMENTS: Array<[string, string]> = [
  ['â€¢', '•'],
  ['â€“', '–'],
  ['â€”', '—'],
  ['â€™', '’'],
  ['ï¿½', '�'],
];

const roadmapStatusColor = (status?: string): 'emerald' | 'amber' | 'slate' => {
  const normalized = (status ?? '').toLowerCase();
  if (normalized.includes('eligible') && !normalized.includes('almost') && !normalized.includes('not')) return 'emerald';
  if (normalized.includes('almost')) return 'amber';
  return 'slate';
};

const normalizeText = (value?: string | null) => {
  if (!value) return '';
  return TEXT_REPLACEMENTS.reduce((result, [from, to]) => result.split(from).join(to), value).replace(/\uFFFD/g, '•');
};

const pathwayBlock = (title: string, steps: CareerRoadmapPathwayStep[]) => (
  <div className="rounded-2xl border border-slate-200 bg-white p-4">
    <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
    <div className="mt-3 space-y-3">
      {steps.map((step) => (
        <div key={`${title}-${step.title}`}>
          <p className="text-sm font-medium text-slate-800">{normalizeText(step.title)}</p>
          <p className="text-sm text-slate-600">{normalizeText(step.description)}</p>
        </div>
      ))}
    </div>
  </div>
);

const subjectRequirementCard = (item: CareerRoadmapSubjectRequirement) => (
  <article key={`${item.subject}-${item.required}`} className="rounded-2xl border border-slate-200 bg-white p-4">
    <div className="flex items-start justify-between gap-3">
      <div>
        <h3 className="text-sm font-semibold text-slate-900">{normalizeText(item.subject)}</h3>
        <p className="text-sm text-slate-600">{normalizeText(item.notes) || (item.required ? 'Required subject' : 'Recommended subject')}</p>
      </div>
      <Badge color={item.required ? 'emerald' : 'amber'}>{item.required ? 'Required' : 'Recommended'}</Badge>
    </div>
    <div className="mt-3 grid gap-2 text-sm text-slate-700 sm:grid-cols-3">
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Minimum</p><p>{normalizeText(item.minimumPass) || 'Not set'}</p></div>
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Level</p><p>{item.minimumLevel ?? 'Not set'}</p></div>
      <div><p className="text-xs uppercase tracking-wide text-slate-400">Suggested mark</p><p>{normalizeText(item.suggestedMark) || 'Not set'}</p></div>
    </div>
  </article>
);

type Props = { current: CareerRoadmapGenerateResponse | null; activeTab: RoadmapTab; setActiveTab: (tab: RoadmapTab) => void; displayCurrentAps: number | null; displayRequiredAps: number | null; displayApsGap: number | null; };
export function CareerRoadmapDetails({ current, activeTab, setActiveTab, displayCurrentAps, displayRequiredAps, displayApsGap }: Props) { return <>        <div className="rounded-3xl border border-slate-200 bg-white">
          <div className="flex flex-wrap gap-2 border-b border-slate-200 px-4 py-3">
            {tabs.map((tab) => (
              <button key={tab} type="button" className={`rounded-full px-4 py-2 text-sm font-medium ${activeTab === tab ? 'bg-primary-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`} onClick={() => setActiveTab(tab)}>
                {tab}
              </button>
            ))}
          </div>
          <div className="p-5">
            {!current ? <EmptyState title="Generate a roadmap" message="Search a career, enter your subjects and marks, then generate your AI career and university readiness roadmap." /> : null}

            {current && activeTab === 'Roadmap' ? <div className="space-y-4">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-semibold text-slate-900">{normalizeText(current.careerName)}</h2>
                    <p className="mt-2 text-sm text-slate-600">{normalizeText(current.overview.description) || 'Career overview is being refined.'}</p>
                  </div>
                  <Badge color={roadmapStatusColor(current.apsReadiness.status)}>{current.apsReadiness.status}</Badge>
                </div>
                <div className="mt-4 grid gap-4 lg:grid-cols-2">
                  <div>
                    <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Daily responsibilities</p>
                    <ul className="mt-2 space-y-2 text-sm text-slate-700">
                      {current.overview.dailyResponsibilities.map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                    </ul>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Skills needed</p>
                    <ul className="mt-2 space-y-2 text-sm text-slate-700">
                      {current.overview.skillsNeeded.map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                    </ul>
                  </div>
                </div>
                <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-3">
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Career demand</p><p>{normalizeText(current.overview.careerDemand) || 'Verify current demand by region.'}</p></div>
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Salary range</p><p>{normalizeText(current.overview.salaryRange) || 'Varies by experience.'}</p></div>
                  <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Professional body</p><p>{normalizeText(current.overview.professionalBody) || 'Check sector-specific registration.'}</p></div>
                </div>
              </div>
              <div className="grid gap-4 lg:grid-cols-2">
                {pathwayBlock('University pathway', current.universityPathway)}
                {pathwayBlock('Professional pathway', current.professionalPathway)}
              </div>
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <h3 className="text-sm font-semibold text-slate-900">Roadmap timeline</h3>
                <div className="mt-4 space-y-3">
                  {current.roadmapTimeline.map((item) => (
                    <div key={`${item.stage}-${item.title}`} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                      <p className="text-sm font-medium text-slate-900">{item.stage ? `${item.stage}. ` : ''}{normalizeText(item.title)}</p>
                      <p className="mt-1 text-sm text-slate-600">{normalizeText(item.description)}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div> : null}

            {current && activeTab === 'University Requirements' ? <div className="space-y-4">
              <div className="grid gap-3 lg:grid-cols-2">
                {current.universityRequirements.map((item) => {
                  const institution = resolveInstitutionDisplay({ name: item.institutionName, province: item.province, applicationUrl: item.applicationUrl, institutionType: item.institutionType });
                  return (
                  <article key={`${item.institutionName}-${item.qualificationName}`} className="rounded-2xl border border-slate-200 bg-white p-4">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="flex items-start gap-3">
                        <InstitutionLogo src={institution.logoUrl} institutionName={institution.displayName} abbreviation={institution.abbreviation} size={56} className="rounded-2xl" />
                        <div>
                          <h3 className="text-sm font-semibold text-slate-900">{item.institutionName}</h3>
                          <p className="text-sm text-slate-600">{normalizeText(item.qualificationName)}</p>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Badge color={item.verified ? 'emerald' : 'amber'}>{item.verificationBadge}</Badge>
                        <Badge color={roadmapStatusColor(item.requirementStatus)}>{item.requirementStatus}</Badge>
                      </div>
                    </div>
                    <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">{item.verified ? 'APS' : 'Estimated APS'}</p><p>{item.apsRequired ?? 'Verify'}{item.apsGap != null ? ` · Gap ${item.apsGap}` : ''}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Province</p><p>{item.province || 'South Africa'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Mathematics</p><p>{item.mathematicsRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">English</p><p>{item.englishRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Accounting</p><p>{item.accountingRequirement || 'Not stated'}</p></div>
                      <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Duration</p><p>{item.duration || 'Verify with institution'}</p></div>
                    </div>
                    <p className="mt-3 text-sm text-slate-600">{normalizeText(item.notes || item.source) || 'Verify the full admission requirement directly with the institution.'}</p>
                    {item.applicationUrl ? <a className="mt-3 inline-block text-sm font-medium text-primary-600 hover:text-primary-500" href={item.applicationUrl} target="_blank" rel="noreferrer">Application link</a> : null}
                  </article>
                );
                })}
              </div>
            </div> : null}

            {current && activeTab === 'APS Readiness' ? <div className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Readiness summary</h3>
                  <div className="mt-4 grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Current APS</p><p>{displayCurrentAps ?? 'Unavailable'}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Required APS</p><p>{displayRequiredAps ?? 'APS requirement not verified'}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">APS gap</p><p>{displayApsGap ?? 'Unavailable'}</p></div>
                    <div><p className="text-xs uppercase tracking-[0.2em] text-slate-400">Risk level</p><p>{current.gapAnalysis?.riskLevel ?? 'Pro insight'}</p></div>
                  </div>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Improvement suggestions</h3>
                  <ul className="mt-3 space-y-2 text-sm text-slate-700">
                    {(current.gapAnalysis?.improvementSuggestions ?? []).map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                  </ul>
                </div>
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Missing subjects</h3>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {(current.gapAnalysis?.missingSubjects ?? []).length ? (current.gapAnalysis?.missingSubjects ?? []).map((item) => <Badge key={item} color="amber">{item}</Badge>) : <p className="text-sm text-slate-500">No missing required subjects detected.</p>}
                  </div>
                </div>
                <div className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">Subjects needing improvement</h3>
                  <ul className="mt-3 space-y-2 text-sm text-slate-700">
                    {(current.gapAnalysis?.subjectsNeedingImprovement ?? []).length ? (current.gapAnalysis?.subjectsNeedingImprovement ?? []).map((item) => <li key={item}>• {normalizeText(item)}</li>) : <li className="text-slate-500">No urgent subject improvements flagged.</li>}
                  </ul>
                </div>
              </div>
            </div> : null}

            {current && activeTab === 'Subject Requirements' ? <div className="space-y-4">
              <div className="grid gap-4 lg:grid-cols-2">
                {current.requiredSubjects.map(subjectRequirementCard)}
                {current.recommendedSubjects.map(subjectRequirementCard)}
              </div>
            </div> : null}

            {current && activeTab === 'Alternative Pathways' ? <div className="space-y-4">
              <div className="rounded-2xl border border-slate-200 bg-white p-4">
                <h3 className="text-sm font-semibold text-slate-900">Alternative pathways</h3>
                <ul className="mt-3 space-y-2 text-sm text-slate-700">
                  {current.alternativePathways.map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                </ul>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <h3 className="text-sm font-semibold text-slate-900">Best-fit universities</h3>
                <ul className="mt-3 space-y-2 text-sm text-slate-700">
                  {(current.gapAnalysis?.bestFitUniversities ?? []).map((item) => <li key={item}>• {normalizeText(item)}</li>)}
                </ul>
              </div>
            </div> : null}

            {current && activeTab === 'AI Study Plan' ? <div className="space-y-4">
              {current.studyPlan.map((item) => (
                <article key={item.title} className="rounded-2xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-900">{normalizeText(item.title)}</h3>
                  <p className="mt-1 text-sm text-slate-600">{normalizeText(item.focus)}</p>
                  <ul className="mt-3 space-y-2 text-sm text-slate-700">
                    {item.actions.map((action) => <li key={action}>• {normalizeText(action)}</li>)}
                  </ul>
                </article>
              ))}
            </div> : null}
          </div>
        </div>

        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          Admission requirements may change yearly. Always verify final requirements directly with the university or college before applying.
        </div>
</>; }
