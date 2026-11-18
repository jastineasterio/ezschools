import { api, unwrapApiResponse } from './api';
import type {
  ApiResponse,
  AuditChartPoint,
  AuditDistributionItem,
  AuditFilters,
  AuditLogResponse,
  AuditOverview,
  PaginatedResponse,
} from '@/types/api';

export function getAuditLogs(filters: AuditFilters) {
  return unwrapApiResponse<PaginatedResponse<AuditLogResponse>>(
    api.get<ApiResponse<PaginatedResponse<AuditLogResponse>>>('/platform/audit-logs', {
      params: {
        page: filters.page,
        size: filters.size,
        search: filters.search,
        action: filters.action,
        entityType: filters.entityType,
        result: filters.result,
        actor: filters.actor,
        school: filters.school,
        start: filters.start,
        end: filters.end,
      },
    })
  );
}

export function getAuditOverview(
  start?: string,
  end?: string,
  search?: string,
  action?: string,
  entityType?: string,
  result?: string,
  actor?: number | null,
  school?: number | null,
) {
  return unwrapApiResponse<AuditOverview>(
    api.get<ApiResponse<AuditOverview>>('/platform/audit-logs/overview', {
      params: { start, end, search, action, entityType, result, actor, school },
    })
  );
}

export function getAuditLog(id: number) {
  return unwrapApiResponse<AuditLogResponse>(
    api.get<ApiResponse<AuditLogResponse>>(`/platform/audit-logs/${id}`)
  );
}

export function getAuditTrend(
  start?: string,
  end?: string,
  search?: string,
  action?: string,
  entityType?: string,
  result?: string,
  actor?: number | null,
  school?: number | null,
) {
  return unwrapApiResponse<AuditChartPoint[]>(
    api.get<ApiResponse<AuditChartPoint[]>>('/platform/audit-logs/charts/trend', {
      params: { start, end, search, action, entityType, result, actor, school },
    })
  );
}

export function getAuditActionDistribution(
  start?: string,
  end?: string,
  search?: string,
  action?: string,
  entityType?: string,
  result?: string,
  actor?: number | null,
  school?: number | null,
) {
  return unwrapApiResponse<AuditDistributionItem[]>(
    api.get<ApiResponse<AuditDistributionItem[]>>('/platform/audit-logs/charts/actions', {
      params: { start, end, search, action, entityType, result, actor, school },
    })
  );
}

export function getAuditResultDistribution(
  start?: string,
  end?: string,
  search?: string,
  action?: string,
  entityType?: string,
  result?: string,
  actor?: number | null,
  school?: number | null,
) {
  return unwrapApiResponse<AuditDistributionItem[]>(
    api.get<ApiResponse<AuditDistributionItem[]>>('/platform/audit-logs/charts/results', {
      params: { start, end, search, action, entityType, result, actor, school },
    })
  );
}
