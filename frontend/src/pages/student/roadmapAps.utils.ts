import type { ApsCalculationResponse, ApsSubjectInput, ApsSubjectResult } from '@/types';

export type ResultSource = 'MANUAL' | 'PROFILE';

export type SubjectRow = {
  id: string;
  subjectName: string;
  markPercentage: string;
  level?: number | null;
  apsPoints?: number | null;
  included?: boolean;
  exclusionReason?: string | null;
};

export const calculateNscLevel = (mark?: number | null) => {
  if (mark == null || Number.isNaN(mark) || mark < 0 || mark > 100) return null;
  if (mark >= 80) return 7;
  if (mark >= 70) return 6;
  if (mark >= 60) return 5;
  if (mark >= 50) return 4;
  if (mark >= 40) return 3;
  if (mark >= 30) return 2;
  return 1;
};

export const createSubjectRow = (): SubjectRow => ({
  id: typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`,
  subjectName: '',
  markPercentage: '',
});

export const normalizeManualRow = (row: SubjectRow): ApsSubjectInput | null => {
  if (!row.subjectName.trim()) return null;
  if (row.markPercentage === '') {
    return {
      subjectName: row.subjectName.trim(),
      markPercentage: null,
      level: row.level ?? null,
      apsPoints: row.apsPoints ?? row.level ?? null,
    };
  }
  const mark = Number(row.markPercentage);
  const level = calculateNscLevel(mark);
  return {
    subjectName: row.subjectName.trim(),
    markPercentage: Number.isNaN(mark) ? null : mark,
    level,
    apsPoints: level,
  };
};

export const fingerprintSubjects = (subjects: ApsSubjectInput[]) => subjects
  .map((subject) => `${subject.subjectName}:${subject.markPercentage ?? ''}:${subject.level ?? ''}:${subject.apsPoints ?? ''}`)
  .sort()
  .join('|');

export const fingerprintCalculation = (
  source: ResultSource,
  careerName: string,
  grade: string,
  province: string,
  subjects: ApsSubjectInput[],
  resultSetId?: string | null,
) => `${source}|${resultSetId ?? ''}|${careerName.trim().toLowerCase()}|${grade}|${province}|${fingerprintSubjects(subjects)}`;

export const toSubjectRowsFromAps = (subjects?: ApsSubjectResult[]) => {
  if (!subjects?.length) return [];
  return subjects.map((item, index) => ({
    id: `${item.subjectName}-${index}`,
    subjectName: item.subjectName,
    markPercentage: item.markPercentage == null ? '' : String(item.markPercentage),
    level: item.level ?? null,
    apsPoints: item.apsPoints ?? item.level ?? null,
    included: item.included,
    exclusionReason: item.exclusionReason ?? null,
  }));
};

export const countValidManualSubjects = (rows: SubjectRow[]) => rows.filter((row) => normalizeManualRow(row)?.markPercentage != null).length;

export const authoritativeAps = (aps?: ApsCalculationResponse | null) => aps?.totalAps ?? null;

export const calculateApsGap = (learnerAps?: number | null, requiredAps?: number | null) => {
  if (learnerAps == null || requiredAps == null || requiredAps <= 0) return null;
  return Math.max(requiredAps - learnerAps, 0);
};
