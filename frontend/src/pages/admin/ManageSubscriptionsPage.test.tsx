import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { ManageSubscriptionsPage } from './ManageSubscriptionsPage';

const state = vi.hoisted(() => ({ result: {} as Record<string, unknown> }));
vi.mock('@tanstack/react-query', () => ({
  useQuery: () => state.result,
  useQueryClient: () => ({ invalidateQueries: vi.fn() }),
  useMutation: () => ({ isPending: false, isError: false, reset: vi.fn() }),
}));
describe('existing admin subscription page', () => {
  beforeEach(() => { state.result = { data: { content: [], totalElements: 0, totalPages: 0 }, isPending: false, isError: false }; });
  it('offers search and a clear empty state', () => {
    const html = renderToStaticMarkup(<ManageSubscriptionsPage />);
    expect(html).toContain('Manage User Subscriptions');
    expect(html).toContain('Search users by name, username or email');
    expect(html).toContain('No matching users');
    expect(html).not.toContain('Company Oversight');
  });
  it('renders real API fields and all three management actions', () => {
    state.result = { data: { content: [{ userId: 'user-id', fullName: 'Managed Learner', email: 'managed@example.test', currentPlan: 'PRO', source: 'ADMIN_OVERRIDE', status: 'ACTIVE' }], totalElements: 1, totalPages: 1 } };
    const html = renderToStaticMarkup(<ManageSubscriptionsPage />);
    for (const value of ['Managed Learner', 'managed@example.test', 'PRO', 'ADMIN_OVERRIDE', 'ACTIVE', 'Change Plan', 'View Subscription', 'View History']) expect(html).toContain(value);
  });
  it('renders loading and retryable API failures', () => {
    state.result = { isPending: true };
    expect(renderToStaticMarkup(<ManageSubscriptionsPage />)).toContain('Loading subscriptions');
    state.result = { isError: true, error: new Error('Access refused') };
    const html = renderToStaticMarkup(<ManageSubscriptionsPage />);
    expect(html).toContain('Access refused'); expect(html).toContain('Retry');
  });
});
