import axios, {
  type AxiosInstance,
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
} from 'axios';

import type {
  ApiError,
  ApiResponse,
  LoginResponse,
  TokenResponse,
  CurrentUserResponse,
} from '@/types/api';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';
type RetryableAxiosRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean };
let currentUserRequest: Promise<CurrentUserResponse> | null = null;

export async function unwrapApiResponse<T>(
  request: Promise<{ data: ApiResponse<T> }>
): Promise<T> {
  const response = await request;
  if (response.data.data === undefined) {
    throw new Error(response.data.message || 'The server returned an empty response');
  }
  return response.data.data;
}

export function createApiResponseClient(client: AxiosInstance) {
  const unwrap = async <T>(
    request: Promise<{ data: ApiResponse<T> }>
  ): Promise<{ data: T }> => ({
    data: await unwrapApiResponse(request),
  });

  return {
    get: <T>(url: string, config?: AxiosRequestConfig) =>
      unwrap(client.get<ApiResponse<T>>(url, config)),
    post: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
      unwrap(client.post<ApiResponse<T>>(url, body, config)),
    put: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
      unwrap(client.put<ApiResponse<T>>(url, body, config)),
    patch: <T>(url: string, body?: unknown, config?: AxiosRequestConfig) =>
      unwrap(client.patch<ApiResponse<T>>(url, body, config)),
    delete: <T = void>(url: string, config?: AxiosRequestConfig) =>
      unwrap(client.delete<ApiResponse<T>>(url, config)),
  };
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<ApiError>(error)) {
    const response = error.response?.data;
    const statusPrefix = error.response?.status
      ? `Request failed (${error.response.status}): `
      : '';
    if (response?.errors) {
      const details = Object.values(response.errors).filter(Boolean);
      if (details.length > 0) {
        return `${statusPrefix}${details.join(', ')}`;
      }
    }
    if (response?.message) {
      return `${statusPrefix}${response.message}`;
    }
    if (statusPrefix) {
      return `${statusPrefix}${error.message}`;
    }
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

export const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token');

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

api.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError<ApiError>) => {
    const originalRequest = error.config;
    const retryableRequest = originalRequest as RetryableAxiosRequestConfig | undefined;

    if (
      error.response?.status === 401 &&
      retryableRequest &&
      !retryableRequest._retry
    ) {
      retryableRequest._retry = true;

      try {
        const refreshTokenValue = localStorage.getItem('refresh_token');

        if (refreshTokenValue) {
          const response = await axios.post<ApiResponse<TokenResponse>>(
            `${API_BASE_URL}/auth/refresh`,
            {
              refreshToken: refreshTokenValue,
            }
          );

          const tokenData = response.data.data;

          if (!tokenData) {
            throw new Error('Invalid refresh token response');
          }

          localStorage.setItem('access_token', tokenData.accessToken);
          localStorage.setItem('refresh_token', tokenData.refreshToken);

          if (retryableRequest.headers) {
            retryableRequest.headers.Authorization =
              `Bearer ${tokenData.accessToken}`;
          }

          return api(retryableRequest);
        }
      } catch (refreshError) {
        if (
          axios.isAxiosError(refreshError) &&
          refreshError.response?.status === 401
        ) {
          clearTokens();
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export async function login(
  email: string,
  password: string
): Promise<LoginResponse> {
  const response = await api.post<ApiResponse<LoginResponse>>(
    '/auth/login',
    {
      email,
      password,
    }
  );

  const loginData = response.data.data;

  if (!loginData) {
    throw new Error(response.data.message || 'Login failed');
  }

  setTokens(loginData.accessToken, loginData.refreshToken);

  return loginData;
}

export async function refreshToken(
  refreshTokenValue: string
): Promise<TokenResponse> {
  const response = await api.post<ApiResponse<TokenResponse>>(
    '/auth/refresh',
    {
      refreshToken: refreshTokenValue,
    }
  );

  const tokenData = response.data.data;

  if (!tokenData) {
    throw new Error(response.data.message || 'Token refresh failed');
  }

  return tokenData;
}

export async function logout(): Promise<void> {
  try {
    await api.post('/auth/logout');
  } finally {
    clearTokens();
  }
}

export function getCurrentUser(): Promise<CurrentUserResponse> {
  if (!currentUserRequest) {
    const request = api
      .get<ApiResponse<CurrentUserResponse>>('/auth/me')
      .then((response) => {
        if (!response.data.data) {
          throw new Error(
            response.data.message || 'Failed to fetch current user'
          );
        }

        return response.data.data;
      })
      .finally(() => {
        if (currentUserRequest === request) {
          currentUserRequest = null;
        }
      });
    currentUserRequest = request;
  }

  return currentUserRequest;
}

export function setTokens(
  accessToken: string,
  refreshToken: string
): void {
  currentUserRequest = null;
  localStorage.setItem('access_token', accessToken);
  localStorage.setItem('refresh_token', refreshToken);
}

export function clearTokens(): void {
  currentUserRequest = null;
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
}

export function getAccessToken(): string | null {
  return localStorage.getItem('access_token');
}

export function getRefreshToken(): string | null {
  return localStorage.getItem('refresh_token');
}

export function isAuthenticated(): boolean {
  return !!getAccessToken();
}
