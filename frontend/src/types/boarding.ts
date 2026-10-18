export interface DormitoryResponse {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  description: string | null;
  capacity: number;
  gender: string | null;
  wardenId: number | null;
  wardenName: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDormitoryRequest {
  name: string;
  description?: string;
  capacity?: number;
  gender?: string;
  wardenId?: number;
  status?: string;
}

export interface RoomResponse {
  id: number;
  uuid: string;
  schoolId: number;
  dormitoryId: number;
  dormitoryName: string | null;
  roomNumber: string;
  capacity: number;
  currentOccupancy: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRoomRequest {
  dormitoryId: number;
  roomNumber: string;
  capacity?: number;
  status?: string;
}

export interface BedResponse {
  id: number;
  uuid: string;
  schoolId: number;
  roomId: number;
  roomNumber: string | null;
  bedNumber: string;
  studentId: number | null;
  studentName: string | null;
  status: string;
  allocatedDate: string | null;
  releasedDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBedRequest {
  roomId: number;
  bedNumber: string;
  status?: string;
}

export interface BoardingStudentResponse {
  id: number;
  uuid: string;
  schoolId: number;
  studentId: number;
  studentName: string | null;
  admissionNumber: string | null;
  dormitoryId: number;
  dormitoryName: string | null;
  roomId: number;
  roomNumber: string | null;
  bedId: number;
  bedNumber: string | null;
  allocatedDate: string;
  releasedDate: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBoardingStudentRequest {
  studentId: number;
  dormitoryId: number;
  roomId: number;
  bedId: number;
  status?: string;
}

export interface BoardingAttendanceResponse {
  id: number;
  uuid: string;
  schoolId: number;
  studentId: number;
  studentName: string | null;
  admissionNumber: string | null;
  dormitoryId: number;
  dormitoryName: string | null;
  attendanceDate: string;
  status: string;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBoardingAttendanceRequest {
  studentId: number;
  dormitoryId: number;
  attendanceDate: string;
  status: string;
  remarks?: string;
}
