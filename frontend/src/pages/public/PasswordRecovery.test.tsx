import { describe, expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server';
import { ForgotPasswordPage, ResetPasswordPage } from './AuthPages';
vi.mock('@/hooks/useAuth', () => ({ useAuth: () => ({}) }));

function render(path: string, page: React.ReactNode, state?: object) {
  return renderToStaticMarkup(<StaticRouter location={{ pathname: path, state }}>{page}</StaticRouter>);
}
describe('email password recovery forms', () => {
  it.each(['/auth/forgot-password', '/company/forgot-password', '/admin/forgot-password', '/district/forgot-password'])('requests email without a phone number at %s', path => {
    const html = render(path, <ForgotPasswordPage />);
    expect(html).toContain('Enter your registered email address.');
    expect(html).toContain('We will send you a verification code.');
    expect(html).toContain('type="email"');
    expect(html).toContain('Send verification code');
    expect(html).not.toContain('type="tel"');
    expect(html).not.toContain('Phone number');
  });
  it('requires a six-digit code before showing password fields', () => {
    const html = render('/auth/reset-password', <ResetPasswordPage />, { email: 'student@example.com' });
    expect(html).toContain('Enter verification code');
    expect(html).toContain('autoComplete="one-time-code"');
    expect(html).toContain('pattern="[0-9]{6}"');
    expect(html).not.toContain('type="password"');
    expect(html).not.toContain('type="tel"');
    expect(html).toContain('Codes expire after 10 minutes.');
  });
});
