const normalizeBillingInterval = (rawInterval?: string) => {
  const normalized = (rawInterval ?? '').trim().toLowerCase();
  if (!normalized) return 'month';
  if (normalized === 'monthly') return 'month';
  if (normalized === 'yearly' || normalized === 'annually' || normalized === 'annual') return 'year';
  if (normalized === 'weekly') return 'week';
  if (normalized === 'daily') return 'day';
  return normalized;
};

const formatAmount = (amount: number) => (
  Number.isInteger(amount) ? String(amount) : amount.toFixed(2)
);

export const formatPlanPrice = (amount: number, currency: string, billingInterval?: string) => {
  if (!Number.isFinite(amount) || amount <= 0) return 'Free';

  const intervalLabel = normalizeBillingInterval(billingInterval);
  const currencyCode = (currency ?? '').trim().toUpperCase();
  const amountLabel = formatAmount(amount);
  if (currencyCode === 'ZAR') {
    return `R${amountLabel} / ${intervalLabel}`;
  }

  return `${currencyCode || 'USD'} ${amountLabel} / ${intervalLabel}`;
};
