export interface ApiError {
  success: false;
  message: string;
  errors?: Record<string, string>;
  timestamp: string;
  path: string;
}

export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  message?: string;
  errors?: Record<string, string>;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  schoolId: number | null;
}

export interface CurrentUserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  schoolId: number | null;
  roles: string[];
  school?: SchoolResponse;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface SchoolResponse {
  id: number;
  uuid: string;
  name: string;
  registrationNumber: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  region: string | null;
  district: string | null;
  isActive: boolean;
  isSuspended: boolean;
  suspendedAt: string | null;
  suspendedReason: string | null;
  createdAt: string;
  updatedAt: string;
  userCount?: number;
  package?: PackageSummaryResponse;
  subscription?: SubscriptionSummaryResponse;
}

export interface CreateSchoolRequest {
  name: string;
  registrationNumber: string;
  email?: string;
  phone?: string;
  address?: string;
  region?: string;
  district?: string;
}

export interface UpdateSchoolRequest {
  name?: string;
  registrationNumber?: string;
  email?: string;
  phone?: string;
  address?: string;
  region?: string;
  district?: string;
}

export interface PackageResponse {
  id: number;
  uuid: string;
  name: string;
  displayName: string;
  description: string | null;
  price: number;
  billingPeriod: string;
  active: boolean;
  default: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
  roles: PackageRoleResponse[];
  features: PackageFeatureResponse[];
  limits: PackageLimitResponse[];
  schoolCount?: number;
}

export interface PackageSummaryResponse {
  id: number;
  name: string;
  displayName: string;
  price: number;
  billingPeriod: string;
}

export interface PackageRoleResponse {
  id: number;
  roleName: string;
}

export interface PackageFeatureResponse {
  id: number;
  featureKey: string;
  featureName: string;
  isEnabled: boolean;
}

export interface PackageLimitResponse {
  id: number;
  limitKey: string;
  limitName: string;
  maxValue: number;
  currentValue: number;
  unit: string | null;
}

export interface CreatePackageRequest {
  name: string;
  displayName: string;
  description?: string;
  price: number;
  billingPeriod?: string;
  active?: boolean;
  default?: boolean;
  sortOrder?: number;
}

export interface SubscriptionResponse {
  id: number;
  uuid: string;
  schoolId: number;
  packageId: number;
  status: string;
  startDate: string;
  endDate: string | null;
  billingPeriod: string;
  price: number;
  paymentStatus: string;
  trialEndsAt: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  createdAt: string;
  updatedAt: string;
  school?: SchoolResponse;
  package?: PackageSummaryResponse;
}

export interface SubscriptionSummaryResponse {
  id: number;
  status: string;
  startDate: string;
  endDate: string | null;
  billingPeriod: string;
  price: number;
  paymentStatus: string;
  package: PackageSummaryResponse;
}

export interface SchoolBrandingResponse {
  id: number;
  schoolId: number;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  backgroundColor: string;
  textColor: string;
  logoUrl: string | null;
  faviconUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateBrandingRequest {
  primaryColor?: string;
  secondaryColor?: string;
  accentColor?: string;
  backgroundColor?: string;
  textColor?: string;
  logoUrl?: string;
  faviconUrl?: string;
}

export interface AuditLogResponse {
  id: number;
  actorUserId: number | null;
  actorEmail?: string;
  schoolId: number | null;
  schoolName?: string;
  action: string;
  entityType: string | null;
  entityId: number | null;
  description: string;
  ipAddress: string | null;
  userAgent: string | null;
  metadata: Record<string, unknown> | null;
  createdAt: string;
}

export interface PlatformDashboardResponse {
  totalSchools: number;
  activeSchools: number;
  suspendedSchools: number;
  totalUsers: number;
  activeSubscriptions: number;
  packageDistribution: { name: string; count: number }[];
  recentSchools: SchoolResponse[];
  recentAuditLogs: AuditLogResponse[];
}

export interface RoleResponse {
  id: number;
  name: string;
  displayName: string;
  description: string | null;
  isSystem: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  permissions: PermissionResponse[];
}

export interface CreateRoleRequest {
  name: string;
  displayName: string;
  description?: string;
  isSystem?: boolean;
  permissionIds: number[];
}

export interface PermissionResponse {
  id: number;
  name: string;
  displayName: string;
  description: string | null;
  category: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface SchoolUserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  roles: string[];
  schoolId: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSchoolUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roles: string[];
  password: string;
}

export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  schoolId: number | null;
  schoolName?: string;
  roles: string[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  roles?: string[];
  isActive?: boolean;
}
