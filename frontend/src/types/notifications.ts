export interface Notification {
  id: number;
  uuid: string;
  schoolId: number | null;
  userId: number;
  title: string;
  message: string;
  notificationType: string;
  priority: string;
  isRead: boolean;
  readAt: string | null;
  actionUrl: string | null;
  metadata: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateNotificationRequest {
  title: string;
  message: string;
  notificationType?: string;
  priority?: string;
  actionUrl?: string;
  metadata?: string;
}

export type CommunicationChannel = 'SMS' | 'EMAIL' | 'BOTH';
export type CommunicationRecipientGroup =
  | 'STUDENTS'
  | 'PARENTS'
  | 'TEACHERS'
  | 'STAFF'
  | 'ALL_USERS';

export interface CommunicationCampaign {
  id: number;
  schoolId: number;
  senderUserId: number;
  title: string;
  category: string;
  channel: string;
  recipientGroup: string;
  intendedCount: number;
  excludedCount: number;
  acceptedCount: number;
  failedCount: number;
  status: string;
  createdAt: string;
}

export interface CommunicationDelivery {
  id: number;
  recipientType: string;
  recipientId: number;
  channel: string;
  status: string;
  providerMessageId: string | null;
  failureCode: string | null;
  attemptCount: number;
  lastAttemptAt: string | null;
}

export interface PlatformNotificationHistoryEntry {
  id: number;
  uuid: string | null;
  eventCode: string;
  channel: string;
  recipientType: string;
  recipientIdentifier: string | null;
  subject: string | null;
  message: string;
  status: string;
  provider: string | null;
  providerMessageId: string | null;
  errorMessage: string | null;
  sentAt: string | null;
  createdAt: string | null;
}

export interface PlatformNotificationHistoryPage {
  content: PlatformNotificationHistoryEntry[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface RecipientPreview {
  recipientGroup: string;
  channel: string;
  eligibleCount: number;
  excludedCount: number;
  channelEnabled: boolean;
  disabledReason: string | null;
}

export interface NotificationChannels {
  notificationsEnabled: boolean;
  smsEnabled: boolean;
  smsConfigured: boolean;
  emailEnabled: boolean;
  emailConfigured: boolean;
}

export interface ManagerChannelPermission {
  managerUserId: number;
  managerEmail: string;
  schoolId: number;
  smsEnabled: boolean;
  emailEnabled: boolean;
}
