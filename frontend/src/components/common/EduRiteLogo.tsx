import type { ImgHTMLAttributes } from 'react';
import eduriteLogo from '@/assets/edurite-icon.jpeg';

type EduRiteLogoProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'alt'>;

export const EduRiteLogo = ({ className, ...props }: EduRiteLogoProps) => (
  <img
    src={eduriteLogo}
    alt="EduRite"
    className={className}
    loading="eager"
    {...props}
  />
);
