import { cn } from '@/lib/cn';

export const Badge = ({ children, color = 'slate' }: { children: React.ReactNode; color?: 'slate' | 'emerald' | 'amber' | 'blue' }) => (
  <span className={cn('rounded-full px-2.5 py-1 text-xs font-medium', color === 'emerald' && 'bg-emerald-100 text-emerald-700', color === 'amber' && 'bg-amber-100 text-amber-700', color === 'blue' && 'bg-blue-100 text-blue-700', color === 'slate' && 'bg-slate-100 text-slate-700')}>
    {children}
  </span>
);
