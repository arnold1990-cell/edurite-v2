import { ChevronDown, Globe, Headphones } from 'lucide-react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import eduRiteLogo from '@/assets/edurite-icon.jpeg';

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

export const PublicLayout = () => {
  const { pathname } = useLocation();
  const isAuthRoute = pathname.includes('/login') || pathname.startsWith('/auth');

  return (
    <div className="min-h-screen bg-slate-50">
      <header className={`border-b border-slate-200 bg-white ${isAuthRoute ? 'shadow-[0_10px_30px_-24px_rgba(15,23,42,0.25)]' : ''}`}>
        <div
          className={`mx-auto flex w-full justify-between gap-6 ${isAuthRoute ? 'max-w-none items-end px-4 pb-0 pt-0 sm:px-6 md:px-8 lg:px-10' : 'max-w-[1280px] items-center px-4 py-4 md:px-6'}`}
          style={isAuthRoute ? { minHeight: 'clamp(140px, 16vw, 176px)' } : undefined}
        >
          <Link
            to="/"
            aria-label="EduRite home"
            className={`flex shrink-0 items-end self-end ${isAuthRoute ? 'mb-0 pb-0 pt-0' : ''}`}
          >
            <img
              src={eduRiteLogo}
              alt="EduRite"
              className={`${isAuthRoute ? 'block max-h-none max-w-none object-contain align-bottom' : 'block h-12 w-12 object-contain'}`}
              style={isAuthRoute ? { height: 'clamp(92px, 11vw, 120px)', width: 'auto', objectFit: 'contain' } : undefined}
              onError={(event) => {
                event.currentTarget.style.display = 'none';
              }}
            />
          </Link>

          <nav className={`flex items-center gap-3 sm:gap-4 ${isAuthRoute ? 'self-center' : ''}`}>
            <Link to="/auth/register/student" className="inline-flex h-11 items-center rounded-xl bg-primary-600 px-5 text-sm font-semibold text-white shadow-[0_16px_34px_-18px_rgba(37,99,235,0.85)] transition hover:bg-primary-700">
              Get Started
            </Link>
            <Link to="/about" className="hidden items-center gap-2 rounded-xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-50 md:inline-flex">
              <Headphones className="h-4 w-4 text-slate-500" />
              Help &amp; Support
            </Link>
            <button type="button" className="inline-flex items-center gap-2 rounded-xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-50">
              <Globe className="h-4 w-4 text-slate-500" />
              English
              <ChevronDown className="h-4 w-4 text-slate-400" />
            </button>
          </nav>
        </div>
      </header>

      <main className={isAuthRoute ? 'w-full' : 'mx-auto w-full max-w-[1280px] px-4 py-4 md:px-6 md:py-6'}>
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-white">
        <div className={`mx-auto w-full ${isAuthRoute ? 'max-w-none px-6 py-4 text-center text-xs text-slate-500 md:px-8 lg:px-10' : 'max-w-[1280px] px-4 py-5 md:px-6'}`}>
          {isAuthRoute ? (
            <div>© {new Date().getFullYear()} EduRite. All rights reserved.</div>
          ) : (
            <>
              <nav className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm text-slate-600">
                {footerLinks.map((item) => (
                  <Link key={item.to} to={item.to} className="hover:text-slate-900">
                    {item.label}
                  </Link>
                ))}
              </nav>
              <div className="mt-6 border-t border-slate-200 pt-4 text-xs text-slate-500">
                © {new Date().getFullYear()} EduRite. All rights reserved.
              </div>
            </>
          )}
        </div>
      </footer>
    </div>
  );
};
