import { api } from './api';
import type { PaginatedResponse } from '@/types/api';

export interface Student {
  id: number;
  userId: number;
  admissionNumber: string;
  firstName: string;
  middleName: string | null;
  lastName: string;
  gender: string | null;
  dateOfBirth: string | null;
  registrationDate: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  photoUrl: string | null;
  studentType: string | null;
  status: string;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStudentRequest {
  userId: number;
  admissionNumber: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  gender?: string;
  dateOfBirth?: string;
  registrationDate?: string;
  phone?: string;
  email?: string;
  address?: string;
  photoUrl?: string;
  studentType?: string;
  status?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
}

export interface UpdateStudentRequest {
  userId: number;
  admissionNumber: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  gender?: string;
  dateOfBirth?: string;
  registrationDate?: string;
  phone?: string;
  email?: string;
  address?: string;
  photoUrl?: string;
  studentType?: string;
  status?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
}

export interface Teacher {
  id: number;
  userId: number;
  employeeNumber: string | null;
  gender: string | null;
  dateOfBirth: string | null;
  employmentStatus: string | null;
  qualification: string | null;
  experienceYears: number | null;
  photoUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeacherRequest {
  userId: number;
  employeeNumber?: string;
  gender?: string;
  dateOfBirth?: string;
  employmentStatus?: string;
  qualification?: string;
  experienceYears?: number;
  photoUrl?: string;
}

export interface UpdateTeacherRequest {
  userId: number;
  employeeNumber?: string;
  gender?: string;
  dateOfBirth?: string;
  employmentStatus?: string;
  qualification?: string;
  experienceYears?: number;
  photoUrl?: string;
}

export interface TeacherAssignment {
  id: number;
  teacherId: number;
  subjectId: number;
  classId: number;
  streamId: number | null;
  academicYearId: number;
  isClassTeacher: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeacherAssignmentRequest {
  teacherId: number;
  subjectId: number;
  classId: number;
  streamId?: number;
  academicYearId: number;
  isClassTeacher?: boolean;
}

export interface UpdateTeacherAssignmentRequest {
  teacherId: number;
  subjectId: number;
  classId: number;
  streamId?: number;
  academicYearId: number;
  isClassTeacher?: boolean;
}

export interface Parent {
  id: number;
  userId: number | null;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string;
  gender: string | null;
  occupation: string | null;
  address: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateParentRequest {
  firstName: string;
  lastName: string;
  email?: string;
  phone: string;
  gender?: string;
  occupation?: string;
  address?: string;
  userId?: number;
}

export interface UpdateParentRequest {
  firstName: string;
  lastName: string;
  email?: string;
  phone: string;
  gender?: string;
  occupation?: string;
  address?: string;
  userId?: number;
}

export interface StudentParent {
  id: number;
  studentId: number;
  parentId: number;
  relationshipType: string;
  isPrimary: boolean;
  receivesSms: boolean;
  receivesNotifications: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStudentParentRequest {
  studentId: number;
  parentId: number;
  relationshipType: string;
  isPrimary?: boolean;
  receivesSms?: boolean;
  receivesNotifications?: boolean;
}

export interface UpdateStudentParentRequest {
  studentId: number;
  parentId: number;
  relationshipType: string;
  isPrimary?: boolean;
  receivesSms?: boolean;
  receivesNotifications?: boolean;
}

export interface Enrollment {
  id: number;
  studentId: number;
  academicYearId: number;
  classId: number;
  streamId: number | null;
  status: string;
  startDate: string;
  endDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEnrollmentRequest {
  studentId: number;
  academicYearId: number;
  classId: number;
  streamId?: number;
  status?: string;
  startDate: string;
  endDate?: string;
}

export interface UpdateEnrollmentRequest {
  studentId: number;
  academicYearId: number;
  classId: number;
  streamId?: number;
  status?: string;
  startDate: string;
  endDate: string;
}

export async function getStudents(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Student>>('/school/students', { params });
  return data;
}

export async function getStudent(id: number) {
  const { data } = await api.get<Student>(`/school/students/${id}`);
  return data;
}

export async function createStudent(data: CreateStudentRequest) {
  const response = await api.post<Student>('/school/students', data);
  return response.data;
}

export async function updateStudent(id: number, data: UpdateStudentRequest) {
  const response = await api.put<Student>(`/school/students/${id}`, data);
  return response.data;
}

export async function updateStudentStatus(id: number, status: string) {
  const response = await api.patch<Student>(`/school/students/${id}/status`, { status });
  return response.data;
}

export async function getTeachers(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Teacher>>('/school/teachers', { params });
  return data;
}

export async function getTeacher(id: number) {
  const { data } = await api.get<Teacher>(`/school/teachers/${id}`);
  return data;
}

export async function getMyTeacherProfile() {
  const { data } = await api.get<Teacher>('/school/teachers/me');
  return data;
}

export async function createTeacher(data: CreateTeacherRequest) {
  const response = await api.post<Teacher>('/school/teachers', data);
  return response.data;
}

export async function updateTeacher(id: number, data: UpdateTeacherRequest) {
  const response = await api.put<Teacher>(`/school/teachers/${id}`, data);
  return response.data;
}

export async function getTeacherAssignments(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<TeacherAssignment>>('/school/teacher-assignments', { params });
  return data;
}

export async function getTeacherAssignment(id: number) {
  const { data } = await api.get<TeacherAssignment>(`/school/teacher-assignments/${id}`);
  return data;
}

export async function getTeacherAssignmentsByTeacher(teacherId: number) {
  const { data } = await api.get<TeacherAssignment[]>(`/school/teacher-assignments/teacher/${teacherId}`);
  return data;
}

export async function createTeacherAssignment(data: CreateTeacherAssignmentRequest) {
  const response = await api.post<TeacherAssignment>('/school/teacher-assignments', data);
  return response.data;
}

export async function updateTeacherAssignment(id: number, data: UpdateTeacherAssignmentRequest) {
  const response = await api.put<TeacherAssignment>(`/school/teacher-assignments/${id}`, data);
  return response.data;
}

export async function deleteTeacherAssignment(id: number) {
  await api.delete(`/school/teacher-assignments/${id}`);
}

export async function getParents(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Parent>>('/school/parents', { params });
  return data;
}

export async function getParent(id: number) {
  const { data } = await api.get<Parent>(`/school/parents/${id}`);
  return data;
}

export async function createParent(data: CreateParentRequest) {
  const response = await api.post<Parent>('/school/parents', data);
  return response.data;
}

export async function updateParent(id: number, data: UpdateParentRequest) {
  const response = await api.put<Parent>(`/school/parents/${id}`, data);
  return response.data;
}

export async function getStudentParents(studentId: number) {
  const { data } = await api.get<StudentParent[]>(`/school/student-parents/student/${studentId}`);
  return data;
}

export async function getParentStudents(parentId: number) {
  const { data } = await api.get<StudentParent[]>(`/school/student-parents/parent/${parentId}`);
  return data;
}

export async function createStudentParent(data: CreateStudentParentRequest) {
  const response = await api.post<StudentParent>('/school/student-parents', data);
  return response.data;
}

export async function updateStudentParent(id: number, data: UpdateStudentParentRequest) {
  const response = await api.put<StudentParent>(`/school/student-parents/${id}`, data);
  return response.data;
}

export async function deleteStudentParent(id: number) {
  await api.delete(`/school/student-parents/${id}`);
}

export async function getEnrollments(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<Enrollment>>('/school/enrollments', { params });
  return data;
}

export async function getStudentEnrollments(studentId: number) {
  const { data } = await api.get<Enrollment[]>(`/school/enrollments/student/${studentId}`);
  return data;
}

export async function getEnrollment(id: number) {
  const { data } = await api.get<Enrollment>(`/school/enrollments/${id}`);
  return data;
}

export async function createEnrollment(data: CreateEnrollmentRequest) {
  const response = await api.post<Enrollment>('/school/enrollments', data);
  return response.data;
}

export async function updateEnrollment(id: number, data: UpdateEnrollmentRequest) {
  const response = await api.put<Enrollment>(`/school/enrollments/${id}`, data);
  return response.data;
}
