export interface VehicleResponse {
  id: number;
  uuid: string;
  schoolId: number;
  registrationNumber: string;
  vehicleType: string;
  capacity: number;
  driverId: number | null;
  driverName: string | null;
  driverPhone: string | null;
  insuranceNumber: string | null;
  insuranceExpiry: string | null;
  lastServiceDate: string | null;
  nextServiceDate: string | null;
  status: string;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVehicleRequest {
  registrationNumber: string;
  vehicleType: string;
  capacity: number;
  driverId?: number;
  driverName?: string;
  driverPhone?: string;
  insuranceNumber?: string;
  insuranceExpiry?: string;
  lastServiceDate?: string;
  nextServiceDate?: string;
  status?: string;
  remarks?: string;
}

export interface RouteResponse {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  code: string | null;
  startPoint: string;
  endPoint: string;
  distanceKm: number | null;
  estimatedDurationMinutes: number | null;
  morningDeparture: string | null;
  afternoonDeparture: string | null;
  status: string;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRouteRequest {
  name: string;
  code?: string;
  startPoint: string;
  endPoint: string;
  distanceKm?: number;
  estimatedDurationMinutes?: number;
  morningDeparture?: string;
  afternoonDeparture?: string;
  status?: string;
  remarks?: string;
}

export interface RouteStopResponse {
  id: number;
  uuid: string;
  routeId: number;
  schoolId: number;
  name: string;
  stopOrder: number;
  latitude: number | null;
  longitude: number | null;
  arrivalTime: string | null;
  departureTime: string | null;
  status: string;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRouteStopRequest {
  routeId: number;
  name: string;
  stopOrder: number;
  latitude?: number;
  longitude?: number;
  arrivalTime?: string;
  departureTime?: string;
  status?: string;
  remarks?: string;
}

export interface TransportAssignmentResponse {
  id: number;
  uuid: string;
  schoolId: number;
  studentId: number;
  studentName: string;
  routeId: number;
  routeName: string;
  stopId: number;
  stopName: string;
  vehicleId: number;
  vehicleRegistration: string;
  pickupPoint: string | null;
  dropPoint: string | null;
  academicYearId: number | null;
  startDate: string;
  endDate: string | null;
  feeApplicable: boolean;
  monthlyFee: number | null;
  status: string;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTransportAssignmentRequest {
  studentId: number;
  routeId: number;
  stopId: number;
  vehicleId: number;
  pickupPoint?: string;
  dropPoint?: string;
  academicYearId?: number;
  startDate: string;
  endDate?: string;
  feeApplicable?: boolean;
  monthlyFee?: number;
  status?: string;
  remarks?: string;
}
