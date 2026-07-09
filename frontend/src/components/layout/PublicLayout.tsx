import { useEffect, useRef, useState } from 'react';
import { Check, ChevronDown, Globe, Headphones } from 'lucide-react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { EduRiteLogo } from '@/components/common/EduRiteLogo';

const footerLinks = [
  { to: '/', label: 'Home' },
  { to: '/about', label: 'About' },
  { to: '/careers', label: 'Careers' },
  { to: '/courses', label: 'e-Resources' },
  { to: '/bursaries', label: 'Bursaries' },
  { to: '/pricing', label: 'Pricing' },
  { to: '/privacy-policy', label: 'Privacy Policy' },
  { to: '/terms-and-conditions', label: 'Terms & Conditions' },
] as const;

const languageOptions = ['English', 'Setswana', 'Shona', 'Ndebele'] as const;
type LanguageOption = (typeof languageOptions)[number];

const PublicLanguageSelector = ({ compact = false }: { compact?: boolean }) => {
  const { pathname } = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const [selectedLanguage, setSelectedLanguage] = useState<LanguageOption>('English');
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setIsOpen(false);
  }, [pathname]);

  useEffect(() => {
    const handlePointerDown = (event: MouseEvent | TouchEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('touchstart', handlePointerDown);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('touchstart', handlePointerDown);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={isOpen}
        aria-label={`Selected language: ${selectedLanguage}`}
        onClick={() => setIsOpen((current) => !current)}
        className={`inline-flex items-center gap-2 rounded-xl border border-slate-200 font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-50 ${
          compact ? 'px-3 py-2 text-xs' : 'px-4 py-3 text-sm'
        }`}
      >
        <Globe className="h-4 w-4 text-slate-500" />
        {selectedLanguage}
        <ChevronDown className={`h-4 w-4 text-slate-400 transition ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {isOpen ? (
        <div
          role="menu"
          aria-label="Language selector"
          className="absolute right-0 top-[calc(100%+0.5rem)] z-50 w-44 rounded-2xl border border-slate-200 bg-white p-1.5 shadow-[0_24px_48px_-24px_rgba(15,23,42,0.3)]"
        >
          {languageOptions.map((language) => {
            const isSelected = selectedLanguage === language;
            return (
              <button
                key={language}
                type="button"
                role="menuitemradio"
                aria-checked={isSelected}
                onClick={() => {
                  setSelectedLanguage(language);
                  setIsOpen(false);
                }}
                className={`flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-left text-sm transition ${isSelected ? 'bg-primary-50 text-primary-700' : 'text-slate-700 hover:bg-slate-50'}`}
              >
                <span>{language}</span>
                {isSelected ? <Check className="h-4 w-4" /> : null}
              </button>
            );
          })}
        </div>
      ) : null}
    </div>
  );
};

const PublicFooter = ({ compact = false }: { compact?: boolean }) => (
  <footer className="border-t border-slate-200 bg-white">
    <div className={`mx-auto w-full ${compact ? 'max-w-none px-4 py-3 md:flex md:items-center md:justify-between md:px-6 lg:px-8' : 'max-w-[1280px] px-4 py-5 md:px-6'}`}>
      <nav className={`flex flex-wrap items-center justify-center text-slate-600 ${compact ? 'gap-x-4 gap-y-1 text-[11px] sm:text-xs md:justify-start' : 'gap-x-6 gap-y-2 text-sm'}`}>
        {footerLinks.map((item) => (
          <Link key={item.to} to={item.to} className="hover:text-slate-900">
            {item.label}
          </Link>
        ))}
      </nav>
      <div className={compact ? 'mt-2 text-center text-[11px] text-slate-500 md:mt-0 md:text-right' : 'mt-6 border-t border-slate-200 pt-4 text-center text-xs text-slate-500'}>
        Copyright 2026 EduRite. All rights reserved.
      </div>
    </div>
  </footer>
);

export const PublicLayout = () => {
  const { pathname } = useLocation();
  const isAuthRoute = pathname.startsWith('/auth')
    || ['/login', '/register', '/forgot-password', '/reset-password', '/verify-email', '/verify-otp'].some((segment) => pathname.includes(segment));
  const isLoginRoute = pathname.includes('/login');

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <header className={`border-b border-slate-200 bg-white ${isAuthRoute ? 'shadow-[0_10px_30px_-24px_rgba(15,23,42,0.25)]' : ''}`}>
        <div
          className={`mx-auto flex w-full justify-between ${isAuthRoute ? (isLoginRoute ? 'max-w-none items-center gap-3 px-4 py-2.5 sm:px-6 md:px-8 lg:px-10' : 'max-w-none items-end gap-6 px-4 pb-0 pt-0 sm:px-6 md:px-8 lg:px-10') : 'max-w-[1280px] items-center gap-6 px-4 py-4 md:px-6'}`}
          style={isAuthRoute ? { minHeight: isLoginRoute ? 'clamp(84px, 9vw, 104px)' : 'clamp(140px, 16vw, 176px)' } : undefined}
        >
          <Link
            to="/"
            aria-label="EduRite home"
            className={`flex shrink-0 items-center justify-center ${isAuthRoute ? (isLoginRoute ? 'min-h-[64px]' : 'mb-0 min-h-[120px] pb-0 pt-0 self-end') : 'min-h-[72px]'}`}
          >
            <EduRiteLogo
              className={`${isAuthRoute ? (isLoginRoute ? 'block h-16 w-auto object-contain' : 'block h-24 w-auto object-contain align-bottom') : 'block h-12 w-auto object-contain md:h-14'}`}
              style={isAuthRoute ? { height: isLoginRoute ? 'clamp(64px, 7vw, 82px)' : 'clamp(92px, 11vw, 120px)', width: 'auto', objectFit: 'contain' } : undefined}
            />
          </Link>

          <nav className={`flex items-center ${isLoginRoute ? 'gap-2 sm:gap-3' : 'gap-3 sm:gap-4'} ${isAuthRoute ? 'self-center' : ''}`}>
            <Link to="/auth/register/student" className={`inline-flex items-center rounded-xl bg-primary-600 font-semibold text-white shadow-[0_16px_34px_-18px_rgba(37,99,235,0.85)] transition hover:bg-primary-700 ${isLoginRoute ? 'h-10 px-4 text-xs sm:text-sm' : 'h-11 px-5 text-sm'}`}>
              Get Started
            </Link>
            <Link to="/about" className={`hidden items-center gap-2 rounded-xl border border-slate-200 font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-50 ${isLoginRoute ? 'px-3 py-2 text-xs lg:inline-flex' : 'px-4 py-3 text-sm md:inline-flex'}`}>
              <Headphones className="h-4 w-4 text-slate-500" />
              Help &amp; Support
            </Link>
            <PublicLanguageSelector compact={isLoginRoute} />
          </nav>
        </div>
      </header>

      <main className={isAuthRoute ? 'flex w-full min-h-0 flex-1 flex-col' : 'mx-auto flex w-full max-w-[1280px] flex-1 flex-col px-4 py-4 md:px-6 md:py-6'}>
        <Outlet />
      </main>

      <PublicFooter compact={isLoginRoute} />
    </div>
  );
};

