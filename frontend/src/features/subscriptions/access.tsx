import { useState, type ReactNode } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/services/apiClient';
import { useAuth } from '@/hooks/useAuth';

export interface SubscriptionAccess {
  plan: 'BASIC' | 'PREMIUM' | 'PRO'; status: string; trialActive: boolean; accessAllowed: boolean; trialDaysRemaining: number;
  entitlements: string[];
  features: { id: string; label: string; minimumPlan: string }[];
  routeRules: { path: string; section: string | null; feature: string }[];
  aiUsage: { allowance: number; used: number; pending: number; remaining: number; periodStart: string; periodEnd: string };
  billingInterval: string; expiresAt?: string; trialEndDate?: string;
}
export function useSubscriptionAccess() {
  const { user } = useAuth();
  return useQuery({ queryKey: ['subscription-access', user?.id], queryFn: () => apiClient.get<SubscriptionAccess>('/subscriptions/entitlements').then(r => r.data), enabled: Boolean(user), staleTime: 0, refetchInterval: 30000 });
}
export function usePendingPayments() {
  const { user } = useAuth();
  return useQuery({ queryKey: ['pending-payments', user?.id], enabled: Boolean(user),
    queryFn: () => apiClient.get<{ paymentReference: string; provider: string; status: string; checkoutUrl?: string; formFields?: Record<string, string> }[]>('/subscriptions/pending-payments').then(r => r.data),
    staleTime: 0, refetchInterval: 10000 });
}
export function PendingPaymentNotice() {
  const pending = usePendingPayments();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const cancel = async (paymentReference: string) => {
    setBusy(true); setError('');
    try { await apiClient.post('/subscriptions/cancel', { paymentReference }); await pending.refetch(); }
    catch { setError('Could not cancel the payment. Please try again.'); }
    finally { setBusy(false); }
  };
  return <>{error && <p role="alert">{error}</p>}{pending.data?.map(payment => <aside key={payment.paymentReference} role="alert" className="my-4 rounded-xl border border-amber-400 bg-amber-50 p-4">
    <strong>Payment pending</strong><p>Complete your payment. Purchased access activates only after payment confirmation.</p>
    {payment.checkoutUrl && payment.formFields && Object.keys(payment.formFields).length > 0
      ? <form method="POST" action={payment.checkoutUrl}>{Object.entries(payment.formFields).map(([name, value]) => <input key={name} type="hidden" name={name} value={value} />)}<button className="underline" type="submit">Complete payment</button></form>
      : payment.checkoutUrl && payment.provider !== 'payfast'
        ? <a className="underline" href={payment.checkoutUrl}>Complete payment</a>
        : <p>Checkout details are unavailable. Cancel this pending payment and select your plan again.</p>}
    <button className="ml-4 underline" disabled={busy} onClick={() => cancel(payment.paymentReference)}>Cancel pending payment</button>
  </aside>)}</>;
}
export function featureForUrl(access: SubscriptionAccess | undefined, url: string) {
  const target = new URL(url, 'https://edurite.local');
  return access?.routeRules.find(rule => rule.path === target.pathname && (!rule.section || rule.section === target.searchParams.get('section')))?.feature;
}
export function FeatureLock({ feature }: { feature: string }) {
  const { data } = useSubscriptionAccess();
  const info = data?.features.find(item => item.id === feature);
  return <section className="rounded-xl border border-slate-200 bg-white p-6" aria-label="Upgrade required">
    <h2 className="text-lg font-semibold">{info?.label || 'This feature'} <span className="text-sm text-blue-700">{info?.minimumPlan}</span></h2>
    <p className="my-3">Upgrade to {info?.minimumPlan || 'a paid plan'} to unlock {info?.label.toLowerCase() || 'this feature'}.</p>
    <div className="flex gap-4"><Link className="rounded bg-blue-700 px-4 py-2 text-white" to="/student/subscription">View Plans</Link><Link className="px-4 py-2" to="/student/dashboard">Not Now</Link></div>
  </section>;
}
export function RequireEntitlement({ feature, children }: { feature: string; children: ReactNode }) {
  const access=useSubscriptionAccess();
  if (access.isPending) return <p role="status">Checking access...</p>;
  if (access.isError) return <p role="alert">Access could not be checked. <button onClick={() => access.refetch()}>Retry</button></p>;
  return access.data.entitlements.includes(feature) ? <>{children}</> : <FeatureLock feature={feature} />;
}
export function TrialNotice({ access }: { access: SubscriptionAccess }) {
  if (!access.trialActive) return null;
  return <aside role="status" className={`mb-4 rounded-xl border p-4 ${access.trialDaysRemaining <= 3 ? 'border-amber-500 bg-amber-50' : 'border-blue-200 bg-blue-50'}`}>
    <strong>Premium trial</strong><p>{access.trialDaysRemaining} days remaining</p>
    {access.trialEndDate && <p>Trial ends on {new Date(access.trialEndDate).toLocaleDateString()}</p>}
    <Link className="font-semibold text-blue-700 underline" to="/student/subscription">Upgrade</Link>
  </aside>;
}
export function TrialExpired() {
  return <section className="rounded-xl border bg-white p-6" aria-label="Free trial ended">
    <h2 className="text-lg font-semibold">Your 14-day free trial has ended.</h2>
    <p className="my-3">Choose a plan to continue using EduRite.</p>
    <Link className="inline-block rounded bg-blue-700 px-4 py-2 text-white" to="/student/subscription">View Plans</Link>
  </section>;
}
export function StudentRouteAccess({ children }: { children: ReactNode }) {
  const location=useLocation();
  const access=useSubscriptionAccess();
  const pending=usePendingPayments();
  // Account management must remain reachable if the entitlement request fails.
  if ((['/student/subscription','/student/settings','/student/notifications'].includes(location.pathname) || location.pathname.startsWith('/student/settings/'))) return <>{children}</>;
  if (access.isPending || pending.isPending) return <p role="status">Checking access...</p>;
  if (pending.isError) return <p role="alert">Payment status could not be checked. <button onClick={() => pending.refetch()}>Retry</button></p>;
  if (access.isError) return <p role="alert">Access could not be checked. <button onClick={() => access.refetch()}>Retry</button></p>;
  if (pending.data?.length) return <Navigate to="/student/subscription" replace />;
  if (location.pathname === "/student/documents" || (location.pathname === "/student/profile" && new URLSearchParams(location.search).get("section") !== "cv")) return <>{children}</>;
  if (!access.data.accessAllowed) return <TrialExpired />;
  const feature=featureForUrl(access.data, location.pathname+location.search);
  return <><TrialNotice access={access.data} />{feature && !access.data.entitlements.includes(feature) ? <FeatureLock feature={feature} /> : children}</>;
}
export function AccessBadge({ to }: { to: string }) {
  const { data }=useSubscriptionAccess();
  const feature=featureForUrl(data,to);
  const info=data?.features.find(item => item.id===feature);
  return feature && !data?.entitlements.includes(feature) ? <small className="ml-2 text-blue-700">Locked: {info?.minimumPlan}</small> : null;
}
export function AiUsageDisplay() {
  const { data }=useSubscriptionAccess();
  if (!data) return null;
  return <p className="my-3 text-sm text-slate-600">AI interactions: {data.aiUsage.used} / {data.aiUsage.allowance} used, {data.aiUsage.remaining} remaining{data.aiUsage.pending ? `, ${data.aiUsage.pending} processing` : ''}. Resets {data.aiUsage.periodEnd} (UTC). <Link className="text-blue-700" to="/student/subscription">View plans</Link></p>;
}
