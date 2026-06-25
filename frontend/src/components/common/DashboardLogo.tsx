import type { ImgHTMLAttributes } from 'react';
import eduriteLogo from '@/assets/Edurite-dashboard.jpeg';

type DashboardLogoProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'alt'>;

export const DashboardLogo = ({ className, ...props }: DashboardLogoProps) => (
  <img
    src={eduriteLogo}
    alt="EduRite Dashboard"
    className={className}
    loading="eager"
    {...props}
  />
);
