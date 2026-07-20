import { describe, expect, it } from 'vitest';
import { authoritativeAps, calculateApsGap, calculateNscLevel, countValidManualSubjects, normalizeManualRow } from '@/pages/student/roadmapAps.utils';

describe('roadmapAps.utils', () => {
  it('maps NSC mark boundaries to the correct levels', () => {
    expect(calculateNscLevel(0)).toBe(1);
    expect(calculateNscLevel(29)).toBe(1);
    expect(calculateNscLevel(30)).toBe(2);
    expect(calculateNscLevel(39)).toBe(2);
    expect(calculateNscLevel(40)).toBe(3);
    expect(calculateNscLevel(49)).toBe(3);
    expect(calculateNscLevel(50)).toBe(4);
    expect(calculateNscLevel(59)).toBe(4);
    expect(calculateNscLevel(60)).toBe(5);
    expect(calculateNscLevel(69)).toBe(5);
    expect(calculateNscLevel(70)).toBe(6);
    expect(calculateNscLevel(79)).toBe(6);
    expect(calculateNscLevel(80)).toBe(7);
    expect(calculateNscLevel(100)).toBe(7);
  });

  it('rejects invalid marks instead of inventing a level', () => {
    expect(calculateNscLevel(-1)).toBeNull();
    expect(calculateNscLevel(101)).toBeNull();
    expect(calculateNscLevel(Number.NaN)).toBeNull();
    expect(calculateNscLevel(undefined)).toBeNull();
  });

  it('derives manual subject levels from the mark and keeps empty marks empty', () => {
    expect(normalizeManualRow({ id: '1', subjectName: 'Accounting', markPercentage: '75' })).toEqual({
      subjectName: 'Accounting',
      markPercentage: 75,
      level: 6,
      apsPoints: 6,
    });

    expect(normalizeManualRow({ id: '2', subjectName: 'Business Studies', markPercentage: '' })).toEqual({
      subjectName: 'Business Studies',
      markPercentage: null,
      level: null,
      apsPoints: null,
    });
  });

  it('counts only manual rows with valid numeric marks', () => {
    expect(countValidManualSubjects([
      { id: '1', subjectName: 'Accounting', markPercentage: '75' },
      { id: '5', subjectName: 'History', markPercentage: '62' },
      { id: '2', subjectName: 'Mathematics', markPercentage: '' },
      { id: '3', subjectName: '', markPercentage: '82' },
      { id: '4', subjectName: 'English', markPercentage: 'hello' },
    ])).toBe(2);
  });

  it('calculates APS gaps without fake negative values', () => {
    expect(calculateApsGap(25, 30)).toBe(5);
    expect(calculateApsGap(32, 30)).toBe(0);
    expect(calculateApsGap(null, 30)).toBeNull();
    expect(calculateApsGap(25, null)).toBeNull();
  });

  it('reads the authoritative APS only from the structured calculation response', () => {
    expect(authoritativeAps({ totalAps: 25 } as never)).toBe(25);
    expect(authoritativeAps({ totalAps: null } as never)).toBeNull();
    expect(authoritativeAps(undefined)).toBeNull();
  });
});
