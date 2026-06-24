import { apiClient } from '@/services/apiClient';
import type { LocationOption } from '@/types';

export const locationService = {
  getProvinces: () => apiClient.get<LocationOption[]>('/locations/provinces').then((response) => response.data),
  getDistricts: () => apiClient.get<LocationOption[]>('/locations/districts').then((response) => response.data),
  getCircuits: (districtId: string) => apiClient.get<LocationOption[]>(`/locations/districts/${districtId}/circuits`).then((response) => response.data),
};
