import { useQuery, type UseQueryOptions } from '@tanstack/react-query';

export const useAppQuery = <TData,>(options: UseQueryOptions<TData>) => useQuery(options);
