import { describe, expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server';
import { StudentRouteAccess } from './access';

const query = vi.hoisted(() => ({ value: {} as Record<string, unknown> }));
vi.mock('@tanstack/react-query', () => ({ useQuery: () => query.value }));
vi.mock('@/hooks/useAuth', () => ({ useAuth: () => ({ user: { id: 'student' } }) }));
const features = [
  { id: 'CAREER_ROADMAP_PERSONALISED', minimumPlan: 'PREMIUM', label: 'Personalised career roadmap' },
  { id: 'APPLICATION_SUPPORT', minimumPlan: 'PRO', label: 'Application preparation and guidance' },
];
function render(location: string, entitlements: string[], error = false, accessAllowed = true) {
  query.value = { isPending: false, isError: error, data: { accessAllowed, entitlements, features, routeRules: [
    { path: '/student/career-explorer', section: 'career-path', feature: features[0].id },
    { path: '/student/funding', section: 'applications', feature: features[1].id },
  ] } };
  return renderToStaticMarkup(<StaticRouter location={location}><StudentRouteAccess><p>Protected feature content</p></StudentRouteAccess></StaticRouter>);
}
describe('direct student URLs use server entitlements', () => {
  it('locks a manually entered Premium URL for Basic', () => {
    const html = render('/student/career-explorer?section=career-path', []);
    expect(html).toContain('Upgrade to PREMIUM');
    expect(html).toContain('View Plans');
    expect(html).not.toContain('Protected feature content');
  });
  it('locks Pro applications for Premium', () => {
    expect(render('/student/funding?section=applications', [features[0].id])).toContain('Upgrade to PRO');
  });
  it('unlocks only after the server supplies the entitlement', () => {
    expect(render('/student/funding?section=applications', features.map(f => f.id))).toContain('Protected feature content');
  });
  it('fails closed while keeping account management reachable', () => {
    expect(render('/student/funding?section=applications', [], true)).not.toContain('Protected feature content');
    expect(render('/student/subscription', [], true)).toContain('Protected feature content');
  });
  it('blocks expired trial discovery regardless of browser query claims', () => {
    const html = render('/student/career-explorer?plan=PRO&trialEndDate=2099-01-01', [], false, false);
    expect(html).toContain('Your 14-day free trial has ended.');
    expect(html).toContain('View Plans');
    expect(html).not.toContain('Protected feature content');
    expect(render('/student/subscription', [], false, false)).toContain('Protected feature content');
    expect(render('/student/settings', [], false, false)).toContain('Protected feature content');
    expect(render('/student/profile', [], false, false)).toContain('Protected feature content');
  });
  it('shows server days remaining and a near-expiry warning without hiding content', () => {
    render('/student/dashboard', []);
    Object.assign(query.value.data as object, { trialActive: true, trialDaysRemaining: 2, trialEndDate: '2026-10-06T12:00:00Z' });
    const html = renderToStaticMarkup(<StaticRouter location="/student/dashboard"><StudentRouteAccess>Learning</StudentRouteAccess></StaticRouter>);
    expect(html).toContain('Free Trial');
    expect(html).toContain('2 days remaining');
    expect(html).toContain('Trial ends on');
    expect(html).toContain('Upgrade');
    expect(html).toContain('border-amber-500');
    expect(html).toContain('Learning');
  });
  it('leaves basic discovery available', () => {
    expect(render('/student/career-explorer', [])).toContain('Protected feature content');
  });
});
