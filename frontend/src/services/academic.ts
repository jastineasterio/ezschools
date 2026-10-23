import { api, unwrapApiResponse } from './api';
import type { ApiResponse, PaginatedResponse } from '@/types/api';
import type { DepartmentHeadOptionResponse } from '@/types/academic';

export interface AcademicYear {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
  isCurrent: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAcademicYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  status?: string;
  isCurrent?: boolean;
}

export interface UpdateAcademicYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  status?: string;
  isCurrent?: boolean;
}

export interface Department {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  code: string | null;
  description: string | null;
  headOfDepartmentId: number | null;
  headOfDepartmentName: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDepartmentRequest {
  name: string;
  code?: string;
  description?: string;
  headOfDepartmentId?: number;
  status?: string;
}

export interface UpdateDepartmentRequest {
  name: string;
  code?: string;
  description?: string;
  headOfDepartmentId?: number;
  status?: string;
}

export interface Class {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  code: string | null;
  description: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateClassRequest {
  name: string;
  code?: string;
  description?: string;
  status?: string;
}

export interface UpdateClassRequest {
  name: string;
  code?: string;
  description?: string;
  status?: string;
}

export interface Stream {
  id: number;
  uuid: string;
  schoolId: number;
  classId: number;
  className: string;
  name: string;
  code: string | null;
  capacity: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStreamRequest {
  classId: number;
  name: string;
  code?: string;
  capacity?: number;
  status?: string;
}

export interface UpdateStreamRequest {
  classId: number;
  name: string;
  code?: string;
  capacity?: number;
  status?: string;
}

export interface Subject {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  code: string | null;
  description: string | null;
  departmentId: number | null;
  departmentName: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSubjectRequest {
  name: string;
  code?: string;
  description?: string;
  departmentId?: number;
  status?: string;
}

export interface UpdateSubjectRequest {
  name: string;
  code?: string;
  description?: string;
  departmentId?: number;
  status?: string;
}

export interface StreamSubjectAssignment {
  id: number;
  subjectId: number;
  subjectName: string;
  subjectCode: string | null;
  academicYearId: number;
  classId: number;
  streamId: number;
}

export interface SaveStreamSubjectsRequest {
  academicYearId: number;
  classId: number;
  streamId: number;
  subjectIds: number[];
}

export async function getAcademicYears(page?: number, size?: number, search?: string) {
  const params: Record<string, number | string> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (search?.trim()) params.search = search.trim();
  return unwrapApiResponse(api.get<ApiResponse<PaginatedResponse<AcademicYear>>>('/school/academic-years', { params }));
}

export async function getAcademicYear(id: number) {
  return unwrapApiResponse(api.get<ApiResponse<AcademicYear>>(`/school/academic-years/${id}`));
}

export async function createAcademicYear(data: CreateAcademicYearRequest) {
  return unwrapApiResponse(api.post<ApiResponse<AcademicYear>>('/school/academic-years', data));
}

export async function updateAcademicYear(id: number, data: UpdateAcademicYearRequest) {
  return unwrapApiResponse(api.put<ApiResponse<AcademicYear>>(`/school/academic-years/${id}`, data));
}

export async function setCurrentAcademicYear(id: number) {
  return unwrapApiResponse(api.patch<ApiResponse<AcademicYear>>(`/school/academic-years/${id}/status`));
}

export async function getDepartments(page?: number, size?: number, search?: string) {
  const params: Record<string, number | string> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (search?.trim()) params.search = search.trim();
  return unwrapApiResponse(api.get<ApiResponse<PaginatedResponse<Department>>>('/school/departments', { params }));
}

export async function getEligibleDepartmentHeads() {
  return unwrapApiResponse(
    api.get<ApiResponse<DepartmentHeadOptionResponse[]>>('/school/departments/eligible-heads')
  );
}

export async function getDepartment(id: number) {
  return unwrapApiResponse(api.get<ApiResponse<Department>>(`/school/departments/${id}`));
}

export async function createDepartment(data: CreateDepartmentRequest) {
  return unwrapApiResponse(api.post<ApiResponse<Department>>('/school/departments', data));
}

export async function updateDepartment(id: number, data: UpdateDepartmentRequest) {
  return unwrapApiResponse(api.put<ApiResponse<Department>>(`/school/departments/${id}`, data));
}

export async function updateDepartmentStatus(id: number, status: string) {
  return unwrapApiResponse(api.patch<ApiResponse<Department>>(`/school/departments/${id}/status`, undefined, { params: { status } }));
}

export async function getClasses(page?: number, size?: number, search?: string) {
  const params: Record<string, number | string> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (search?.trim()) params.search = search.trim();
  return unwrapApiResponse(api.get<ApiResponse<PaginatedResponse<Class>>>('/school/classes', { params }));
}

export async function getClass(id: number) {
  return unwrapApiResponse(api.get<ApiResponse<Class>>(`/school/classes/${id}`));
}

export async function createClass(data: CreateClassRequest) {
  return unwrapApiResponse(api.post<ApiResponse<Class>>('/school/classes', data));
}

export async function updateClass(id: number, data: UpdateClassRequest) {
  return unwrapApiResponse(api.put<ApiResponse<Class>>(`/school/classes/${id}`, data));
}

export async function updateClassStatus(id: number, status: string) {
  return unwrapApiResponse(api.patch<ApiResponse<Class>>(`/school/classes/${id}/status`, undefined, { params: { status } }));
}

export async function getStreams(page?: number, size?: number, search?: string) {
  const params: Record<string, number | string> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (search?.trim()) params.search = search.trim();
  return unwrapApiResponse(api.get<ApiResponse<PaginatedResponse<Stream>>>('/school/streams', { params }));
}

export async function getStreamsByClass(classId: number) {
  return unwrapApiResponse(api.get<ApiResponse<Stream[]>>(`/school/streams/class/${classId}`));
}

export async function getStream(id: number) {
  return unwrapApiResponse(api.get<ApiResponse<Stream>>(`/school/streams/${id}`));
}

export async function createStream(data: CreateStreamRequest) {
  return unwrapApiResponse(api.post<ApiResponse<Stream>>('/school/streams', data));
}

export async function updateStream(id: number, data: UpdateStreamRequest) {
  return unwrapApiResponse(api.put<ApiResponse<Stream>>(`/school/streams/${id}`, data));
}

export async function updateStreamStatus(id: number, status: string) {
  return unwrapApiResponse(api.patch<ApiResponse<Stream>>(`/school/streams/${id}/status`, undefined, { params: { status } }));
}

export async function getSubjects(page?: number, size?: number, search?: string) {
  const params: Record<string, number | string> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  if (search?.trim()) params.search = search.trim();
  return unwrapApiResponse(api.get<ApiResponse<PaginatedResponse<Subject>>>('/school/subjects', { params }));
}

export async function getSubject(id: number) {
  return unwrapApiResponse(api.get<ApiResponse<Subject>>(`/school/subjects/${id}`));
}

export async function createSubject(data: CreateSubjectRequest) {
  return unwrapApiResponse(api.post<ApiResponse<Subject>>('/school/subjects', data));
}

export async function updateSubject(id: number, data: UpdateSubjectRequest) {
  return unwrapApiResponse(api.put<ApiResponse<Subject>>(`/school/subjects/${id}`, data));
}

export async function updateSubjectStatus(id: number, status: string) {
  return unwrapApiResponse(api.patch<ApiResponse<Subject>>(`/school/subjects/${id}/status`, undefined, { params: { status } }));
}

export async function getStreamSubjectAssignments(academicYearId: number, classId: number, streamId: number) {
  const params = { academicYearId, classId, streamId };
  return unwrapApiResponse(
    api.get<ApiResponse<StreamSubjectAssignment[]>>('/school/stream-subject-assignments', { params })
  );
}

export async function saveStreamSubjectAssignments(data: SaveStreamSubjectsRequest) {
  return unwrapApiResponse(
    api.put<ApiResponse<StreamSubjectAssignment[]>>('/school/stream-subject-assignments', data)
  );
}

export async function getStudentStreamSubjects(studentId: number, academicYearId: number) {
  return unwrapApiResponse(
    api.get<ApiResponse<StreamSubjectAssignment[]>>(
      `/school/stream-subject-assignments/student/${studentId}`,
      { params: { academicYearId } }
    )
  );
}
