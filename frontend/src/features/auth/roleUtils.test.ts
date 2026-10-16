import { describe, expect, it } from 'vitest';
import { normalizeBackendRole } from './roleUtils';
describe('administrator role normalization', () => {
  it('uses the existing administrator portal for SUPER_ADMIN accounts', () => {
    expect(normalizeBackendRole('ROLE_SUPER_ADMIN')).toBe('ROLE_ADMIN');
    expect(normalizeBackendRole('SUPER_ADMIN')).toBe('ROLE_ADMIN');
  });
  it('does not promote ordinary or unknown roles', () => {
    expect(normalizeBackendRole('STUDENT')).toBe('ROLE_STUDENT');
    expect(normalizeBackendRole('ADMINISTRATOR')).toBeNull();
  });
});
