import { api as http, createApiResponseClient } from './api';
import type { PaginatedResponse } from '@/types/api';
import type { AttendanceResponse, ResultResponse } from '@/types/academic';

const api = createApiResponseClient(http);

export interface Attendance {
  id: number;
  studentId: number;
  classId: number;
  academicYearId: number;
  attendanceDate: string;
  status: string;
  streamId: number | null;
  subjectId: number | null;
  teacherId: number | null;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAttendanceRequest {
  studentId: number;
  classId: number;
  academicYearId: number;
  attendanceDate: string;
  status: string;
  streamId?: number;
  subjectId?: number;
  teacherId?: number;
  remarks?: string;
}

export interface UpdateAttendanceRequest {
  studentId: number;
  classId: number;
  academicYearId: number;
  attendanceDate: string;
  status: string;
  streamId?: number;
  subjectId?: number;
  teacherId?: number;
  remarks?: string;
}

export interface ExaminationType {
  id: number;
  name: string;
  code: string | null;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExaminationTypeRequest {
  name: string;
  code?: string;
  description?: string;
  active?: boolean;
}

export interface UpdateExaminationTypeRequest {
  name: string;
  code?: string;
  description?: string;
  active?: boolean;
}

export interface Examination {
  id: number;
  academicYearId: number;
  name: string;
  typeId: number;
  startDate: string | null;
  endDate: string | null;
  status: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExaminationRequest {
  academicYearId: number;
  name: string;
  typeId: number;
  startDate?: string;
  endDate?: string;
  status?: string;
  description?: string;
}

export interface UpdateExaminationRequest {
  academicYearId: number;
  name: string;
  typeId: number;
  startDate?: string;
  endDate?: string;
  status?: string;
  description?: string;
}

export interface ExaminationSubject {
  id: number;
  examinationId: number;
  subjectId: number;
  classId: number;
  streamId: number | null;
  maximumMarks: number;
  passMark: number;
  gradingSchemeId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExaminationSubjectRequest {
  subjectId: number;
  classId: number;
  streamId?: number;
  maximumMarks: number;
  passMark: number;
  gradingSchemeId?: number;
}

export interface UpdateExaminationSubjectRequest {
  subjectId: number;
  classId: number;
  streamId?: number;
  maximumMarks: number;
  passMark: number;
  gradingSchemeId?: number;
}

export interface GradingScheme {
  id: number;
  name: string;
  description: string | null;
  isDefault: boolean;
  status: string;
  ranges?: GradingSchemeRange[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateGradingSchemeRequest {
  name: string;
  description?: string;
  isDefault?: boolean;
  status?: string;
}

export interface UpdateGradingSchemeRequest {
  name: string;
  description?: string;
  isDefault?: boolean;
  status?: string;
}

export interface GradingSchemeRange {
  id: number;
  gradingSchemeId: number;
  grade: string;
  minMark: number;
  maxMark: number;
  gradePoint: number | null;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AddGradingSchemeRangeRequest {
  grade: string;
  minMark: number;
  maxMark: number;
  gradePoint?: number;
  remark?: string;
}

export type Result = ResultResponse;

export interface CreateResultRequest {
  studentId: number;
  examinationSubjectId: number;
  marksObtained?: number;
  remarks?: string;
}

export interface UpdateResultRequest {
  studentId: number;
  examinationSubjectId: number;
  marksObtained?: number;
  remarks?: string;
}

export interface ResultApproval {
  id: number;
  resultId: number;
  approverId: number;
  action: string;
  comment: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExaminationReport {
  id: number;
  studentId: number;
  examinationId: number;
  totalMarks: number;
  marksObtained: number;
  percentage: number;
  grade: string;
  rank: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export async function getAttendance(
  page?: number,
  size?: number,
  classId?: number,
  date?: string
) {
  const params: Record<string, number | string> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (classId !== undefined) params.classId = classId;
  if (date) params.date = date;
  const { data } = await api.get<PaginatedResponse<AttendanceResponse>>('/school/attendance', { params });
  return data;
}

export async function getClassAttendance(classId: number, date: string) {
  const { data } = await api.get<Attendance[]>(`/school/attendance/class/${classId}/date/${date}`);
  return data;
}

export async function getStudentAttendance(studentId: number) {
  const { data } = await api.get<Attendance[]>(`/school/attendance/student/${studentId}`);
  return data;
}

export async function createAttendance(data: CreateAttendanceRequest) {
  const response = await api.post<Attendance>('/school/attendance', data);
  return response.data;
}

export async function bulkCreateAttendance(data: CreateAttendanceRequest[]) {
  const response = await api.post<Attendance[]>('/school/attendance/bulk', data);
  return response.data;
}

export async function updateAttendance(id: number, data: UpdateAttendanceRequest) {
  const response = await api.put<Attendance>(`/school/attendance/${id}`, data);
  return response.data;
}

export async function getExaminationTypes(page?: number, size?: number, search?: string, active?: boolean) {
  const params: Record<string, number | string | boolean> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (search !== undefined) params.search = search;
  if (active !== undefined) params.active = active;
  const { data } = await api.get<PaginatedResponse<ExaminationType>>('/school/examination-types', { params });
  return data;
}

export async function getExaminationType(id: number) {
  const { data } = await api.get<ExaminationType>(`/school/examination-types/${id}`);
  return data;
}

export async function createExaminationType(data: CreateExaminationTypeRequest) {
  const response = await api.post<ExaminationType>('/school/examination-types', data);
  return response.data;
}

export async function updateExaminationType(id: number, data: UpdateExaminationTypeRequest) {
  const response = await api.put<ExaminationType>(`/school/examination-types/${id}`, data);
  return response.data;
}

export async function setExaminationTypeStatus(id: number, active: boolean) {
  const { data } = await api.patch<ExaminationType>(`/school/examination-types/${id}/status`, undefined, {
    params: { active },
  });
  return data;
}

export async function deleteExaminationType(id: number) {
  await api.delete(`/school/examination-types/${id}`);
}

export async function getExaminations(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Examination>>('/school/examinations', { params });
  return data;
}

export async function getExamination(id: number) {
  const { data } = await api.get<Examination>(`/school/examinations/${id}`);
  return data;
}

export async function createExamination(data: CreateExaminationRequest) {
  const response = await api.post<Examination>('/school/examinations', data);
  return response.data;
}

export async function updateExamination(id: number, data: UpdateExaminationRequest) {
  const response = await api.put<Examination>(`/school/examinations/${id}`, data);
  return response.data;
}

export async function updateExaminationStatus(id: number, status: string) {
  const response = await api.patch<Examination>(`/school/examinations/${id}/status`, { status });
  return response.data;
}

export async function getExaminationSubjects(examinationId: number, page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<ExaminationSubject>>(`/school/examinations/${examinationId}/subjects`, { params });
  return data;
}

export async function getAllExaminationSubjects(examinationId: number) {
  const { data } = await api.get<ExaminationSubject[]>(`/school/examinations/${examinationId}/subjects/all`);
  return data;
}

export async function createExaminationSubject(examinationId: number, data: CreateExaminationSubjectRequest) {
  const response = await api.post<ExaminationSubject>(`/school/examinations/${examinationId}/subjects`, data);
  return response.data;
}

export async function updateExaminationSubject(examinationId: number, id: number, data: UpdateExaminationSubjectRequest) {
  const response = await api.put<ExaminationSubject>(`/school/examinations/${examinationId}/subjects/${id}`, data);
  return response.data;
}

export async function deleteExaminationSubject(examinationId: number, id: number) {
  await api.delete(`/school/examinations/${examinationId}/subjects/${id}`);
}

export async function getGradingSchemes(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<GradingScheme>>('/school/grading-schemes', { params });
  return data;
}

export async function getGradingScheme(id: number) {
  const { data } = await api.get<GradingScheme>(`/school/grading-schemes/${id}`);
  return data;
}

export async function createGradingScheme(data: CreateGradingSchemeRequest) {
  const response = await api.post<GradingScheme>('/school/grading-schemes', data);
  return response.data;
}

export async function updateGradingScheme(id: number, data: UpdateGradingSchemeRequest) {
  const response = await api.put<GradingScheme>(`/school/grading-schemes/${id}`, data);
  return response.data;
}

export async function addGradingSchemeRange(schemeId: number, data: AddGradingSchemeRangeRequest) {
  const response = await api.post<GradingSchemeRange>(`/school/grading-schemes/${schemeId}/ranges`, data);
  return response.data;
}

export async function getResults(params?: {
  page?: number;
  size?: number;
  examinationId?: number;
  classId?: number;
  streamId?: number;
  subjectId?: number;
}) {
  const queryParams: Record<string, number> = {};
  if (params?.page !== undefined) queryParams.page = params.page;
  if (params?.size !== undefined) queryParams.size = params.size;
  if (params?.examinationId !== undefined) queryParams.examinationId = params.examinationId;
  if (params?.classId !== undefined) queryParams.classId = params.classId;
  if (params?.streamId !== undefined) queryParams.streamId = params.streamId;
  if (params?.subjectId !== undefined) queryParams.subjectId = params.subjectId;
  const { data } = await api.get<PaginatedResponse<Result>>('/school/results', { params: queryParams });
  return data;
}

export async function getStudentResults(studentId: number, page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Result>>(`/school/results/student/${studentId}`, { params });
  return data;
}

export async function getExaminationResults(examinationId: number, page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Result>>(`/school/results/examination/${examinationId}`, { params });
  return data;
}

export async function getResult(id: number) {
  const { data } = await api.get<Result>(`/school/results/${id}`);
  return data;
}

export async function createResult(data: CreateResultRequest) {
  const response = await api.post<Result>('/school/results', data);
  return response.data;
}

export async function updateResult(id: number, data: UpdateResultRequest) {
  const response = await api.put<Result>(`/school/results/${id}`, data);
  return response.data;
}

export async function submitResult(id: number) {
  const response = await api.post<Result>(`/school/results/${id}/submit`);
  return response.data;
}

export async function approveResult(id: number) {
  const response = await api.post<Result>(`/school/results/${id}/approve`);
  return response.data;
}

export async function rejectResult(id: number, comment?: string) {
  const response = await api.post<Result>(`/school/results/${id}/reject`, undefined, {
    params: { comment },
  });
  return response.data;
}

export async function publishResult(id: number) {
  const response = await api.post<Result>(`/school/results/${id}/publish`);
  return response.data;
}

export async function getResultApprovals(resultId: number, page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<ResultApproval>>(`/school/result-approvals/result/${resultId}`, { params });
  return data;
}

export async function getExaminationReports(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<ExaminationReport>>('/school/examination-reports', { params });
  return data;
}

export async function getStudentExaminationReports(studentId: number, page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<ExaminationReport>>(`/school/examination-reports/student/${studentId}`, { params });
  return data;
}

export async function getExaminationReport(id: number) {
  const { data } = await api.get<ExaminationReport>(`/school/examination-reports/${id}`);
  return data;
}

export async function generateExaminationReport(studentId: number, examinationId: number) {
  const { data } = await api.post<ExaminationReport>('/school/examination-reports/generate', null, {
    params: { studentId, examinationId },
  });
  return data;
}

export interface UpdateGradingSchemeRangeRequest {
  grade: string;
  minMark: number;
  maxMark: number;
  gradePoint?: number;
  remark?: string;
}

export interface ResultSubmission {
  studentId: number;
  marksObtained?: number | null;
  remarks?: string;
}

export interface ResultAnalytics {
  overallAverage: number | null;
  totalStudentsAssessed: number;
  passRate: number;
  failRate: number;
  highestScore: number | null;
  lowestScore: number | null;
  totalExaminations: number;
  submittedResults: number;
  approvedResults: number;
  publishedResults: number;
  totalResults: number;
  passedResults: number;
  failedResults: number;
  draftResults: number;
  gradeDistribution: { grade: string; count: number; percentage: number }[];
  classPerformance: { classId: number; className: string; average: number | null; studentCount: number; passRate: number; highestScore: number | null; lowestScore: number | null }[];
  streamPerformance: { streamId: number; streamName: string; className: string; average: number | null; studentCount: number; passRate: number }[];
  subjectPerformance: { subjectId: number; subjectName: string; average: number | null; studentCount: number; passRate: number; highestScore: number | null; lowestScore: number | null }[];
  topStudents: { studentId: number; studentName: string; admissionNumber: string; average: number | null; grade: string | null; assessmentCount: number }[];
  studentsNeedingSupport: { studentId: number; studentName: string; admissionNumber: string; average: number | null; grade: string | null; assessmentCount: number; needsSupport: boolean }[];
  performanceTrends: { examinationId: number; examinationName: string; average: number | null; studentCount: number; passRate: number }[];
}

export async function updateGradingSchemeRange(schemeId: number, rangeId: number, data: UpdateGradingSchemeRangeRequest) {
  const { data: result } = await api.put<GradingSchemeRange>(`/school/grading-schemes/${schemeId}/ranges/${rangeId}`, data);
  return result;
}

export async function deleteGradingSchemeRange(schemeId: number, rangeId: number) {
  await api.delete(`/school/grading-schemes/${schemeId}/ranges/${rangeId}`);
}

export async function toggleGradingSchemeStatus(schemeId: number, status: string) {
  const { data } = await api.patch<GradingScheme>(`/school/grading-schemes/${schemeId}/status`, undefined, {
    params: { status },
  });
  return data;
}

export async function deleteGradingScheme(schemeId: number) {
  await api.delete(`/school/grading-schemes/${schemeId}`);
}

export async function recordExamResults(examinationSubjectId: number, submissions: ResultSubmission[]) {
  const { data } = await api.post<Result[]>(`/school/teacher-workspace/examination-subjects/${examinationSubjectId}/results`, submissions);
  return data;
}

export async function submitAllExamResults(examinationSubjectId: number) {
  const { data } = await api.post<Result[]>(`/school/teacher-workspace/examination-subjects/${examinationSubjectId}/submit`);
  return data;
}

export async function reopenResult(id: number) {
  const { data } = await api.post<Result>(`/school/results/${id}/reopen`);
  return data;
}

export async function getResultsAnalytics(params?: {
  academicYearId?: number;
  examinationId?: number;
  classId?: number;
  streamId?: number;
  subjectId?: number;
  startDate?: string;
  endDate?: string;
}) {
  const { data } = await api.get<ResultAnalytics>('/school/results/analytics', { params });
  return data;
}

export async function getDepartmentResultsAnalytics(params?: {
  academicYearId?: number;
  examinationId?: number;
  classId?: number;
  streamId?: number;
  startDate?: string;
  endDate?: string;
}) {
  const { data } = await api.get<ResultAnalytics>('/school/results/analytics/department', { params });
  return data;
}
