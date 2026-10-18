import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '@/layouts/ProtectedRoute';

function hasRole(userRoles: string[] | undefined, expectedRole: string): boolean {
  const normalizedExpected = expectedRole.trim().toUpperCase();
  return (userRoles ?? []).some((role) => {
    const normalized = role.trim().toUpperCase();
    return normalized === normalizedExpected || normalized === `ROLE_${normalizedExpected}`;
  });
}
import { PlatformLayout } from '@/layouts/PlatformLayout';
import { SchoolLayout } from '@/layouts/SchoolLayout';
import { LoginPage } from '@/pages/LoginPage';
import { PlatformDashboardPage } from '@/pages/PlatformDashboardPage';
import { SchoolsPage } from '@/pages/SchoolsPage';
import { SchoolDetailPage } from '@/pages/SchoolDetailPage';
import { SchoolFormPage } from '@/pages/SchoolFormPage';
import { PackagesPage } from '@/pages/PackagesPage';
import { PackageFormPage } from '@/pages/PackageFormPage';
import { SubscriptionsPage } from '@/pages/SubscriptionsPage';
import { PlatformUsersPage } from '@/pages/PlatformUsersPage';
import { AuditLogsPage } from '@/pages/AuditLogsPage';
import { PlatformSettingsPage } from '@/pages/PlatformSettingsPage';
import { PlatformSmsConfigurationsPage } from '@/pages/PlatformSmsConfigurationsPage';
import { PlatformNotificationManagementPage } from '@/pages/PlatformNotificationManagementPage';
import { PlatformAnalyticsPage } from '@/pages/PlatformAnalyticsPage';
import { ManagerDashboardPage } from '@/pages/ManagerDashboardPage';
import { ManagerSchoolPage } from '@/pages/ManagerSchoolPage';
import { ManagerBrandingPage } from '@/pages/ManagerBrandingPage';
import { ManagerAcademicYearsPage } from '@/pages/ManagerAcademicYearsPage';
import { ManagerClassesPage } from '@/pages/ManagerClassesPage';
import { ManagerStreamsPage } from '@/pages/ManagerStreamsPage';
import { ManagerSubjectsPage } from '@/pages/ManagerSubjectsPage';
import { ManagerDepartmentsPage } from '@/pages/ManagerDepartmentsPage';
import { ManagerStudentsPage } from '@/pages/ManagerStudentsPage';
import { ManagerTeachersPage } from '@/pages/ManagerTeachersPage';
import { ManagerParentsPage } from '@/pages/ManagerParentsPage';
import { ManagerAttendancePage } from '@/pages/ManagerAttendancePage';
import { ManagerExaminationsPage } from '@/pages/ManagerExaminationsPage';
import { ManagerResultsPage } from '@/pages/ManagerResultsPage';
import { ManagerFinancePage } from '@/pages/ManagerFinancePage';
import { ManagerFeeStructuresPage } from '@/pages/ManagerFeeStructuresPage';
import { ManagerFeeAssignmentsPage } from '@/pages/ManagerFeeAssignmentsPage';
import { ManagerPaymentsPage } from '@/pages/ManagerPaymentsPage';
import { ManagerSmsPage } from '@/pages/ManagerSmsPage';
import { ManagerLibraryPage } from '@/pages/ManagerLibraryPage';
import { ManagerTransportPage } from '@/pages/ManagerTransportPage';
import { ManagerNotificationsPage } from '@/pages/ManagerNotificationsPage';
import { ManagerReportsPage } from '@/pages/ManagerReportsPage';
import { ManagerBoardingPage } from '@/pages/ManagerBoardingPage';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { useAuth } from '@/store/AuthContext';

function RoleRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (hasRole(user.roles, 'SUPER_ADMIN')) {
    return <Navigate to="/platform/dashboard" replace />;
  }
  return <Navigate to="/school/dashboard" replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RoleRedirect />} />

      <Route element={<ProtectedRoute roles={['SUPER_ADMIN']} />}>
        <Route path="/platform" element={<PlatformLayout />}>
          <Route path="dashboard" element={<PlatformDashboardPage />} />
          <Route path="schools" element={<SchoolsPage />} />
          <Route path="schools/:id" element={<SchoolDetailPage />} />
          <Route path="schools/new" element={<SchoolFormPage />} />
          <Route path="schools/:id/edit" element={<SchoolFormPage />} />
          <Route path="packages" element={<PackagesPage />} />
          <Route path="packages/new" element={<PackageFormPage />} />
          <Route path="packages/:id/edit" element={<PackageFormPage />} />
          <Route path="subscriptions" element={<SubscriptionsPage />} />
          <Route path="users" element={<PlatformUsersPage />} />
          <Route path="audit-logs" element={<AuditLogsPage />} />
          <Route path="analytics" element={<PlatformAnalyticsPage />} />
          <Route path="settings" element={<PlatformSettingsPage />} />
          <Route path="sms-configurations" element={<PlatformSmsConfigurationsPage />} />
          <Route path="notifications" element={<PlatformNotificationManagementPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['MANAGER']} />}>
        <Route path="/school" element={<SchoolLayout />}>
          <Route path="dashboard" element={<ManagerDashboardPage />} />
          <Route path="profile" element={<ManagerSchoolPage />} />
          <Route path="branding" element={<ManagerBrandingPage />} />
          <Route path="academic-years" element={<ManagerAcademicYearsPage />} />
          <Route path="classes" element={<ManagerClassesPage />} />
          <Route path="streams" element={<ManagerStreamsPage />} />
          <Route path="subjects" element={<ManagerSubjectsPage />} />
          <Route path="departments" element={<ManagerDepartmentsPage />} />
          <Route path="students" element={<ManagerStudentsPage />} />
          <Route path="teachers" element={<ManagerTeachersPage />} />
          <Route path="parents" element={<ManagerParentsPage />} />
          <Route path="attendance" element={<ManagerAttendancePage />} />
          <Route path="examinations" element={<ManagerExaminationsPage />} />
          <Route path="results" element={<ManagerResultsPage />} />
          <Route path="finance" element={<ManagerFinancePage />} />
          <Route path="fee-structures" element={<ManagerFeeStructuresPage />} />
          <Route path="fee-assignments" element={<ManagerFeeAssignmentsPage />} />
          <Route path="payments" element={<ManagerPaymentsPage />} />
          <Route path="sms" element={<ManagerSmsPage />} />
          <Route path="library" element={<ManagerLibraryPage />} />
          <Route path="transport" element={<ManagerTransportPage />} />
          <Route path="notifications" element={<ManagerNotificationsPage />} />
          <Route path="reports" element={<ManagerReportsPage />} />
          <Route path="boarding" element={<ManagerBoardingPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
