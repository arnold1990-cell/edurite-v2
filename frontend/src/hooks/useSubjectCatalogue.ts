import { useAppQuery } from './useAppQuery';
import { apiClient } from '@/services/apiClient';
export interface CatalogueSubject { name: string; phase: string; language: boolean; languageLevel?: string }
export function useSubjectCatalogue() {
  return useAppQuery({ queryKey: ['subject-catalogue'], queryFn: () => apiClient.get<CatalogueSubject[]>('/student/subjects').then(r => r.data), staleTime: 300_000 });
}
