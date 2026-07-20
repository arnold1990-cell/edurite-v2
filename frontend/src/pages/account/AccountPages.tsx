import { useEffect, useMemo, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { authStore } from '@/features/auth/authStore';
import { useAuth } from '@/hooks/useAuth';
import { ACCOUNT_ENDPOINTS, accountService } from '@/services/accountService';
import { normalizeAuthResponse } from '@/services/authService';

const homeByRole = {
  STUDENT: '/student/settings',
  COMPANY: '/company/settings',
  ADMIN: '/admin/settings',
  DISTRICT_ADMIN: '/district/dashboard',
  DISTRICT_DIRECTOR: '/district/dashboard',
  CIRCUIT_MANAGER: '/district/dashboard',
  SUBJECT_ADVISOR: '/district/advisor/dashboard',
  SCHOOL_ADMIN: '/school/dashboard',
  TEACHER: '/teacher/dashboard',
  SCHOOL_STUDENT: '/school-student/dashboard',
} as const;

export const AccountChangePasswordPage = () => {
  const { getPrimaryRole, user } = useAuth();
  const role = getPrimaryRole() ?? 'STUDENT';
  const backPath = homeByRole[role];
  const forcePasswordChange = Boolean(user?.passwordChangeRequired);
  const usesDistrictPortal = role === 'DISTRICT_ADMIN' || role === 'DISTRICT_DIRECTOR' || role === 'CIRCUIT_MANAGER' || role === 'SUBJECT_ADVISOR';
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [toastMessage, setToastMessage] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const backLabel = useMemo(() => {
    if (usesDistrictPortal) {
      return 'Back to District Portal';
    }
    return 'Back to settings';
  }, [usesDistrictPortal]);

  useEffect(() => {
    if (!toastMessage) {
      return undefined;
    }

    const timeout = window.setTimeout(() => setToastMessage(''), 2400);
    return () => window.clearTimeout(timeout);
  }, [toastMessage]);

  const requestOtp = useMutation({
    mutationFn: accountService.requestPasswordChangeOtp,
    onSuccess: (response) => {
      setErrorMessage('');
      setMessage(response.message || 'Password change OTP sent.');
    },
    onError: (error) => {
      setMessage('');
      setErrorMessage(error instanceof Error ? error.message : 'Unable to send OTP.');
    },
  });

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage('');
    setMessage('');

    if (!currentPassword.trim()) {
      setErrorMessage('Current password is required.');
      return;
    }
    if (!forcePasswordChange && !code.trim()) {
      setErrorMessage('OTP code is required.');
      return;
    }
    if (!newPassword.trim()) {
      setErrorMessage('New password is required.');
      return;
    }
    if (!confirmNewPassword.trim()) {
      setErrorMessage('Confirm new password is required.');
      return;
    }
    if (newPassword !== confirmNewPassword) {
      setErrorMessage('New password and confirm password must match.');
      return;
    }

    void (async () => {
      setIsSubmitting(true);
      try {
        const endpoint = forcePasswordChange ? ACCOUNT_ENDPOINTS.firstLoginChangePassword : ACCOUNT_ENDPOINTS.changePasswordWithOtp;
        const firstLoginPayload = {
          currentPassword,
          newPassword,
          confirmNewPassword,
        };
        const otpPayload = {
          currentPassword,
          code: code.trim(),
          newPassword,
        };
        if (import.meta.env.DEV) {
          console.info('[account/change-password] submitting password change', {
            endpoint,
            payloadKeys: forcePasswordChange ? Object.keys(firstLoginPayload) : Object.keys(otpPayload),
            userRole: role,
            tokenExists: Boolean(authStore.getAccessToken()),
          });
        }
        const response = forcePasswordChange
          ? await accountService.forcePasswordChange(firstLoginPayload)
          : await accountService.changePasswordWithOtp(otpPayload);
        const normalizedAuth = normalizeAuthResponse(response.data);
        const successMessage = normalizedAuth.message || 'Password changed successfully.';
        const rememberMe = authStore.shouldPersistSession();
        if (import.meta.env.DEV) {
          console.info('[account/change-password] password change succeeded', {
            endpoint,
            userRole: role,
            forcePasswordChange,
            tokenExists: Boolean(authStore.getAccessToken()),
            responseStatus: response.status,
          });
        }
        setErrorMessage('');
        setMessage(successMessage);
        setToastMessage(successMessage);
        setCode('');
        setCurrentPassword('');
        setNewPassword('');
        setConfirmNewPassword('');
        authStore.setTokens(normalizedAuth.accessToken, normalizedAuth.refreshToken, rememberMe);
        authStore.setUser({
          ...normalizedAuth.user,
          passwordChangeRequired: normalizedAuth.mustChangePassword ?? normalizedAuth.user.passwordChangeRequired ?? false,
        }, rememberMe);
        if (role === 'DISTRICT_ADMIN') {
          window.setTimeout(() => window.location.replace('/district/dashboard'), 900);
        } else {
          window.setTimeout(() => window.location.replace(backPath), 900);
        }
      } catch (error) {
        if (import.meta.env.DEV) {
          const responseStatus = typeof error === 'object' && error !== null && 'status' in error ? (error as { status?: number }).status : undefined;
          console.error('[account/change-password] password change failed', {
            endpoint: forcePasswordChange ? ACCOUNT_ENDPOINTS.firstLoginChangePassword : ACCOUNT_ENDPOINTS.changePasswordWithOtp,
            userRole: role,
            forcePasswordChange,
            tokenExists: Boolean(authStore.getAccessToken()),
            responseStatus,
            error,
          });
        }
        setMessage('');
        setErrorMessage(error instanceof Error ? error.message : 'Unable to change password.');
      } finally {
        setIsSubmitting(false);
      }
    })();
  };

  return (
    <section className="space-y-6">
      {toastMessage ? (
        <div className="fixed right-4 top-4 z-50 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800 shadow-lg shadow-emerald-100">
          {toastMessage}
        </div>
      ) : null}
      <div>
        <h1 className="text-2xl font-bold">Security / Change Password</h1>
        <p className="text-sm text-slate-600">
          {forcePasswordChange
            ? 'First login detected. Change your temporary password before continuing into the District Portal.'
            : 'Update your password securely. EduRite will request OTP verification unless your account is completing a first-login reset.'}
        </p>
      </div>
      <div className="card space-y-4 p-5">
        <div className="rounded border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-900">
          {forcePasswordChange ? (
            <>
              <p>First login detected.</p>
              <p>Change your temporary password before accessing the rest of the portal.</p>
            </>
          ) : (
            <>
              <p>Step 1: Request OTP.</p>
              <p>Step 2: Enter current password, OTP, and new password.</p>
            </>
          )}
        </div>
        <div className="flex flex-wrap gap-3">
          {!forcePasswordChange ? (
            <Button
              type="button"
              onClick={() => requestOtp.mutate()}
              disabled={requestOtp.isPending || isSubmitting}
            >
              {requestOtp.isPending ? 'Sending OTP...' : 'Send OTP'}
            </Button>
          ) : null}
          <Link to={backPath} className="inline-flex items-center rounded-md border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50">
            {backLabel}
          </Link>
        </div>
        <form className="grid gap-4 md:grid-cols-2" onSubmit={handleSubmit}>
          <label className="text-sm font-medium text-slate-700 md:col-span-2">
            Current password
            <Input
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5"
              autoComplete="current-password"
              required
            />
          </label>
          {!forcePasswordChange ? (
            <label className="text-sm font-medium text-slate-700">
              OTP code
              <Input
                value={code}
                onChange={(event) => setCode(event.target.value)}
                className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5"
                placeholder="Enter OTP"
                required
              />
            </label>
          ) : null}
          <label className="text-sm font-medium text-slate-700">
            New password
            <Input
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5"
              autoComplete="new-password"
              required
            />
          </label>
          <label className="text-sm font-medium text-slate-700 md:col-span-2">
            Confirm new password
            <Input
              type="password"
              value={confirmNewPassword}
              onChange={(event) => setConfirmNewPassword(event.target.value)}
              className="mt-2 rounded-2xl border-slate-200 bg-slate-50 px-4 py-3.5"
              autoComplete="new-password"
              required
            />
          </label>
          <div className="md:col-span-2">
            <Button type="submit" disabled={isSubmitting || requestOtp.isPending}>
              {isSubmitting ? 'Changing password...' : 'Change password'}
            </Button>
          </div>
        </form>
        {message ? <p className="text-sm text-emerald-700">{message}</p> : null}
        {errorMessage ? <p className="text-sm text-red-600">{errorMessage}</p> : null}
      </div>
    </section>
  );
};
