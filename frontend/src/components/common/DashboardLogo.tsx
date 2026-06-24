import type { ImgHTMLAttributes } from 'react';
import dashboardLogo from '@/assets/Edurite-dashboard.jpeg';

type DashboardLogoProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'alt'>;

export const DashboardLogo = ({ className, onError, ...props }: DashboardLogoProps) => (
  <img
    {...props}
    src={dashboardLogo}
    alt="EduRite"
    className={['block w-auto object-contain', className].filter(Boolean).join(' ')}
    onError={(event) => {
      event.currentTarget.style.display = 'none';
      onError?.(event);
    }}
  />
);
