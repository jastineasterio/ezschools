export interface ResultNotificationPreview {
  examinationId: number;
  examinationName: string;
  classId: number;
  className: string;
  streamId: number | null;
  streamName: string | null;
  recipientType: string;
  eligibleCount: number;
  excludedCount: number;
  students: StudentResultPreview[];
  parents: ParentGuardianPreview[];
  sampleMessage: string;
  canSend: boolean;
  reason: string | null;
}

export interface StudentResultPreview {
  resultId: number | null;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  classId: number;
  className: string;
  streamId: number | null;
  streamName: string | null;
  subjectId: number | null;
  subjectName: string | null;
  examinationSubjectId: number | null;
  marksObtained: number | null;
  maximumMarks: number | null;
  grade: string | null;
  status: string | null;
  phone: string | null;
  eligible: boolean;
  exclusionReason: string | null;
}

export interface ParentGuardianPreview {
  parentId: number;
  parentName: string;
  phone: string | null;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  relationshipType: string | null;
  eligible: boolean;
  exclusionReason: string | null;
}

export interface ResultNotificationRequest {
  examinationId: number;
  classId: number;
  streamId: number;
  recipientType: string;
  idempotencyKey?: string;
}

export interface ResultNotificationDto {
  id: number;
  uuid: string | null;
  schoolId: number;
  examinationId: number;
  classId: number;
  streamId: number | null;
  recipientType: string;
  idempotencyKey: string;
  title: string;
  totalRecipients: number;
  excludedCount: number;
  successfulCount: number;
  failedCount: number;
  pendingCount: number;
  status: string;
  initiatedBy: number | null;
  createdAt: string;
}

export interface ResultNotificationDeliveryDto {
  id: number;
  notificationId: number;
  smsMessageId: number | null;
  recipientType: string;
  recipientId: number;
  recipientName: string;
  recipientPhone: string | null;
  studentId: number | null;
  studentName: string | null;
  admissionNumber: string | null;
  channel: string;
  status: string;
  providerMessageId: string | null;
  batchId: string | null;
  failureCode: string | null;
  errorMessage: string | null;
  deliveryStatus: string | null;
  attemptCount: number;
  lastAttemptAt: string | null;
  sentAt: string | null;
  deliveredAt: string | null;
  failedAt: string | null;
}

export interface ResultNotificationSummary {
  totalRecipients: number;
  excludedCount: number;
  successfulCount: number;
  failedCount: number;
  pendingCount: number;
}

export interface PlaceholderInfo {
  key: string;
  description: string;
  example: string;
}

export interface ResultNotificationTemplate {
  schoolId: number;
  smsTemplate: string;
  isDefault: boolean;
  updatedAt: string | null;
  supportedPlaceholders: PlaceholderInfo[];
}

export interface SaveResultNotificationTemplateRequest {
  smsTemplate: string;
}

export type RecipientType = 'STUDENTS' | 'PARENTS' | 'BOTH';
