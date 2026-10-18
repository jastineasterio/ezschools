export interface SmsConfiguration {
  id: number;
  uuid: string;
  schoolId: number;
  provider: string;
  senderId: string | null;
  isActive: boolean;
  monthlyLimit: number | null;
  dailyLimit: number | null;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSmsConfigurationRequest {
  schoolId: number;
  provider: string;
  apiKey: string;
  apiSecret: string;
  senderId?: string;
  isActive?: boolean;
  monthlyLimit?: number;
  dailyLimit?: number;
  remarks?: string;
}

export interface SmsMessage {
  id: number;
  uuid: string;
  schoolId: number;
  templateId: number | null;
  recipient: string;
  message: string;
  status: string;
  providerResponse: string | null;
  sentAt: string | null;
  deliveredAt: string | null;
  failedAt: string | null;
  errorMessage: string | null;
  createdBy: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSmsMessageRequest {
  schoolId: number;
  recipient: string;
  message: string;
  templateId?: number;
  bulk?: boolean;
}

export interface SmsTemplate {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  content: string;
  variables: string | null;
  isActive: boolean;
}

export interface CreateSmsTemplateRequest {
  schoolId: number;
  name: string;
  content: string;
  variables?: string;
  isActive?: boolean;
}
