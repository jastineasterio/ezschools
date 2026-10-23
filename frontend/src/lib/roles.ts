const ROLE_PREFIX = 'ROLE_';

export const ROLE_DASHBOARD_ROUTES = {
  SUPER_ADMIN: '/platform/dashboard',
  MANAGER: '/school/dashboard',
  TEACHER: '/school/teacher/dashboard',
  STUDENT: '/school/student/dashboard',
  PARENT: '/school/parent/dashboard',
  ACCOUNTANT: '/school/accountant/dashboard',
  LIBRARIAN: '/school/librarian/dashboard',
  HOD: '/school/hod/dashboard',
  WARDEN: '/school/warden/dashboard',
  TRANSPORT_OFFICER: '/school/transport-officer/dashboard',
} as const;

export type DashboardRole = keyof typeof ROLE_DASHBOARD_ROUTES;

export const SCHOOL_DASHBOARD_ROLES = [
  'TEACHER',
  'STUDENT',
  'PARENT',
  'ACCOUNTANT',
  'LIBRARIAN',
  'HOD',
  'WARDEN',
  'TRANSPORT_OFFICER',
] as const satisfies readonly DashboardRole[];

export function normalizeRole(role: string): string {
  const normalized = role.trim().toUpperCase();
  return normalized.startsWith(ROLE_PREFIX)
    ? normalized.slice(ROLE_PREFIX.length)
    : normalized;
}

export function hasRole(
  userRoles: readonly string[] | undefined,
  expectedRole: string
): boolean {
  const normalizedExpected = normalizeRole(expectedRole);
  return (userRoles ?? []).some(
    (role) => normalizeRole(role) === normalizedExpected
  );
}

export function getDashboardPath(
  userRoles: readonly string[] | undefined
): string {
  const normalizedRoles = new Set((userRoles ?? []).map(normalizeRole));
  const priority: DashboardRole[] = [
    'SUPER_ADMIN',
    'MANAGER',
    ...SCHOOL_DASHBOARD_ROLES,
  ];
  const matchingRole = priority.find((role) => normalizedRoles.has(role));

  return matchingRole
    ? ROLE_DASHBOARD_ROUTES[matchingRole]
    : '/access-unavailable';
}

export function getRoleDisplayName(role: DashboardRole): string {
  const displayNames: Record<DashboardRole, string> = {
    SUPER_ADMIN: 'Super Admin',
    MANAGER: 'Manager',
    TEACHER: 'Teacher',
    STUDENT: 'Student',
    PARENT: 'Parent/Guardian',
    ACCOUNTANT: 'Accountant',
    LIBRARIAN: 'Librarian',
    HOD: 'Head of Department',
    WARDEN: 'Warden',
    TRANSPORT_OFFICER: 'Transport Officer',
  };

  return displayNames[role];
}
