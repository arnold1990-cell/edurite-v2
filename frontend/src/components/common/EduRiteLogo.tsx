import type { ComponentPropsWithoutRef } from 'react';
import eduriteLogo from '@/assets/edurite-main-logo.jpeg';
import './edurite-logo.css';

type EduRiteLogoProps = Omit<
    ComponentPropsWithoutRef<'img'>,
    'src' | 'alt' | 'width' | 'height'
> & {
    size?: 'small' | 'medium' | 'large';
    surface?: 'transparent' | 'light';
};

export const EduRiteLogo = ({
                                size = 'medium',
                                surface = 'transparent',
                                className = '',
                                ...props
                            }: EduRiteLogoProps) => (
    <img
        {...props}
        src={eduriteLogo}
        alt="EduRite"
        loading="eager"
        className={`edurite-logo edurite-logo--${size} edurite-logo--${surface} ${className}`}
    />
);
