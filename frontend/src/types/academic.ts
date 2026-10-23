export interface AcademicYearResponse {
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

export interface DepartmentResponse {
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

export interface DepartmentHeadOptionResponse {
  id: number;
  name: string;
  email: string;
}

export interface CreateDepartmentRequest {
  name: string;
  code?: string;
  description?: string;
  headOfDepartmentId?: number;
  status?: string;
}

export interface ClassResponse {
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

export interface StreamResponse {
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

export interface SubjectResponse {
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

export interface TeacherProfileResponse {
  id: number;
  uuid: string;
  userId: number;
  schoolId: number;
  employeeNumber: string | null;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  gender: string | null;
  dateOfBirth: string | null;
  employmentStatus: string;
  qualification: string | null;
  experienceYears: number | null;
  photoUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeacherProfileRequest {
  userId: number;
  employeeNumber?: string;
  gender?: string;
  dateOfBirth?: string;
  employmentStatus?: string;
  qualification?: string;
  experienceYears?: number;
  photoUrl?: string;
}

export interface TeacherAssignmentResponse {
  id: number;
  uuid: string;
  teacherId: number;
  teacherName: string;
  schoolId: number;
  subjectId: number;
  subjectName: string;
  classId: number;
  className: string;
  streamId: number | null;
  streamName: string | null;
  academicYearId: number;
  academicYearName: string;
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

export interface StudentProfileResponse {
  id: number;
  uuid: string;
  userId: number;
  schoolId: number;
  admissionNumber: string;
  initialPassword?: string;
  firstName: string;
  middleName: string | null;
  lastName: string;
  fullName: string;
  gender: string | null;
  dateOfBirth: string | null;
  registrationDate: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  photoUrl: string | null;
  studentType: string;
  status: string;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStudentProfileRequest {
  userId: number;
  admissionNumber?: string;
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

export interface ParentResponse {
  id: number;
  uuid: string;
  schoolId: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string | null;
  phone: string;
  gender: string | null;
  occupation: string | null;
  address: string | null;
  userId: number | null;
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

export interface StudentParentResponse {
  id: number;
  uuid: string;
  studentId: number;
  studentName: string;
  parentId: number;
  parentName: string;
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

export interface StudentEnrollmentResponse {
  id: number;
  uuid: string;
  studentId: number;
  studentName: string;
  schoolId: number;
  academicYearId: number;
  academicYearName: string;
  classId: number;
  className: string;
  streamId: number | null;
  streamName: string | null;
  status: string;
  startDate: string;
  endDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStudentEnrollmentRequest {
  studentId: number;
  academicYearId: number;
  classId: number;
  streamId?: number;
  status?: string;
  startDate: string;
  endDate?: string;
}

export interface AttendanceResponse {
  id: number;
  uuid: string;
  schoolId: number;
  studentId: number;
  studentName: string;
  classId: number;
  className: string;
  streamId: number | null;
  streamName: string | null;
  academicYearId: number;
  academicYearName: string;
  subjectId: number | null;
  subjectName: string | null;
  teacherId: number | null;
  teacherName: string | null;
  attendanceDate: string;
  status: string;
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

export interface ExaminationTypeResponse {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  code: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExaminationTypeRequest {
  name: string;
  code?: string;
  description?: string;
}

export interface ExaminationResponse {
  id: number;
  uuid: string;
  schoolId: number;
  academicYearId: number;
  academicYearName: string;
  name: string;
  typeId: number;
  typeName: string;
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

export interface ExaminationSubjectResponse {
  id: number;
  uuid: string;
  examinationId: number;
  examinationName: string;
  subjectId: number;
  subjectName: string;
  classId: number;
  className: string;
  streamId: number | null;
  streamName: string | null;
  maximumMarks: number;
  passMark: number;
  gradingSchemeId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExaminationSubjectRequest {
  examinationId: number;
  subjectId: number;
  classId: number;
  streamId?: number;
  maximumMarks: number;
  passMark: number;
  gradingSchemeId?: number;
}

export interface GradingSchemeResponse {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  description: string | null;
  isDefault: boolean;
  status: string;
  ranges: GradingSchemeRangeResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateGradingSchemeRequest {
  name: string;
  description?: string;
  isDefault?: boolean;
  status?: string;
}

export interface GradingSchemeRangeResponse {
  id: number;
  uuid: string;
  schemeId: number;
  grade: string;
  minMark: number;
  maxMark: number;
  gradePoint: number | null;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGradingSchemeRangeRequest {
  schemeId: number;
  grade: string;
  minMark: number;
  maxMark: number;
  gradePoint?: number;
  remark?: string;
}

export interface ResultResponse {
  id: number;
  uuid: string;
  schoolId: number;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  examinationSubjectId: number;
  examinationId: number;
  examinationName: string;
  subjectId: number;
  subjectName: string;
  classId: number;
  className: string;
  teacherId: number;
  teacherName: string;
  marksObtained: number | null;
  percentage: number | null;
  grade: string | null;
  gradePoint: number | null;
  remarks: string | null;
  status: string;
  submittedAt: string | null;
  reviewedAt: string | null;
  approvedAt: string | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateResultRequest {
  studentId: number;
  examinationSubjectId: number;
  marksObtained?: number;
  remarks?: string;
}

export interface ResultApprovalResponse {
  id: number;
  uuid: string;
  resultId: number;
  schoolId: number;
  action: string;
  actorUserId: number;
  actorUserName: string;
  actorRole: string;
  comment: string | null;
  oldStatus: string | null;
  newStatus: string;
  createdAt: string;
}

export interface ExaminationReportResponse {
  id: number;
  uuid: string;
  schoolId: number;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  examinationId: number;
  examinationName: string;
  fileKey: string;
  filename: string;
  mimeType: string;
  size: number;
  generatedBy: number;
  generatedByUserName: string;
  generatedAt: string;
  createdAt: string;
  updatedAt: string;
}
