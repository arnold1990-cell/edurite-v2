import { afterEach, describe, expect, it, vi } from 'vitest';
import { verificationDestination, verificationNoticePath } from './verificationNavigation';
import { getDashboardPathForUser, isAuthorizedPathForRole } from './roleUtils';

afterEach(() => vi.unstubAllGlobals());
describe('verification and login destinations', () => {
  it('preserves query and fragment through an email link opened in another tab', () => {
    const values = new Map<string, string>();
    vi.stubGlobal('localStorage', { setItem: (k: string, v: string) => values.set(k, v), getItem: (k: string) => values.get(k), removeItem: (k: string) => values.delete(k) });
    const target = '/student/profile?section=academic#marks';
    expect(new URL(verificationNoticePath('Learner@example.test', target), 'https://edurite.test').searchParams.get('next')).toBe(target);
    expect(verificationDestination('learner@example.test')).toBe(target);
    expect(verificationDestination('learner@example.test')).toBe('');
  });
  it('works without storage and rejects external destinations', () => {
    vi.stubGlobal('localStorage', { setItem: () => { throw new Error('disabled'); }, getItem: () => { throw new Error('disabled'); } });
    expect(verificationNoticePath('student@example.test', '//evil.test')).toContain('next=');
    expect(verificationDestination('student@example.test', '//evil.test')).toBe('');
    expect(verificationDestination('student@example.test', '/student/dashboard')).toBe('/student/dashboard');
  });
  it.each([
    ['ROLE_STUDENT', '/student/dashboard'], ['ROLE_ADMIN', '/admin/dashboard'],
    ['ROLE_COMPANY', '/company/dashboard'], ['ROLE_SCHOOL_ADMIN', '/school/dashboard'],
    ['ROLE_TEACHER', '/teacher/dashboard'], ['ROLE_SCHOOL_STUDENT', '/school-student/dashboard'],
    ['ROLE_DISTRICT_ADMIN', '/district/dashboard'], ['ROLE_CIRCUIT_MANAGER', '/district/circuit/dashboard'],
    ['ROLE_SUBJECT_ADVISOR', '/district/advisor/dashboard'],
  ])('routes authenticated %s to its dashboard', (role, path) => {
    expect(getDashboardPathForUser({ roles: [role] as never, approvalStatus: 'APPROVED' })).toBe(path);
  });
  it('rejects a destination belonging to another role', () => {
    expect(isAuthorizedPathForRole('/admin/dashboard', 'STUDENT')).toBe(false);
    expect(isAuthorizedPathForRole('/student/profile?section=academic', 'STUDENT')).toBe(true);
  });
});
