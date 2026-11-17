export interface SmsConfiguration {
  id: number;
  uuid: string;
  schoolId: number;
  schoolName?: string | null;
  provider: string;
  senderId: string | null;
  active: boolean;
  credentialsConfigured: boolean;
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
  apiSecret?: string;
  senderId?: string;
  isActive?: boolean;
  monthlyLimit?: number;
  dailyLimit?: number;
  remarks?: string;
}

export interface UpdateSmsConfigurationRequest {
  provider: string;
  apiKey: string;
  apiSecret?: string;
  senderId?: string;
  isActive?: boolean;
  monthlyLimit?: number;
  dailyLimit?: number;
  remarks?: string;
}

export interface SmsTestResult {
  success: boolean;
  providerMessageId: string | null;
  failureCode: string | null;
  balance: Record<string, unknown> | null;
  senderIds: string[] | null;
}

export interface SmsPurchase {
  id: number;
  uuid: string;
  schoolId: number;
  purchaseId: string;
  invoiceNumber: string | null;
  transactionRef: string | null;
  quantitySms: number | null;
  unitPrice: number | null;
  subtotalAmount: number | null;
  vatAmount: number | null;
  totalAmount: number | null;
  currency: string;
  efdReceipt: boolean;
  status: string | null;
  paymentStatus: string | null;
  channel: string | null;
  canRetry: boolean;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSmsPurchaseRequest {
  quantitySms: number;
  payerPhone: string;
}

export interface RetryPurchaseRequest {
  phone?: string;
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
