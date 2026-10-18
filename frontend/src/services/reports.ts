import { api } from './api';
import type { PaginatedResponse } from '@/types/api';
import type { ReportJob, CreateReportJobRequest } from '@/types/reports';

export async function getReports(page = 0, size = 20): Promise<PaginatedResponse<ReportJob>> {
  const { data } = await api.get(`/school/reports?page=${page}&size=${size}`);
  return data.data;
}

export async function createReport(payload: CreateReportJobRequest) {
  const { data } = await api.post('/school/reports', payload);
  return data.data;
}
