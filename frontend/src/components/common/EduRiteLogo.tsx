import type { ImgHTMLAttributes } from 'react';
import eduriteLogo from '@/assets/branding/edurite-logo.png';
import './edurite-logo.css';

type EduRiteLogoProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'alt' | 'width' | 'height' | 'size'> & {
  size?: 'small' | 'medium' | 'large';
  surface?: 'transparent' | 'light';
};

export const EduRiteLogo = ({ size = 'medium', surface = 'transparent', className = '', ...props }: EduRiteLogoProps) => (
  <img {...props} src={eduriteLogo} alt="EduRite" loading="eager"
    className={`edurite-logo edurite-logo--${size} edurite-logo--${surface} ${className}`} />
);
