import { describe, expect, it } from 'vitest';
import { selectExtractedResult, validateAcademicResults, mergeManualResults } from './academicResults';

describe('academic results review', () => {
  it('preserves manual rows and context when a reviewed extraction is added', () => {
    const manual = { subjectName: 'Mathematics', markPercentage: 70, academicYear: '2025' };
    const extracted = { subjectName: 'Physical Sciences', markPercentage: 76, documentId: 'owned/report.pdf', academicYear: '2026', examination: 'Final' };
    expect(selectExtractedResult([manual], extracted)).toEqual([manual, extracted]);
  });
  it('replaces exactly one equivalent draft subject only after selection', () => {
    const current = [{ subjectName: 'Mathematics', markPercentage: 60 }, { subjectName: 'History', markPercentage: 72 }];
    const extracted = { subjectName: 'MATHEMATICS', markPercentage: 80 };
    expect(current[0].markPercentage).toBe(60);
    expect(selectExtractedResult(current, extracted)).toEqual([current[1], extracted]);
    expect(() => selectExtractedResult([{ ...current[0], verified: true }], extracted)).toThrow('administrator');
  });
  it('merges career manual marks using equivalent names and preserves other saved results and context', () => {
    const saved = [{ subjectName: 'Mathematics', markPercentage: 60, academicYear: '2026' }, { subjectName: 'History', markPercentage: 70 }];
    expect(mergeManualResults(saved, [{ subjectName: 'Maths', markPercentage: 80 }])).toEqual([
      { subjectName: 'Mathematics', markPercentage: 80, academicYear: '2026', source: 'MANUAL', verified: false, documentId: undefined }, saved[1],
    ]);
    expect(() => mergeManualResults(saved, [{ subjectName: 'Maths', markPercentage: 80 }, { subjectName: 'Mathematics', markPercentage: 60 }])).toThrow('Choose one result');
  });
  it('rejects incomplete or invalid rows rather than silently discarding marks', () => {
    expect(() => validateAcademicResults([{ subjectName: '', markPercentage: 75 }])).toThrow('Choose a subject');
    expect(() => validateAcademicResults([{ subjectName: 'Mathematics' }])).toThrow('percentage or achievement');
    for (const markPercentage of [-1, 101, 75.5, NaN]) expect(() => validateAcademicResults([{ subjectName: 'Mathematics', markPercentage }])).toThrow('whole-number');
    expect(() => validateAcademicResults([{ subjectName: 'Mathematics', markPercentage: 0 }])).not.toThrow();
  });
});
