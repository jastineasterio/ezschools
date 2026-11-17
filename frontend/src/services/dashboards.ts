import { api, unwrapApiResponse } from './api';
import type { ApiResponse, PaginatedResponse } from '@/types/api';
import type {
  AccountantDashboard,
  HodDashboard,
  LibrarianDashboard,
  ParentDashboard,
  TransportOfficerDashboard,
  WardenDashboard,
} from '@/types/dashboards';
import type { PlatformDashboardResponse } from '@/types/api';

export async function getPlatformDashboard(): Promise<PlatformDashboardResponse> {
  return unwrapApiResponse(
    api.get<ApiResponse<PlatformDashboardResponse>>('/platform/dashboard')
  );
}

export async function getParentDashboard(): Promise<ParentDashboard> {
  return unwrapApiResponse(
    api.get<ApiResponse<ParentDashboard>>('/school/parent/dashboard')
  );
}

export async function getAccountantDashboard(): Promise<AccountantDashboard> {
  return unwrapApiResponse(
    api.get<ApiResponse<AccountantDashboard>>('/school/accountant/dashboard')
  );
}

export async function getLibrarianDashboard(): Promise<LibrarianDashboard> {
  return unwrapApiResponse(
    api.get<ApiResponse<LibrarianDashboard>>('/school/librarian/dashboard')
  );
}

export async function getHodDashboard(): Promise<HodDashboard> {
  return unwrapApiResponse(
    api.get<ApiResponse<HodDashboard>>('/school/hod/dashboard')
  );
}

export async function getWardenDashboard(): Promise<WardenDashboard> {
  return unwrapApiResponse(
    api.get<ApiResponse<WardenDashboard>>('/school/warden/dashboard')
  );
}

export async function getTransportOfficerDashboard(): Promise<TransportOfficerDashboard> {
  return unwrapApiResponse(
    api.get<ApiResponse<TransportOfficerDashboard>>(
      '/school/transport-officer/dashboard'
    )
  );
}

export interface StudentSelfProfile {
  id: number;
  uuid: string | null;
  userId: number;
  schoolId: number;
  schoolName: string | null;
  schoolCode: string | null;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  gender: string | null;
  dateOfBirth: string | null;
  admissionNumber: string | null;
  className: string | null;
  streamName: string | null;
  academicYearName: string | null;
  enrollmentStatus: string | null;
  studentType: string | null;
  photoUrl: string | null;
}

export interface StudentAttendanceRecord {
  id: number;
  date: string;
  status: string;
  className: string | null;
  subjectName: string | null;
  remarks: string | null;
}

export interface StudentResult {
  id: number;
  examinationName: string;
  subjectName: string;
  marksObtained: number | null;
  maximumMarks: number | null;
  percentage: number | null;
  grade: string | null;
  status: string | null;
}

export interface StudentFeeAssignment {
  id: number;
  feeStructureName: string;
  feeType: string | null;
  amountDue: number;
  amountPaid: number;
  balance: number;
  dueDate: string | null;
  status: string | null;
}

export interface StudentPayment {
  id: number;
  amount: number;
  paymentMethod: string;
  paymentDate: string;
  receiptNumber: string | null;
  status: string;
}

export interface StudentTimetableLesson {
  id: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  subjectName: string;
  className: string;
  streamName: string | null;
  room: string | null;
  teacherName: string | null;
}

export async function getStudentSelfProfile(): Promise<StudentSelfProfile> {
  return unwrapApiResponse(api.get<ApiResponse<StudentSelfProfile>>('/school/student/me'));
}

export async function getStudentSelfEnrollments(): Promise<unknown[]> {
  return unwrapApiResponse(api.get<ApiResponse<unknown[]>>('/school/student/enrollments'));
}

export async function getStudentSelfAttendance(): Promise<StudentAttendanceRecord[]> {
  return unwrapApiResponse(api.get<ApiResponse<StudentAttendanceRecord[]>>('/school/student/attendance'));
}

export async function getStudentSelfResults(page = 0, size = 20): Promise<PaginatedResponse<StudentResult>> {
  const { data } = await api.get<ApiResponse<PaginatedResponse<StudentResult>>>(
    '/school/student/results',
    { params: { page, size } }
  );
  return data.data ?? { content: [], totalElements: 0, totalPages: 0, page, size };
}

export async function getStudentSelfFeeAssignments(): Promise<StudentFeeAssignment[]> {
  return unwrapApiResponse(api.get<ApiResponse<StudentFeeAssignment[]>>('/school/student/fees/assignments'));
}

export async function getStudentSelfPayments(): Promise<StudentPayment[]> {
  return unwrapApiResponse(api.get<ApiResponse<StudentPayment[]>>('/school/student/fees/payments'));
}

export async function getStudentSelfTimetable(): Promise<StudentTimetableLesson[]> {
  return unwrapApiResponse(api.get<ApiResponse<StudentTimetableLesson[]>>('/school/student/timetable'));
}
