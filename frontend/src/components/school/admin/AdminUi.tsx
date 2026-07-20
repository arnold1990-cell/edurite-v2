import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { Button } from '@/components/ui/Button';

export const adminToneClass = (tone?: string) => {
  if (tone === 'positive') return 'border-emerald-200 bg-emerald-50 text-emerald-700';
  if (tone === 'warning') return 'border-amber-200 bg-amber-50 text-amber-700';
  if (tone === 'critical') return 'border-rose-200 bg-rose-50 text-rose-700';
  if (tone === 'info') return 'border-primary-200 bg-primary-50 text-primary-700';
  return 'border-slate-200 bg-slate-100 text-slate-700';
};

export const AdminPageLayout = ({ children, className }: { children: ReactNode; className?: string }) => (
  <section className={cn('space-y-4', className)}>{children}</section>
);

export const AdminPageHeader = ({
  title,
  subtitle,
  actions,
}: {
  title: string;
  subtitle: string;
  actions?: ReactNode;
}) => (
  <div className="flex flex-wrap items-start justify-between gap-4">
    <div className="min-w-0">
      <h2 className="text-[24px] font-bold leading-tight text-slate-950 md:text-[26px]">{title}</h2>
      <p className="mt-1.5 max-w-4xl text-[14px] leading-6 text-slate-600 md:text-[15px]">{subtitle}</p>
    </div>
    {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
  </div>
);

export const AdminCard = ({ children, className }: { children: ReactNode; className?: string }) => (
  <section className={cn('rounded-[16px] border border-[#e5edf7] bg-white p-4 shadow-sm shadow-slate-200/40', className)}>{children}</section>
);

export const AdminMetricCard = ({
  label,
  value,
  helperText,
  tone = 'neutral',
  icon,
  trendLabel,
}: {
  label: string;
  value: string;
  helperText: string;
  tone?: string;
  icon?: ReactNode;
  trendLabel?: string;
}) => (
  <AdminCard className="p-[16px]">
    <div className="flex items-start justify-between gap-3">
      <div className="flex min-w-0 items-start gap-3">
        {icon ? (
          <div className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border', adminToneClass(tone))}>
            {icon}
          </div>
        ) : null}
        <div className="min-w-0">
          <p className="text-[13px] font-semibold text-slate-600">{label}</p>
          <p className="mt-2 text-[24px] font-bold leading-none text-slate-950 md:text-[28px]">{value}</p>
        </div>
      </div>
      {trendLabel ? <AdminBadge label={trendLabel} tone={tone} /> : null}
    </div>
    <p className="mt-3 text-[13px] leading-5 text-slate-500">{helperText}</p>
  </AdminCard>
);

export const AdminFilterBar = ({ children, className }: { children: ReactNode; className?: string }) => (
  <AdminCard className={cn('p-4', className)}>
    <div className="grid gap-3">{children}</div>
  </AdminCard>
);

export const AdminDataTable = ({
  title,
  action,
  children,
  className,
}: {
  title: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) => (
  <section className={cn('min-w-0 overflow-hidden rounded-[16px] border border-[#e5edf7] bg-white shadow-sm shadow-slate-200/40', className)}>
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-5 py-4">
      <h3 className="text-[20px] font-semibold text-slate-950">{title}</h3>
      {action}
    </div>
    {children}
  </section>
);

export const AdminRightPanel = ({ children, className }: { children: ReactNode; className?: string }) => (
  <aside className={cn('min-w-0 space-y-4 self-start xl:w-[300px]', className)}>{children}</aside>
);

export const AdminBadge = ({
  label,
  tone = 'neutral',
  className,
}: {
  label: string;
  tone?: string;
  className?: string;
}) => (
  <span className={cn('inline-flex items-center rounded-full border px-2.5 py-1 text-[12px] font-semibold', adminToneClass(tone), className)}>
    {label}
  </span>
);

export const AdminActionButton = ({
  children,
  variant = 'secondary',
  className,
  ...props
}: React.ComponentProps<typeof Button> & { variant?: 'primary' | 'secondary' | 'ghost' | 'warning' | 'danger' }) => {
  const variantClass =
    variant === 'primary'
      ? 'bg-blue-600 hover:bg-blue-700 text-white'
      : variant === 'ghost'
        ? 'bg-slate-100 text-slate-900 hover:bg-slate-200'
        : variant === 'warning'
          ? 'bg-amber-500 hover:bg-amber-600 text-white'
          : variant === 'danger'
            ? 'bg-rose-600 hover:bg-rose-700 text-white'
            : 'border border-slate-200 bg-white text-slate-900 hover:bg-slate-50';
  return <Button className={cn('h-9 rounded-xl px-3 text-[13px] font-semibold shadow-none', variantClass, className)} {...props}>{children}</Button>;
};



