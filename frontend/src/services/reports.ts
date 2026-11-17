import { api } from './api';
import axios from 'axios';
import type { PaginatedResponse } from '@/types/api';
import type {
  ReportJob,
  CreateReportJobRequest,
  ReportFilterDefinitionsResponse,
  ReportOptionDto,
} from '@/types/reports';

export async function getReports(page = 0, size = 20): Promise<PaginatedResponse<ReportJob>> {
  const { data } = await api.get(`/school/reports?page=${page}&size=${size}`);
  return data.data;
}

export async function createReport(payload: CreateReportJobRequest) {
  const { data } = await api.post('/school/reports', payload);
  return data.data;
}

export async function getReportDefinitions(): Promise<ReportFilterDefinitionsResponse> {
  const { data } = await api.get('/school/reports/definitions');
  return data.data;
}

export async function getReportOptions(
  kind: string,
  classId?: number | null,
  academicYearId?: number | null,
  departmentId?: number | null
): Promise<ReportOptionDto[]> {
  const params = new URLSearchParams();
  if (classId != null) params.set('classId', String(classId));
  if (academicYearId != null) params.set('academicYearId', String(academicYearId));
  if (departmentId != null) params.set('departmentId', String(departmentId));
  const { data } = await api.get(`/school/reports/options/${kind}?${params.toString()}`);
  return data.data;
}

export async function getReport(jobId: number): Promise<ReportJob> {
  const { data } = await api.get(`/school/reports/${jobId}`);
  return data.data;
}

export async function downloadReport(jobId: number): Promise<Blob> {
  const response = await axios.get(`${import.meta.env.VITE_API_URL || '/api'}/school/reports/${jobId}/download`, {
    responseType: 'blob',
    headers: {
      Authorization: `Bearer ${localStorage.getItem('access_token')}`,
    },
  });
  return response.data as Blob;
}

export async function regenerateReport(jobId: number): Promise<ReportJob> {
  const { data } = await api.post(`/school/reports/${jobId}/regenerate`);
  return data.data;
}

export async function deleteReport(jobId: number): Promise<void> {
  await api.delete(`/school/reports/${jobId}`);
}
