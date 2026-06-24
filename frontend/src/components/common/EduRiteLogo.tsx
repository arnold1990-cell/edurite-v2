import type { ImgHTMLAttributes } from 'react';
import eduRiteLogo from '@/assets/Edurite-dashboard.jpeg';

type EduRiteLogoProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'alt'>;

export const EduRiteLogo = ({ className, onError, ...props }: EduRiteLogoProps) => (
  <img
    {...props}
    src={eduRiteLogo}
    alt="EduRite"
    className={['block w-auto object-contain', className].filter(Boolean).join(' ')}
    onError={(event) => {
      event.currentTarget.style.display = 'none';
      onError?.(event);
    }}
  />
);
