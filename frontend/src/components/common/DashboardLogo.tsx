import type { ImgHTMLAttributes } from 'react';
import eduriteLogo from '@/assets/Edurite-dashboard.jpeg';

type DashboardLogoProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'alt'>;

export const DashboardLogo = ({ className, onError, ...props }: DashboardLogoProps) => (
  <img
    {...props}
    src={eduriteLogo}
    alt="EduRite"
    loading="lazy"
    className={['block h-auto w-auto max-w-full object-contain', className].filter(Boolean).join(' ')}
    onError={(event) => {
      event.currentTarget.style.display = 'none';
      onError?.(event);
    }}
  />
);
