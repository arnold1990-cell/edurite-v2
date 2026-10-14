import { describe, expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server';
import { ExploreWorkspace } from './ExploreWorkspace';

const state = vi.hoisted(() => ({ data: { content: [] as { id: string; title: string }[] }, isLoading: false, isFetching: false, isError: false }));
vi.mock('@/hooks/useAppQuery', () => ({ useAppQuery: () => state }));
vi.mock('@/features/subscriptions/access', () => ({ useSubscriptionAccess: () => ({}) }));
vi.mock('./ExploreResources', () => ({ ExploreOpportunities: () => null, ExploreStudyOptions: () => null }));
function render(careerName = '') {
  return renderToStaticMarkup(<StaticRouter location="/student/career-explorer"><ExploreWorkspace
    careerName={careerName} onSelect={() => undefined} onClear={() => undefined} selectionPending={false}
    academicSubjects={[]} savedSnapshot={false} roadmap={null} aps={null} requiredAps={null} gap={null}
    saved={[]} savedLoading={false} savedError={false} retrySaved={() => undefined} onSaved={() => undefined}
    history={[]} clearHistory={() => undefined} outdated={false} actions={null} editor={null} details={null} printContent={null}
  /></StaticRouter>);
}
describe('career selector states', () => {
  it('provides labelled search, selectable catalogue results, confirmation and clear action', () => {
    Object.assign(state, { data: { content: [{ id: 'catalogue-id', title: 'Catalogue Career' }] }, isError: false, isLoading: false, isFetching: false });
    const html=render('Catalogue Career');
    expect(html).toContain('for="career-search"');
    expect(html).toContain('for="career-choice"');
    expect(html).toContain('value="Catalogue Career"');
    expect(html).toContain('Confirm career selection');
    expect(html).toContain('Clear selected career');
  });
  it('explains unsupported search without inventing a result', () => {
    Object.assign(state, { data: { content: [] }, isError: false });
    expect(render()).toContain('No supported careers match your search');
  });
  it('distinguishes failed search from a successful empty result', () => {
    state.isError=true;
    const html=render();
    expect(html).toContain('Retry search');
    expect(html).not.toContain('No supported careers match your search');
  });
});
