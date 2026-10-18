import axios, {
  type AxiosInstance,
  type AxiosError,
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

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !(originalRequest as any)._retry
    ) {
      (originalRequest as any)._retry = true;

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

          if (originalRequest.headers) {
            originalRequest.headers.Authorization =
              `Bearer ${tokenData.accessToken}`;
          }

          return api(originalRequest);
        }
      } catch {
        clearTokens();
        window.location.href = '/login';
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

export async function getCurrentUser(): Promise<CurrentUserResponse> {
  const response = await api.get<ApiResponse<CurrentUserResponse>>('/auth/me');

  if (!response.data.data) {
    throw new Error(response.data.message || 'Failed to fetch current user');
  }

  return response.data.data;
}

export function setTokens(
  accessToken: string,
  refreshToken: string
): void {
  localStorage.setItem('access_token', accessToken);
  localStorage.setItem('refresh_token', refreshToken);
}

export function clearTokens(): void {
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
