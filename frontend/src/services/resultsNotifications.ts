import { api } from './api';
import type { ApiResponse, PaginatedResponse } from '@/types/api';
import type {
  ResultNotificationPreview,
  ResultNotificationRequest,
  ResultNotificationDto,
  ResultNotificationDeliveryDto,
  ResultNotificationSummary,
  ResultNotificationTemplate,
  SaveResultNotificationTemplateRequest,
} from '@/types/resultsNotifications';

export async function getResultNotificationExaminations() {
  const { data } = await api.get<ApiResponse<any[]>>(
    '/school/results-notifications/examinations',
  );
  return data.data ?? [];
}

export async function getResultNotificationClasses() {
  const { data } = await api.get<ApiResponse<any[]>>(
    '/school/results-notifications/classes',
  );
  return data.data ?? [];
}

export async function getResultNotificationStreams(classId: number) {
  const { data } = await api.get<ApiResponse<any[]>>(
    '/school/results-notifications/streams',
    { params: { classId } },
  );
  return data.data ?? [];
}

export async function previewResultNotificationRecipients(
  examinationId: number,
  classId: number,
  streamId: number | null,
  recipientType: string,
): Promise<ResultNotificationPreview> {
  const { data } = await api.get<ApiResponse<ResultNotificationPreview>>(
    '/school/results-notifications/preview',
    { params: { examinationId, classId, streamId, recipientType } },
  );
  if (!data.data) throw new Error('Preview response is empty');
  return data.data;
}

export async function sendResultNotification(
  payload: ResultNotificationRequest,
): Promise<ResultNotificationDto> {
  const { data } = await api.post<ApiResponse<ResultNotificationDto>>(
    '/school/results-notifications',
    payload,
  );
  if (!data.data) throw new Error('Send response is empty');
  return data.data;
}

export async function getResultNotificationHistory(
  page = 0,
  size = 20,
): Promise<PaginatedResponse<ResultNotificationDto>> {
  const { data } = await api.get<ApiResponse<PaginatedResponse<ResultNotificationDto>>>(
    '/school/results-notifications',
    { params: { page, size } },
  );
  if (!data.data) return { content: [], totalElements: 0, totalPages: 0, page, size };
  return data.data;
}

export async function getResultNotificationDeliveries(
  notificationId: number,
): Promise<ResultNotificationDeliveryDto[]> {
  const { data } = await api.get<ApiResponse<ResultNotificationDeliveryDto[]>>(
    `/school/results-notifications/${notificationId}/deliveries`,
  );
  return data.data ?? [];
}

export async function getResultNotificationSummary(
  notificationId: number,
): Promise<ResultNotificationSummary> {
  const { data } = await api.get<ApiResponse<ResultNotificationSummary>>(
    `/school/results-notifications/${notificationId}/summary`,
  );
  if (!data.data) throw new Error('Summary response is empty');
  return data.data;
}

export async function getResultNotificationTemplate(): Promise<ResultNotificationTemplate> {
  const { data } = await api.get<ApiResponse<ResultNotificationTemplate>>(
    '/school/results-notifications/template',
  );
  if (!data.data) throw new Error('Template response is empty');
  return data.data;
}

export async function saveResultNotificationTemplate(
  payload: SaveResultNotificationTemplateRequest,
): Promise<ResultNotificationTemplate> {
  const { data } = await api.put<ApiResponse<ResultNotificationTemplate>>(
    '/school/results-notifications/template',
    payload,
  );
  if (!data.data) throw new Error('Template save response is empty');
  return data.data;
}

export async function restoreResultNotificationTemplate(): Promise<ResultNotificationTemplate> {
  const { data } = await api.post<ApiResponse<ResultNotificationTemplate>>(
    '/school/results-notifications/template/restore-default',
  );
  if (!data.data) throw new Error('Template restore response is empty');
  return data.data;
}
