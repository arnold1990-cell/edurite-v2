import type { StudentSubjectAchievement } from '@/types';

// A selected extraction replaces only that subject after an explicit learner choice.
export function selectExtractedResult(current: StudentSubjectAchievement[], extracted: StudentSubjectAchievement): StudentSubjectAchievement[] {
  const key = academicSubjectKey(extracted.subjectName);
  const existing = current.find(row => academicSubjectKey(row.subjectName) === key);
  if (existing?.verified) throw new Error('Verified results require administrator review to change.');
  return [...current.filter(row => row.subjectName.trim() && academicSubjectKey(row.subjectName) !== key), { ...extracted }];
}

export function validateAcademicResults(rows: StudentSubjectAchievement[]): void {
  for (const row of rows) {
    if (!row.subjectName.trim()) {
      if (row.markPercentage != null || row.achievementLevel != null) throw new Error('Choose a subject for every entered mark.');
      continue;
    }
    if (row.markPercentage == null && row.achievementLevel == null) throw new Error(`Enter a percentage or achievement level for ${row.subjectName}.`);
    if (row.markPercentage != null && (!Number.isInteger(row.markPercentage) || row.markPercentage < 0 || row.markPercentage > 100)) throw new Error('Enter whole-number marks between 0 and 100.');
  }
}

export function academicSubjectKey(name: string): string {
  const key = name.trim().toLowerCase().replace(/\s+/g, ' ').replace(/\bhl$/, 'home language').replace(/\bfal$/, 'first additional language').replace(/\bsal$/, 'second additional language');
  return ({ maths: 'mathematics', math: 'mathematics', 'math lit': 'mathematical literacy', 'physical science': 'physical sciences', 'life science': 'life sciences', cat: 'computer applications technology' } as Record<string, string>)[key] ?? key;
}

export function mergeManualResults(saved: StudentSubjectAchievement[], incoming: StudentSubjectAchievement[]): StudentSubjectAchievement[] {
  const merged = new Map(saved.map(row => [academicSubjectKey(row.subjectName), row]));
  const seen = new Set<string>();
  for (const row of incoming) {
    const key = academicSubjectKey(row.subjectName);
    if (seen.has(key)) throw new Error(`Choose one result for ${row.subjectName} before saving.`);
    seen.add(key);
    const previous = merged.get(key);
    const sameMark = previous?.markPercentage === row.markPercentage && previous?.achievementLevel === row.achievementLevel;
    if (previous?.verified && !sameMark) throw new Error(`The verified result for ${previous.subjectName} requires administrator review to change.`);
    merged.set(key, sameMark && previous ? previous : { ...previous, ...row, subjectName: previous?.subjectName ?? row.subjectName, source: 'MANUAL', verified: false, documentId: undefined });
  }
  return [...merged.values()];
}
