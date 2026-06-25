import type { ImgHTMLAttributes } from 'react';
import eduriteLogo from '@/assets/edurite-icon.jpeg';

type DashboardLogoProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'alt'>;

export const DashboardLogo = ({ className, onError, ...props }: DashboardLogoProps) => (
  <img
    {...props}
    src={eduriteLogo}
    alt="EduRite - Education done right"
    loading="lazy"
    className={['block w-auto object-contain', className].filter(Boolean).join(' ')}
    onError={(event) => {
      event.currentTarget.style.display = 'none';
      onError?.(event);
    }}
  />
);
