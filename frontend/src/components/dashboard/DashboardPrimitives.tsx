import type { ReactNode } from 'react';
import { ArrowRight, ChevronRight, type LucideIcon } from 'lucide-react';
import { cn } from '@/lib/cn';

const toneClasses: Record<string, string> = {
  positive: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  warning: 'border-amber-200 bg-amber-50 text-amber-700',
  critical: 'border-rose-200 bg-rose-50 text-rose-700',
  info: 'border-blue-200 bg-blue-50 text-blue-700',
  neutral: 'border-slate-200 bg-slate-100 text-slate-700',
};

const normalizeTone = (tone?: string) => {
  if (!tone) return 'neutral';
  const lower = tone.toLowerCase();
  if (lower.includes('green') || lower.includes('positive') || lower.includes('success') || lower.includes('on track')) return 'positive';
  if (lower.includes('amber') || lower.includes('warning') || lower.includes('behind')) return 'warning';
  if (lower.includes('red') || lower.includes('critical') || lower.includes('risk')) return 'critical';
  if (lower.includes('info') || lower.includes('blue')) return 'info';
  return 'neutral';
};

export const DashboardShell = ({ children, className }: { children: ReactNode; className?: string }) => (
  <section className={cn('space-y-4', className)}>{children}</section>
);

export const DashboardKpiCard = ({
  icon: Icon,
  label,
  value,
  helperText,
  actionLabel,
  tone,
  className,
}: {
  icon?: LucideIcon;
  label: string;
  value: string;
  helperText?: string;
  actionLabel?: string;
  tone?: string;
  className?: string;
}) => {
  const normalizedTone = normalizeTone(tone);
  return (
    <article className={cn('flex min-h-[104px] flex-col justify-between rounded-2xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-200/60', className)}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-[12px] font-semibold uppercase tracking-[0.16em] text-slate-500">{label}</p>
          <p className="mt-2 truncate text-[24px] font-semibold leading-none text-slate-950">{value}</p>
        </div>
        {Icon ? (
          <div className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border', toneClasses[normalizedTone])}>
            <Icon className="h-4 w-4" />
          </div>
        ) : null}
      </div>
      <div className="mt-3 flex items-center justify-between gap-2">
        <p className="line-clamp-2 text-[12px] leading-5 text-slate-500">{helperText || 'Live dashboard metric.'}</p>
        {actionLabel ? (
          <span className="inline-flex shrink-0 items-center gap-1 text-[12px] font-semibold text-blue-700">
            {actionLabel}
            <ArrowRight className="h-3.5 w-3.5" />
          </span>
        ) : null}
      </div>
    </article>
  );
};

export const DashboardSectionCard = ({
  title,
  subtitle,
  action,
  children,
  className,
  contentClassName,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
  contentClassName?: string;
}) => (
  <section className={cn('rounded-2xl border border-slate-200 bg-white shadow-sm shadow-slate-200/60', className)}>
    <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 px-4 py-3.5">
      <div className="min-w-0">
        <h3 className="text-[15px] font-semibold text-slate-950">{title}</h3>
        {subtitle ? <p className="mt-1 text-[12px] leading-5 text-slate-500">{subtitle}</p> : null}
      </div>
      {action}
    </div>
    <div className={cn('p-4', contentClassName)}>{children}</div>
  </section>
);

export const CompactDataTable = ({
  columns,
  emptyState,
  children,
  className,
}: {
  columns: string[];
  emptyState?: ReactNode;
  children: ReactNode;
  className?: string;
}) => (
  <div className={cn('overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm shadow-slate-200/60', className)}>
    <div className="overflow-x-auto">
      <table className="min-w-full text-left text-[13px] text-slate-700">
        <thead className="bg-slate-50 text-[12px] font-semibold uppercase tracking-[0.12em] text-slate-500">
          <tr>
            {columns.map((column) => <th key={column} className="px-4 py-3">{column}</th>)}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">{children}</tbody>
      </table>
    </div>
    {emptyState}
  </div>
);

export const RiskBadge = ({ label, className }: { label: string; className?: string }) => {
  const tone = normalizeTone(label);
  return (
    <span className={cn('inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-semibold', toneClasses[tone], className)}>
      {label}
    </span>
  );
};

export const ProgressBar = ({
  value,
  tone,
  label,
  className,
}: {
  value: number;
  tone?: string;
  label?: string;
  className?: string;
}) => {
  const normalizedTone = normalizeTone(tone);
  const barClass = normalizedTone === 'positive'
    ? 'bg-emerald-500'
    : normalizedTone === 'warning'
      ? 'bg-amber-500'
      : normalizedTone === 'critical'
        ? 'bg-rose-500'
        : 'bg-blue-600';
  const safeValue = Math.max(0, Math.min(100, value));
  return (
    <div className={cn('space-y-1.5', className)}>
      {label ? (
        <div className="flex items-center justify-between gap-2 text-[12px] text-slate-500">
          <span>{label}</span>
          <span className="font-semibold text-slate-700">{safeValue}%</span>
        </div>
      ) : null}
      <div className="h-2 rounded-full bg-slate-100">
        <div className={cn('h-2 rounded-full', barClass)} style={{ width: `${Math.max(6, safeValue)}%` }} />
      </div>
    </div>
  );
};

export const QuickActionButton = ({
  icon: Icon,
  label,
  helperText,
  onClick,
  className,
}: {
  icon?: LucideIcon;
  label: string;
  helperText?: string;
  onClick?: () => void;
  className?: string;
}) => (
  <button
    type="button"
    onClick={onClick}
    className={cn('flex min-h-[76px] w-full items-start justify-between rounded-2xl border border-slate-200 bg-white p-4 text-left shadow-sm shadow-slate-200/50 transition hover:border-blue-200 hover:bg-blue-50/50', className)}
  >
    <div className="min-w-0">
      <p className="text-[13px] font-semibold text-slate-950">{label}</p>
      {helperText ? <p className="mt-1 text-[12px] leading-5 text-slate-500">{helperText}</p> : null}
    </div>
    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-blue-100 bg-blue-50 text-blue-700">
      {Icon ? <Icon className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
    </div>
  </button>
);

export const EmptyStateCompact = ({
  title,
  message,
  action,
  className,
}: {
  title: string;
  message: string;
  action?: ReactNode;
  className?: string;
}) => (
  <div className={cn('rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-5', className)}>
    <p className="text-[13px] font-semibold text-slate-900">{title}</p>
    <p className="mt-1 text-[12px] leading-5 text-slate-500">{message}</p>
    {action ? <div className="mt-3">{action}</div> : null}
  </div>
);
