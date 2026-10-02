import { describe, expect, it } from 'vitest';
import { academicMark, careerMatch, percentage, upcomingBursaries } from './dashboardData';

describe('student dashboard data presentation', () => {
  it('keeps missing values distinct from a real zero and bounds indicators', () => {
    expect(percentage(undefined)).toBeNull();
    expect(percentage(Number.NaN)).toBeNull();
    expect(percentage(0)).toBe(0);
    expect(percentage(120)).toBe(100);
    expect(percentage(-5)).toBe(0);
  });
  it('does not invent a percentage from an NSC achievement level', () => {
    expect(academicMark({ subjectName: 'Mathematics', achievementLevel: 6 })).toBeNull();
    expect(academicMark({ subjectName: 'Mathematics', achievementLevel: 6, markPercentage: 74 })).toBe(74);
  });
  it('uses the backend percentage score without rescaling it', () => {
    expect(careerMatch({ id: 'a', title: 'Career', score: 91, rationale: '' })).toBe(91);
  });
  it('shows only valid upcoming deadlines, including today, in deadline order', () => {
    const rows = [
      { id: 'old', deadline: '2026-09-27' },
      { id: 'later', deadline: '2026-10-05' },
      { id: 'today', deadline: '2026-09-29' },
      { id: 'invalid', deadline: 'TBC' },
      { id: 'missing', deadline: undefined },
    ].map((item) => ({ ...item, title: item.id, status: 'OPEN' }));
    expect(upcomingBursaries(rows, new Date(2026, 8, 29, 15)).map((row) => row.id)).toEqual(['today', 'later']);
  });
});
