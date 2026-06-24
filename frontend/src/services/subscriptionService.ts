import { apiClient } from '@/services/apiClient';
import type {
  PricingPlan,
  PayFastInitiatePayload,
  PayFastInitiateResponse,
  Subscription,
  SubscriptionCheckoutPayload,
  SubscriptionCheckoutResponse,
  SubscriptionPaymentCancelPayload,
  SubscriptionPaymentConfirmPayload,
  SubscriptionPaymentStatusResponse,
} from '@/types';

export const subscriptionService = {
  plans: () => apiClient.get<PricingPlan[]>('/subscriptions/plans').then((r) => r.data),
  current: () => apiClient.get<Subscription>('/subscriptions/me').then((r) => r.data),
  checkout: (payload: SubscriptionCheckoutPayload) => apiClient.post<SubscriptionCheckoutResponse>('/subscriptions/checkout', payload).then((r) => r.data),
  confirm: (payload: SubscriptionPaymentConfirmPayload) => apiClient.post<SubscriptionPaymentStatusResponse>('/subscriptions/confirm', payload).then((r) => r.data),
  cancel: (payload: SubscriptionPaymentCancelPayload) => apiClient.post<SubscriptionPaymentStatusResponse>('/subscriptions/cancel', payload).then((r) => r.data),
  payFastInitiate: (payload: PayFastInitiatePayload) => apiClient.post<PayFastInitiateResponse>('/payments/payfast/initiate', payload).then((r) => r.data),
  payFastStatus: (paymentReference: string) => apiClient.get<SubscriptionPaymentStatusResponse>('/payments/payfast/status', { params: { paymentReference } }).then((r) => r.data),
  purchase: (plan: 'BASIC' | 'PREMIUM') => apiClient.post('/subscriptions/purchase', { plan }).then((r) => r.data),
};
