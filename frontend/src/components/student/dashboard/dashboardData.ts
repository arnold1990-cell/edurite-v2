import type { Bursary, RecommendationItem, StudentSubjectAchievement } from '@/types';

export const percentage = (value: unknown): number | null => typeof value === 'number' && Number.isFinite(value) ? Math.min(100, Math.max(0, value)) : null;

// NSC levels are not percentages; do not fabricate a mark when only a level exists.
export const academicMark = (subject: StudentSubjectAchievement) => percentage(subject.markPercentage);

export function upcomingBursaries(items: Bursary[], now = new Date()): Bursary[] {
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  return items.filter((item) => item.deadline && Number.isFinite(Date.parse(item.deadline)) && Date.parse(item.deadline) >= today)
    .sort((a, b) => Date.parse(a.deadline!) - Date.parse(b.deadline!)).slice(0, 3);
}

export const careerMatch = (item: RecommendationItem) => percentage(item.score);
