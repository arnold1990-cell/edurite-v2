const safeDestination = (value: string | null | undefined) =>
  value?.startsWith('/') && !value.startsWith('//') && !/[\\\r\n]/.test(value) ? value : '';
const keyFor = (email: string) => `verification-destination:${email.trim().toLowerCase()}`;

export function verificationNoticePath(email: string, destination?: string) {
  const next = safeDestination(destination);
  // Email links commonly open another tab. Only the intended path is stored, never credentials.
  try { localStorage.setItem(keyFor(email), next); } catch { /* URL still preserves the destination. */ }
  return `/verify-email/notice?${new URLSearchParams({ email, next })}`;
}

export function verificationDestination(email: string, next?: string | null) {
  let stored = '';
  try {
    stored = localStorage.getItem(keyFor(email)) || '';
    localStorage.removeItem(keyFor(email));
  } catch { /* Storage may be disabled. */ }
  return safeDestination(next || stored);
}
