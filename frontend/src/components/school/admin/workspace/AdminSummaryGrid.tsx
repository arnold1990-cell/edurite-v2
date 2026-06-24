import {
  BookOpen,
  ClipboardCheck,
  FileBarChart2,
  FileText,
  GraduationCap,
  Send,
  Users,
  UserSquare2,
  type LucideIcon,
} from 'lucide-react';
import type { SchoolAdminSummary } from './types';

type SummaryCard = { label: string; value: number; icon: LucideIcon; accent: string };

export const AdminSummaryGrid = ({ summary }: { summary: SchoolAdminSummary }) => {
  const cards: SummaryCard[] = [
    { label: 'Total Classes', value: summary.totalClasses, icon: GraduationCap, accent: 'from-blue-600 to-indigo-600' },
    { label: 'Total Subjects', value: summary.totalSubjects, icon: BookOpen, accent: 'from-indigo-600 to-violet-600' },
    { label: 'Total Teachers', value: summary.totalTeachers, icon: UserSquare2, accent: 'from-cyan-600 to-blue-600' },
    { label: 'Total Learners', value: summary.totalLearners, icon: Users, accent: 'from-purple-600 to-indigo-600' },
    { label: 'Pending Tasks', value: summary.pendingTasks, icon: ClipboardCheck, accent: 'from-amber-500 to-orange-500' },
    { label: 'Notes', value: summary.totalNotes, icon: FileText, accent: 'from-emerald-600 to-teal-600' },
    { label: 'Submissions', value: summary.totalSubmissions, icon: Send, accent: 'from-sky-600 to-blue-600' },
    { label: 'Reports', value: summary.totalReports, icon: FileBarChart2, accent: 'from-slate-700 to-indigo-700' },
  ];

  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {cards.map((card) => {
        const Icon = card.icon;
        return (
          <article key={card.label} className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{card.label}</p>
              <span className={`inline-flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br ${card.accent} text-white`}>
                <Icon className="h-4 w-4" />
              </span>
            </div>
            <p className="mt-2 text-2xl font-bold text-slate-900">{card.value}</p>
          </article>
        );
      })}
    </div>
  );
};
