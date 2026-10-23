import { api, unwrapApiResponse } from './api';
import type { ApiResponse } from '@/types/api';
import type {
  AccountantDashboard,
  HodDashboard,
  LibrarianDashboard,
  ParentDashboard,
  TransportOfficerDashboard,
  WardenDashboard,
} from '@/types/dashboards';

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
