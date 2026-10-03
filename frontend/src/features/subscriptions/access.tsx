import { type ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/services/apiClient';
import { useAuth } from '@/hooks/useAuth';

export interface SubscriptionAccess {
  plan: 'BASIC' | 'PREMIUM' | 'PRO'; status: string; trialActive: boolean;
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
export function StudentRouteAccess({ children }: { children: ReactNode }) {
  const location=useLocation();
  const access=useSubscriptionAccess();
  // Account management must remain reachable if the entitlement request fails.
  if (['/student/subscription','/student/settings','/student/notifications'].includes(location.pathname)) return <>{children}</>;
  if (access.isPending) return <p role="status">Checking access...</p>;
  if (access.isError) return <p role="alert">Access could not be checked. <button onClick={() => access.refetch()}>Retry</button></p>;
  const feature=featureForUrl(access.data, location.pathname+location.search);
  return feature && !access.data.entitlements.includes(feature) ? <FeatureLock feature={feature} /> : <>{children}</>;
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
