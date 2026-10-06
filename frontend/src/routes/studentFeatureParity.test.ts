import { describe, expect, it } from 'vitest';
import { studentRedirectTarget } from './StudentRedirect';
import { studentTools } from '@/components/student/StudentSectionNavigation';
describe('student feature compatibility', () => {
  it('merges canonical sections without losing query or callback data', () => {
    const destination = studentRedirectTarget('/student/profile?section=cv', '?q=engineer&section=old&paymentReference=abc', '#preview');
    expect(destination).toBe('/student/profile?q=engineer&section=cv&paymentReference=abc#preview');
  });
  it.each([
    ['Courses', '/student/study-options'], ['Subjects', '/student/study-options'],
    ['Institutions', '/student/study-options'], ['Bursaries', '/student/funding'], ['Opportunities', '/student/funding'],
  ])('preserves legacy explore category %s', (category, parent) => {
    const destination = studentRedirectTarget('/student/career-explorer', `?category=${category}&q=science&institution=uct`);
    expect(destination.split('?')[0]).toBe(parent);
    expect(destination).toContain('institution=uct');
    expect(destination).toContain('q=science');
  });
  it.each(['CV Builder', 'Tutor', 'AI Guidance', 'Psychometric', 'Bursaries', 'Bursary', 'University', 'Universities', 'Subscription', 'Settings', 'Notifications', 'Points & Rewards', 'School', 'Documents', 'Study Plan'])('indexes the %s tool', name => {
    expect(studentTools.some(tool => tool.label.includes(name))).toBe(true);
  });
});
