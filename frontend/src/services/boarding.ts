import { api } from './api';
import type {
  DormitoryResponse,
  CreateDormitoryRequest,
  RoomResponse,
  CreateRoomRequest,
  BedResponse,
  CreateBedRequest,
  BoardingStudentResponse,
  CreateBoardingStudentRequest,
  BoardingAttendanceResponse,
  CreateBoardingAttendanceRequest,
} from '@/types/boarding';

export async function getDormitories(page = 0, size = 20): Promise<{ data: DormitoryResponse[]; total: number }> {
  const { data } = await api.get(`/school/boarding/dormitories?page=${page}&size=${size}`);
  const items = data.data as DormitoryResponse[];
  return { data: items, total: items.length };
}

export async function getActiveDormitories(): Promise<DormitoryResponse[]> {
  const { data } = await api.get('/school/boarding/dormitories/active');
  return data.data as DormitoryResponse[];
}

export async function getDormitory(id: number): Promise<DormitoryResponse> {
  const { data } = await api.get(`/school/boarding/dormitories/${id}`);
  return data.data as DormitoryResponse;
}

export async function createDormitory(payload: CreateDormitoryRequest): Promise<DormitoryResponse> {
  const { data } = await api.post('/school/boarding/dormitories', payload);
  return data.data as DormitoryResponse;
}

export async function updateDormitory(id: number, payload: CreateDormitoryRequest): Promise<DormitoryResponse> {
  const { data } = await api.put(`/school/boarding/dormitories/${id}`, payload);
  return data.data as DormitoryResponse;
}

export async function deleteDormitory(id: number): Promise<void> {
  await api.delete(`/school/boarding/dormitories/${id}`);
}

export async function getRooms(page = 0, size = 20): Promise<{ data: RoomResponse[]; total: number }> {
  const { data } = await api.get(`/school/boarding/rooms?page=${page}&size=${size}`);
  const items = data.data as RoomResponse[];
  return { data: items, total: items.length };
}

export async function getRoomsByDormitory(dormitoryId: number): Promise<RoomResponse[]> {
  const { data } = await api.get(`/school/boarding/rooms/dormitory/${dormitoryId}`);
  return data.data as RoomResponse[];
}

export async function getRoom(id: number): Promise<RoomResponse> {
  const { data } = await api.get(`/school/boarding/rooms/${id}`);
  return data.data as RoomResponse;
}

export async function createRoom(payload: CreateRoomRequest): Promise<RoomResponse> {
  const { data } = await api.post('/school/boarding/rooms', payload);
  return data.data as RoomResponse;
}

export async function updateRoom(id: number, payload: CreateRoomRequest): Promise<RoomResponse> {
  const { data } = await api.put(`/school/boarding/rooms/${id}`, payload);
  return data.data as RoomResponse;
}

export async function deleteRoom(id: number): Promise<void> {
  await api.delete(`/school/boarding/rooms/${id}`);
}

export async function getBeds(page = 0, size = 20): Promise<{ data: BedResponse[]; total: number }> {
  const { data } = await api.get(`/school/boarding/beds?page=${page}&size=${size}`);
  const items = data.data as BedResponse[];
  return { data: items, total: items.length };
}

export async function getBedsByRoom(roomId: number): Promise<BedResponse[]> {
  const { data } = await api.get(`/school/boarding/beds/room/${roomId}`);
  return data.data as BedResponse[];
}

export async function getBed(id: number): Promise<BedResponse> {
  const { data } = await api.get(`/school/boarding/beds/${id}`);
  return data.data as BedResponse;
}

export async function createBed(payload: CreateBedRequest): Promise<BedResponse> {
  const { data } = await api.post('/school/boarding/beds', payload);
  return data.data as BedResponse;
}

export async function updateBed(id: number, payload: CreateBedRequest): Promise<BedResponse> {
  const { data } = await api.put(`/school/boarding/beds/${id}`, payload);
  return data.data as BedResponse;
}

export async function deleteBed(id: number): Promise<void> {
  await api.delete(`/school/boarding/beds/${id}`);
}

export async function getBoardingStudents(page = 0, size = 20): Promise<{ data: BoardingStudentResponse[]; total: number }> {
  const { data } = await api.get(`/school/boarding/students?page=${page}&size=${size}`);
  const items = data.data as BoardingStudentResponse[];
  return { data: items, total: items.length };
}

export async function getActiveBoardingStudents(): Promise<BoardingStudentResponse[]> {
  const { data } = await api.get('/school/boarding/students/active');
  return data.data as BoardingStudentResponse[];
}

export async function getBoardingStudent(id: number): Promise<BoardingStudentResponse> {
  const { data } = await api.get(`/school/boarding/students/${id}`);
  return data.data as BoardingStudentResponse;
}

export async function allocateBoardingStudent(payload: CreateBoardingStudentRequest): Promise<BoardingStudentResponse> {
  const { data } = await api.post('/school/boarding/students/allocate', payload);
  return data.data as BoardingStudentResponse;
}

export async function transferBoardingStudent(id: number, payload: CreateBoardingStudentRequest): Promise<BoardingStudentResponse> {
  const { data } = await api.put(`/school/boarding/students/${id}/transfer`, payload);
  return data.data as BoardingStudentResponse;
}

export async function releaseBoardingStudent(id: number): Promise<BoardingStudentResponse> {
  const { data } = await api.post(`/school/boarding/students/${id}/release`);
  return data.data as BoardingStudentResponse;
}

export async function getBoardingAttendance(page = 0, size = 20): Promise<{ data: BoardingAttendanceResponse[]; total: number }> {
  const { data } = await api.get(`/school/boarding/attendance?page=${page}&size=${size}`);
  const items = data.data as BoardingAttendanceResponse[];
  return { data: items, total: items.length };
}

export async function getBoardingAttendanceByDormitoryAndDate(
  dormitoryId: number,
  date: string
): Promise<BoardingAttendanceResponse[]> {
  const { data } = await api.get(`/school/boarding/attendance/dormitory/${dormitoryId}/date/${date}`);
  return data.data as BoardingAttendanceResponse[];
}

export async function getBoardingAttendanceRecord(id: number): Promise<BoardingAttendanceResponse> {
  const { data } = await api.get(`/school/boarding/attendance/${id}`);
  return data.data as BoardingAttendanceResponse;
}

export async function createBoardingAttendance(payload: CreateBoardingAttendanceRequest): Promise<BoardingAttendanceResponse> {
  const { data } = await api.post('/school/boarding/attendance', payload);
  return data.data as BoardingAttendanceResponse;
}

export async function updateBoardingAttendance(
  id: number,
  payload: CreateBoardingAttendanceRequest
): Promise<BoardingAttendanceResponse> {
  const { data } = await api.put(`/school/boarding/attendance/${id}`, payload);
  return data.data as BoardingAttendanceResponse;
}

export async function deleteBoardingAttendance(id: number): Promise<void> {
  await api.delete(`/school/boarding/attendance/${id}`);
}
