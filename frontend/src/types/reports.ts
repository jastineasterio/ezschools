export interface ReportOptionDto {
  id: string;
  name: string;
}

export interface ReportFilterFieldDto {
  name: string;
  label: string;
  type: string;
  required: boolean;
  placeholder?: string;
  helperText?: string;
  optionsEndpoint?: string;
  dependsOn?: string;
  options?: ReportOptionDto[];
}

export interface ReportTypeInfoDto {
  id: string;
  label: string;
  category: string;
  fields: ReportFilterFieldDto[];
}

export interface ReportFilterDefinitionsResponse {
  reportTypes: ReportTypeInfoDto[];
}

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
  academicYearId?: number | null;
  dateFrom?: string | null;
  dateTo?: string | null;
  classId?: number | null;
  streamId?: number | null;
  departmentId?: number | null;
  subjectId?: number | null;
  examinationId?: number | null;
  studentId?: number | null;
  status?: string | null;
  gender?: string | null;
  includeFields?: string[] | null;
}
