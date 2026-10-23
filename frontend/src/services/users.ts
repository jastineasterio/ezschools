import { api, unwrapApiResponse } from './api';
import type {
  ApiResponse,
  CreateSchoolUserRequest,
  SchoolUserResponse,
} from '@/types/api';

export interface SchoolUserPage {
  content: SchoolUserResponse[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface SchoolUserFilters {
  page: number;
  size: number;
  search?: string;
  role?: string;
  active?: boolean;
}

export function getSchoolUsers(filters: SchoolUserFilters) {
  return unwrapApiResponse(
    api.get<ApiResponse<SchoolUserPage>>('/school/users', { params: filters })
  );
}

export function createSchoolUser(request: CreateSchoolUserRequest) {
  return unwrapApiResponse(
    api.post<ApiResponse<SchoolUserResponse>>('/school/users', request)
  );
}
