import { QueryClient, QueryFunction } from '@tanstack/react-query';
import { api } from '@/services/api';
import type { ApiError } from '@/types/api';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

export type QueryKeyFactory = readonly unknown[];

export function createQueryFn<T>(path: string, options?: { method?: string; body?: unknown }): QueryFunction<T> {
  return async () => {
    const method = options?.method || 'GET';
    if (method === 'GET') {
      const { data } = await api.get<T>(path);
      return data;
    }
    if (method === 'POST') {
      const { data } = await api.post<T>(path, options?.body);
      return data;
    }
    if (method === 'PUT') {
      const { data } = await api.put<T>(path, options?.body);
      return data;
    }
    if (method === 'PATCH') {
      const { data } = await api.patch<T>(path, options?.body);
      return data;
    }
    if (method === 'DELETE') {
      const { data } = await api.delete<T>(path);
      return data;
    }
    throw new Error(`Unsupported method: ${method}`);
  };
}

export function handleApiError(error: unknown): never {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as { response: { data: ApiError } };
    const apiError = axiosError.response.data;
    throw new Error(apiError.message || 'An error occurred');
  }
  if (error instanceof Error) {
    throw error;
  }
  throw new Error('An unexpected error occurred');
}
