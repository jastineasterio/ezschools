import { api, unwrapApiResponse } from './api';
import type { ApiResponse } from '@/types/api';
import type {
  ActiveInactiveDto,
  AnalyticsFilters,
  PackageUserDistributionDto,
  RoleDistributionDto,
  SchoolUserDistributionDto,
  TrendDto,
  UserOverviewDto,
} from '@/types/api';

export function getUserOverview(filters: AnalyticsFilters) {
  return unwrapApiResponse<UserOverviewDto>(
    api.get<ApiResponse<UserOverviewDto>>('/platform/analytics/users/overview', {
      params: { start: filters.start, end: filters.end },
    })
  );
}

export function getSchoolUserDistribution(filters: AnalyticsFilters & {
  page: number;
  size: number;
  search?: string;
  sort?: string;
}) {
  return unwrapApiResponse<{
    content: SchoolUserDistributionDto[];
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
  }>(
    api.get<ApiResponse<{
      content: SchoolUserDistributionDto[];
      totalElements: number;
      totalPages: number;
      page: number;
      size: number;
    }>>('/platform/analytics/users/schools', {
      params: {
        start: filters.start,
        end: filters.end,
        search: filters.search,
        page: filters.page,
        size: filters.size,
        sort: filters.sort,
      },
    })
  );
}

export function getPackageUserDistribution(filters: AnalyticsFilters) {
  return unwrapApiResponse<PackageUserDistributionDto[]>(
    api.get<ApiResponse<PackageUserDistributionDto[]>>(
      '/platform/analytics/users/packages',
      { params: { start: filters.start, end: filters.end } }
    )
  );
}

export function getRoleDistribution(filters: AnalyticsFilters) {
  return unwrapApiResponse<RoleDistributionDto[]>(
    api.get<ApiResponse<RoleDistributionDto[]>>('/platform/analytics/users/roles', {
      params: { start: filters.start, end: filters.end },
    })
  );
}

export function getStatusDistribution(filters: AnalyticsFilters) {
  return unwrapApiResponse<ActiveInactiveDto[]>(
    api.get<ApiResponse<ActiveInactiveDto[]>>('/platform/analytics/users/status', {
      params: { start: filters.start, end: filters.end },
    })
  );
}

export function getRegistrationTrends(filters: AnalyticsFilters & { granularity?: string }) {
  return unwrapApiResponse<TrendDto[]>(
    api.get<ApiResponse<TrendDto[]>>('/platform/analytics/users/trends', {
      params: {
        start: filters.start,
        end: filters.end,
        granularity: filters.granularity,
      },
    })
  );
}
