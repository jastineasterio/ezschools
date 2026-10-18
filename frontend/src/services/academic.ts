import { api } from './api';
import type { PaginatedResponse } from '@/types/api';

export interface AcademicYear {
  id: number;
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
  name: string;
  code: string | null;
  description: string | null;
  headOfDepartmentId: number | null;
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
  classId: number;
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
  name: string;
  code: string | null;
  description: string | null;
  departmentId: number | null;
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

export async function getAcademicYears(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<AcademicYear>>('/school/academic-years', { params });
  return data;
}

export async function getAcademicYear(id: number) {
  const { data } = await api.get<AcademicYear>(`/school/academic-years/${id}`);
  return data;
}

export async function createAcademicYear(data: CreateAcademicYearRequest) {
  const response = await api.post<AcademicYear>('/school/academic-years', data);
  return response.data;
}

export async function updateAcademicYear(id: number, data: UpdateAcademicYearRequest) {
  const response = await api.put<AcademicYear>(`/school/academic-years/${id}`, data);
  return response.data;
}

export async function setCurrentAcademicYear(id: number) {
  const response = await api.patch<AcademicYear>(`/school/academic-years/${id}/status`);
  return response.data;
}

export async function getDepartments(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Department>>('/school/departments', { params });
  return data;
}

export async function getDepartment(id: number) {
  const { data } = await api.get<Department>(`/school/departments/${id}`);
  return data;
}

export async function createDepartment(data: CreateDepartmentRequest) {
  const response = await api.post<Department>('/school/departments', data);
  return response.data;
}

export async function updateDepartment(id: number, data: UpdateDepartmentRequest) {
  const response = await api.put<Department>(`/school/departments/${id}`, data);
  return response.data;
}

export async function updateDepartmentStatus(id: number, status: string) {
  const response = await api.patch<Department>(`/school/departments/${id}/status`, { status });
  return response.data;
}

export async function getClasses(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Class>>('/school/classes', { params });
  return data;
}

export async function getClass(id: number) {
  const { data } = await api.get<Class>(`/school/classes/${id}`);
  return data;
}

export async function createClass(data: CreateClassRequest) {
  const response = await api.post<Class>('/school/classes', data);
  return response.data;
}

export async function updateClass(id: number, data: UpdateClassRequest) {
  const response = await api.put<Class>(`/school/classes/${id}`, data);
  return response.data;
}

export async function updateClassStatus(id: number, status: string) {
  const response = await api.patch<Class>(`/school/classes/${id}/status`, { status });
  return response.data;
}

export async function getStreams(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Stream>>('/school/streams', { params });
  return data;
}

export async function getStreamsByClass(classId: number) {
  const { data } = await api.get<Stream[]>(`/school/streams/class/${classId}`);
  return data;
}

export async function getStream(id: number) {
  const { data } = await api.get<Stream>(`/school/streams/${id}`);
  return data;
}

export async function createStream(data: CreateStreamRequest) {
  const response = await api.post<Stream>('/school/streams', data);
  return response.data;
}

export async function updateStream(id: number, data: UpdateStreamRequest) {
  const response = await api.put<Stream>(`/school/streams/${id}`, data);
  return response.data;
}

export async function updateStreamStatus(id: number, status: string) {
  const response = await api.patch<Stream>(`/school/streams/${id}/status`, { status });
  return response.data;
}

export async function getSubjects(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Subject>>('/school/subjects', { params });
  return data;
}

export async function getSubject(id: number) {
  const { data } = await api.get<Subject>(`/school/subjects/${id}`);
  return data;
}

export async function createSubject(data: CreateSubjectRequest) {
  const response = await api.post<Subject>('/school/subjects', data);
  return response.data;
}

export async function updateSubject(id: number, data: UpdateSubjectRequest) {
  const response = await api.put<Subject>(`/school/subjects/${id}`, data);
  return response.data;
}

export async function updateSubjectStatus(id: number, status: string) {
  const response = await api.patch<Subject>(`/school/subjects/${id}/status`, { status });
  return response.data;
}
