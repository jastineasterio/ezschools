import { api } from './api';
import type { PaginatedResponse } from '@/types/api';
import type { FeeStructure, CreateFeeStructureRequest, FeeAssignment, CreateFeeAssignmentRequest, Payment, CreatePaymentRequest } from '@/types/fees';

export async function getFeeStructures(page = 0, size = 20): Promise<PaginatedResponse<FeeStructure>> {
  const { data } = await api.get(`/school/fee-structures?page=${page}&size=${size}`);
  return data.data;
}

export async function getFeeStructure(id: number) {
  const { data } = await api.get(`/school/fee-structures/${id}`);
  return data.data;
}

export async function createFeeStructure(payload: CreateFeeStructureRequest) {
  const { data } = await api.post('/school/fee-structures', payload);
  return data.data;
}

export async function updateFeeStructure(id: number, payload: CreateFeeStructureRequest) {
  const { data } = await api.put(`/school/fee-structures/${id}`, payload);
  return data.data;
}

export async function getFeeAssignments(): Promise<FeeAssignment[]> {
  const { data } = await api.get('/school/fee-assignments');
  return data.data;
}

export async function getStudentFeeAssignments(studentId: number): Promise<FeeAssignment[]> {
  const { data } = await api.get(`/school/fee-assignments/student/${studentId}`);
  return data.data;
}

export async function assignFee(payload: CreateFeeAssignmentRequest) {
  const { data } = await api.post('/school/fee-assignments', payload);
  return data.data;
}

export async function bulkAssignFee(feeStructureId: number, studentIds: number[]) {
  const { data } = await api.post(`/school/fee-assignments/bulk?feeStructureId=${feeStructureId}`, studentIds);
  return data;
}

export async function getPayments(page = 0, size = 20): Promise<PaginatedResponse<Payment>> {
  const { data } = await api.get(`/school/payments?page=${page}&size=${size}`);
  return data.data;
}

export async function getStudentPayments(studentId: number): Promise<Payment[]> {
  const { data } = await api.get(`/school/payments/student/${studentId}`);
  return data.data;
}

export async function createPayment(payload: CreatePaymentRequest) {
  const { data } = await api.post('/school/payments', payload);
  return data.data;
}


