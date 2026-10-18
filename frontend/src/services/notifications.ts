import { api } from './api';
import type { PaginatedResponse } from '@/types/api';
import type {
  CommunicationCampaign,
  CommunicationDelivery,
  CommunicationRecipientGroup,
  CommunicationChannel,
  CreateNotificationRequest,
  ManagerChannelPermission,
  Notification,
  NotificationChannels,
  PlatformNotificationHistoryPage,
  RecipientPreview,
} from '@/types/notifications';

export async function getNotifications(page = 0, size = 20): Promise<PaginatedResponse<Notification>> {
  const { data } = await api.get(`/school/notifications?page=${page}&size=${size}`);
  return data.data;
}

export async function getUnreadNotifications(): Promise<Notification[]> {
  const { data } = await api.get('/school/notifications/unread');
  return data.data;
}

export async function getUnreadCount(): Promise<number> {
  const { data } = await api.get('/school/notifications/unread/count');
  return data.data;
}

export async function markNotificationAsRead(id: number) {
  const { data } = await api.patch(`/school/notifications/${id}/read`);
  return data.data;
}

export async function markAllNotificationsAsRead() {
  const { data } = await api.post('/school/notifications/read-all');
  return data;
}

export async function createNotification(payload: CreateNotificationRequest) {
  const { data } = await api.post('/school/notifications', payload);
  return data.data;
}

export async function getCommunicationChannels(): Promise<NotificationChannels> {
  const { data } = await api.get('/school/communications/channels');
  return data.data;
}

export async function previewCommunicationRecipients(
  group: CommunicationRecipientGroup,
  channel: CommunicationChannel,
): Promise<RecipientPreview> {
  const { data } = await api.get('/school/communications/recipients/preview', { params: { group, channel } });
  return data.data;
}

export async function sendCommunicationCampaign(payload: {
  title: string;
  message: string;
  category: string;
  channel: CommunicationChannel;
  recipientGroup: CommunicationRecipientGroup;
  idempotencyKey: string;
}): Promise<CommunicationCampaign> {
  const { data } = await api.post('/school/communications', payload);
  return data.data;
}

export async function getCommunicationCampaigns(page = 0, size = 20): Promise<PaginatedResponse<CommunicationCampaign>> {
  const { data } = await api.get('/school/communications', { params: { page, size } });
  return data.data;
}

export async function getCommunicationDeliveries(campaignId: number): Promise<CommunicationDelivery[]> {
  const { data } = await api.get(`/school/communications/${campaignId}/deliveries`);
  return data.data;
}

export async function getPlatformCommunicationDeliveries(campaignId: number): Promise<CommunicationDelivery[]> {
  const { data } = await api.get(`/platform/notifications/history/${campaignId}/deliveries`);
  return data.data;
}

export async function getPlatformCommunicationHistory(page = 0, size = 50): Promise<PaginatedResponse<CommunicationCampaign>> {
  const { data } = await api.get('/platform/notifications/history', { params: { page, size } });
  return data.data;
}

export async function getPlatformNotificationHistory(page = 0, size = 50): Promise<PlatformNotificationHistoryPage> {
  const { data } = await api.get('/platform/notifications/history/logs', { params: { page, size } });
  return data.data;
}

export async function retryCommunicationCampaign(campaignId: number): Promise<CommunicationCampaign> {
  const { data } = await api.post(`/school/communications/${campaignId}/retry`);
  return data.data;
}

export async function getNotificationConfiguration(): Promise<NotificationChannels> {
  const { data } = await api.get('/platform/notifications/configuration');
  return data.data;
}

export async function updateNotificationConfiguration(payload: {
  notificationsEnabled: boolean;
  smsEnabled: boolean;
  emailEnabled: boolean;
}): Promise<NotificationChannels> {
  const { data } = await api.put('/platform/notifications/configuration', payload);
  return data.data;
}

export async function getManagerChannelPermissions(): Promise<ManagerChannelPermission[]> {
  const { data } = await api.get('/platform/notifications/managers');
  return data.data;
}

export async function updateManagerChannelPermissions(
  managerId: number,
  payload: { smsEnabled: boolean; emailEnabled: boolean },
): Promise<ManagerChannelPermission> {
  const { data } = await api.put(`/platform/notifications/managers/${managerId}/channels`, payload);
  return data.data;
}

export async function sendTestEmail(recipient: string): Promise<void> {
  await api.post('/platform/notifications/test-email', { recipient, confirmSend: true });
}
