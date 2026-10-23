import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '@/layouts/ProtectedRoute';
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
import { ManagerTimetablePage } from '@/pages/ManagerTimetablePage';
import { ManagerBrandingPage } from '@/pages/ManagerBrandingPage';
import { ManagerAcademicYearsPage } from '@/pages/ManagerAcademicYearsPage';
import { ManagerClassesPage } from '@/pages/ManagerClassesPage';
import { ManagerStreamsPage } from '@/pages/ManagerStreamsPage';
import { ManagerSubjectsPage } from '@/pages/ManagerSubjectsPage';
import { ManagerDepartmentsPage } from '@/pages/ManagerDepartmentsPage';
import { ManagerStudentsPage } from '@/pages/ManagerStudentsPage';
import { ManagerEnrollmentsPage } from '@/pages/ManagerEnrollmentsPage';
import { ManagerTeachersPage } from '@/pages/ManagerTeachersPage';
import { ManagerTeacherAssignmentsPage } from '@/pages/ManagerTeacherAssignmentsPage';
import { ManagerStreamSubjectsPage } from '@/pages/ManagerStreamSubjectsPage';
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
import { ManagerUsersPage } from '@/pages/ManagerUsersPage';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { RoleDashboardPage } from '@/pages/RoleDashboardPage';
import { TeacherDashboardPage } from '@/pages/TeacherDashboardPage';
import { StudentDashboardPage } from '@/pages/StudentDashboardPage';
import { ManagerCertificatesPage } from '@/pages/ManagerCertificatesPage';
import { StudentCertificatesPage } from '@/pages/StudentCertificatesPage';
import { ChangePasswordPage } from '@/pages/ChangePasswordPage';
import { ParentDashboardPage } from '@/pages/ParentDashboardPage';
import { AccountantDashboardPage } from '@/pages/AccountantDashboardPage';
import { LibrarianDashboardPage } from '@/pages/LibrarianDashboardPage';
import { HodDashboardPage } from '@/pages/HodDashboardPage';
import { WardenDashboardPage } from '@/pages/WardenDashboardPage';
import { TransportOfficerDashboardPage } from '@/pages/TransportOfficerDashboardPage';
import { StaffLayout } from '@/layouts/StaffLayout';
import { useAuth } from '@/store/AuthContext';
import { getDashboardPath } from '@/lib/roles';

function RoleRedirect() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={getDashboardPath(user.roles)} replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<RoleRedirect />} />
        <Route
          path="/access-unavailable"
          element={<RoleDashboardPage />}
        />
      </Route>

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
          <Route path="users" element={<ManagerUsersPage />} />
          <Route path="profile" element={<ManagerSchoolPage />} />
          <Route path="branding" element={<ManagerBrandingPage />} />
          <Route path="academic-years" element={<ManagerAcademicYearsPage />} />
          <Route path="classes" element={<ManagerClassesPage />} />
          <Route path="streams" element={<ManagerStreamsPage />} />
          <Route path="subjects" element={<ManagerSubjectsPage />} />
          <Route path="stream-subjects" element={<ManagerStreamSubjectsPage />} />
          <Route path="departments" element={<ManagerDepartmentsPage />} />
          <Route path="students" element={<ManagerStudentsPage />} />
          <Route path="enrollments" element={<ManagerEnrollmentsPage />} />
          <Route path="teachers" element={<ManagerTeachersPage />} />
          <Route path="teacher-assignments" element={<ManagerTeacherAssignmentsPage />} />
          <Route path="timetable" element={<ManagerTimetablePage />} />
          <Route path="parents" element={<ManagerParentsPage />} />
          <Route path="attendance" element={<ManagerAttendancePage />} />
          <Route path="examinations" element={<ManagerExaminationsPage />} />
          <Route path="results" element={<ManagerResultsPage />} />
          <Route path="certificates" element={<ManagerCertificatesPage />} />
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

      <Route element={<ProtectedRoute roles={['TEACHER']} />}>
        <Route path="/school/teacher" element={<StaffLayout role="TEACHER" />}>
          <Route path="dashboard" element={<TeacherDashboardPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['STUDENT']} />}>
        <Route path="/school/student" element={<StaffLayout role="STUDENT" />}>
          <Route path="dashboard" element={<StudentDashboardPage />} />
          <Route path="certificates" element={<StudentCertificatesPage />} />
          <Route path="change-password" element={<ChangePasswordPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['PARENT']} />}>
        <Route path="/school/parent" element={<StaffLayout role="PARENT" />}>
          <Route path="dashboard" element={<ParentDashboardPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['ACCOUNTANT']} />}>
        <Route path="/school/accountant" element={<StaffLayout role="ACCOUNTANT" />}>
          <Route path="dashboard" element={<AccountantDashboardPage />} />
          <Route path="payments" element={<ManagerPaymentsPage />} />
          <Route path="fee-assignments" element={<ManagerFeeAssignmentsPage />} />
          <Route path="fee-structures" element={<ManagerFeeStructuresPage />} />
          <Route path="finance" element={<ManagerFinancePage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['LIBRARIAN']} />}>
        <Route path="/school/librarian" element={<StaffLayout role="LIBRARIAN" />}>
          <Route path="dashboard" element={<LibrarianDashboardPage />} />
          <Route path="library" element={<ManagerLibraryPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['HOD']} />}>
        <Route path="/school/hod" element={<StaffLayout role="HOD" />}>
          <Route path="dashboard" element={<HodDashboardPage />} />
          <Route path="departments" element={<ManagerDepartmentsPage />} />
          <Route path="subjects" element={<ManagerSubjectsPage />} />
          <Route path="examinations" element={<ManagerExaminationsPage />} />
          <Route path="results" element={<ManagerResultsPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['WARDEN']} />}>
        <Route path="/school/warden" element={<StaffLayout role="WARDEN" />}>
          <Route path="dashboard" element={<WardenDashboardPage />} />
          <Route path="boarding" element={<ManagerBoardingPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['TRANSPORT_OFFICER']} />}>
        <Route
          path="/school/transport-officer"
          element={<StaffLayout role="TRANSPORT_OFFICER" />}
        >
          <Route path="dashboard" element={<TransportOfficerDashboardPage />} />
          <Route path="transport" element={<ManagerTransportPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
