import { api as http, createApiResponseClient } from './api';
import type { PaginatedResponse } from '@/types/api';
import type {
  VehicleResponse,
  CreateVehicleRequest,
  RouteResponse,
  CreateRouteRequest,
  RouteStopResponse,
  CreateRouteStopRequest,
  TransportAssignmentResponse,
  CreateTransportAssignmentRequest,
} from '@/types/transport';

const api = createApiResponseClient(http);

export async function getVehicles(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<VehicleResponse>>('/school/transport/vehicles', { params });
  return data;
}

export async function getVehicle(id: number) {
  const { data } = await api.get<VehicleResponse>(`/school/transport/vehicles/${id}`);
  return data;
}

export async function createVehicle(data: CreateVehicleRequest) {
  const response = await api.post<VehicleResponse>('/school/transport/vehicles', data);
  return response.data;
}

export async function updateVehicle(id: number, data: CreateVehicleRequest) {
  const response = await api.put<VehicleResponse>(`/school/transport/vehicles/${id}`, data);
  return response.data;
}

export async function deleteVehicle(id: number) {
  await api.delete(`/school/transport/vehicles/${id}`);
}

export async function getRoutes(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<RouteResponse>>('/school/transport/routes', { params });
  return data;
}

export async function getRoute(id: number) {
  const { data } = await api.get<RouteResponse>(`/school/transport/routes/${id}`);
  return data;
}

export async function createRoute(data: CreateRouteRequest) {
  const response = await api.post<RouteResponse>('/school/transport/routes', data);
  return response.data;
}

export async function updateRoute(id: number, data: CreateRouteRequest) {
  const response = await api.put<RouteResponse>(`/school/transport/routes/${id}`, data);
  return response.data;
}

export async function deleteRoute(id: number) {
  await api.delete(`/school/transport/routes/${id}`);
}

export async function getRouteStops(routeId: number, page?: number, size?: number) {
  const params: Record<string, number> = { routeId };
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<RouteStopResponse>>(`/school/transport/routes/${routeId}/stops`, { params });
  return data;
}

export async function getRouteStop(routeId: number, stopId: number) {
  const { data } = await api.get<RouteStopResponse>(`/school/transport/routes/${routeId}/stops/${stopId}`);
  return data;
}

export async function createRouteStop(routeId: number, data: CreateRouteStopRequest) {
  const response = await api.post<RouteStopResponse>(`/school/transport/routes/${routeId}/stops`, data);
  return response.data;
}

export async function updateRouteStop(routeId: number, stopId: number, data: CreateRouteStopRequest) {
  const response = await api.put<RouteStopResponse>(`/school/transport/routes/${routeId}/stops/${stopId}`, data);
  return response.data;
}

export async function deleteRouteStop(routeId: number, stopId: number) {
  await api.delete(`/school/transport/routes/${routeId}/stops/${stopId}`);
}

export async function getTransportAssignments(page?: number, size?: number) {
  const params: Record<string, number> = {};
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<TransportAssignmentResponse>>('/school/transport/assignments', { params });
  return data;
}

export async function getTransportAssignment(id: number) {
  const { data } = await api.get<TransportAssignmentResponse>(`/school/transport/assignments/${id}`);
  return data;
}

export async function getTransportAssignmentsByStudent(studentId: number, page?: number, size?: number) {
  const params: Record<string, number> = { studentId };
  if (page !== undefined) params.page = page;
  if (size !== undefined) params.size = size;
  const { data } = await api.get<PaginatedResponse<TransportAssignmentResponse>>(`/school/transport/assignments/student/${studentId}`, { params });
  return data;
}

export async function createTransportAssignment(data: CreateTransportAssignmentRequest) {
  const response = await api.post<TransportAssignmentResponse>('/school/transport/assignments', data);
  return response.data;
}

export async function updateTransportAssignment(id: number, data: CreateTransportAssignmentRequest) {
  const response = await api.put<TransportAssignmentResponse>(`/school/transport/assignments/${id}`, data);
  return response.data;
}

export async function deleteTransportAssignment(id: number) {
  await api.delete(`/school/transport/assignments/${id}`);
}
