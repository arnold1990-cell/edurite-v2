import type { ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

export const Button = ({ className, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) => (
  <button
    className={cn(
      'w-full rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary-500 disabled:opacity-50 sm:w-auto',
      className,
    )}
    {...props}
  />
);
