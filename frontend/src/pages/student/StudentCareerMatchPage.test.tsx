import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server';
import type { Recommendation } from '@/types';
import { StudentCareerMatchPage } from './StudentCareerMatchPage';

const state = vi.hoisted(() => ({ data: undefined as Recommendation | undefined }));
vi.mock('@/hooks/useAuth', () => ({ useAuth: () => ({ user: { id: 'learner-a' } }) }));
vi.mock('@/hooks/useAppQuery', () => ({ useAppQuery: () => ({ data: state.data, refetch: vi.fn() }) }));
vi.mock('@tanstack/react-query', () => ({ useQueryClient: () => ({ invalidateQueries: vi.fn() }), useMutation: () => ({ reset: vi.fn(), mutate: vi.fn() }) }));
vi.mock('@/features/subscriptions/access', () => ({ RequireEntitlement: ({ children }: { children: React.ReactNode }) => children, AiUsageDisplay: () => null }));
vi.mock('@/components/student/profile/ProfileAssociations', () => ({ ProfileAssociationButton: () => <button>Link to My Profile</button> }));
vi.mock('./StudentPages', () => ({ StudentPsychometricPage: () => <div>Assessment questions and history</div> }));
const render = (url = '/student/career-match') => renderToStaticMarkup(<StaticRouter location={url}><StudentCareerMatchPage /></StaticRouter>);

describe('one Career Match journey', () => {
  beforeEach(() => { state.data = { suggestedCareers: [], suggestedBursaries: [], suggestedCoursesOrImprovements: [], profileImprovementTips: [], modelVersion: 'test', inputsUsed: [], missingInformation: [] }; });
  it.each([[true, false], [false, true], [true, true], [false, false]])('shows available evidence and missing inputs (academic=%s assessment=%s)', (academic, assessment) => {
    state.data!.inputsUsed = [academic ? 'Saved academic results: Mathematics 72%' : '', assessment ? 'Saved psychometric responses: analytical' : ''].filter(Boolean);
    state.data!.missingInformation = [academic ? '' : 'Academic subjects and marks', assessment ? '' : 'Psychometric assessment (optional)'].filter(Boolean);
    if (academic || assessment) state.data!.suggestedCareers = [{ id: 'career-a', title: 'Data analyst', score: 55, rationale: 'Recorded interests and subject evidence relate to the catalogue description.' }];
    const html = render();
    expect(html.includes('Mathematics 72%')).toBe(academic);
    expect(html.includes('Saved psychometric responses: analytical')).toBe(assessment);
    expect(html).not.toContain('55%');
    expect(html).toContain('Refresh recommendations');
    expect(html).toContain('/student/profile?section=academic');
    expect(html).toContain('/student/career-match?step=assessment');
    expect(html).toContain('not a diagnosis');
    if (!academic && !assessment) expect(html).toContain('No supported catalogue matches yet');
  });
  it('opens questions and history inside Career Match, without a second recommendation list', () => {
    const html = render('/student/career-match?step=assessment');
    expect(html).toContain('Assessment questions and history');
    expect(html).toContain('Back to Career Match');
    expect(html).not.toContain('Career recommendations');
  });
});
