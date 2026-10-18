export interface ReportJob {
  id: number;
  uuid: string;
  schoolId: number | null;
  requestedBy: number;
  reportType: string;
  reportName: string;
  parameters: string | null;
  status: string;
  fileUrl: string | null;
  fileName: string | null;
  fileSize: number | null;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReportJobRequest {
  reportType: string;
  reportName: string;
  parameters?: string;
}
