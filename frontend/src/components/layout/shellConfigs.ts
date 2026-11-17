import type { ElementType } from 'react';
import {
  LayoutDashboard,
  School,
  Users,
  CalendarDays,
  BookOpen,
  BookOpenCheck,
  GraduationCap,
  FileText,
  UserCheck,
  Wallet,
  Receipt,
  CreditCard,
  Building2,
  MessageSquare,
  Bell,
  ClipboardCheck,
  ClipboardList,
  Package,
  Settings,
  BarChart3,
  Award,
  KeyRound,
  Bus,
  User,
  Calendar,
  Megaphone,
  NotebookPen,
  Smartphone,
} from 'lucide-react';
import { getRoleDisplayName, type DashboardRole } from '@/lib/roles';

export interface ShellMenuItem {
  to: string;
  icon: ElementType;
  label: string;
}

export interface ShellMenuSection {
  title: string;
  items: ShellMenuItem[];
}

export interface ShellConfig {
  sections?: ShellMenuSection[];
  items?: ShellMenuItem[];
  brandName: string;
  headerTitle: string;
  headerDescription?: string;
  userRole: string;
}

export const schoolSections: ShellMenuSection[] = [
  {
    title: 'Main',
    items: [
      { to: '/school/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
      { to: '/school/profile', icon: School, label: 'School Profile' },
    ],
  },
  {
    title: 'Academic',
    items: [
      { to: '/school/academic-years', icon: CalendarDays, label: 'Academic Years' },
      { to: '/school/classes', icon: BookOpen, label: 'Classes' },
      { to: '/school/streams', icon: BookOpen, label: 'Streams' },
      { to: '/school/subjects', icon: BookOpen, label: 'Subjects' },
      { to: '/school/stream-subjects', icon: BookOpenCheck, label: 'Stream Subjects' },
      { to: '/school/departments', icon: Users, label: 'Departments' },
    ],
  },
  {
    title: 'People',
    items: [
      { to: '/school/students', icon: Users, label: 'Students' },
      { to: '/school/enrollments', icon: ClipboardCheck, label: 'Enrollments' },
      { to: '/school/teachers', icon: UserCheck, label: 'Teachers' },
      { to: '/school/teacher-assignments', icon: BookOpenCheck, label: 'Teaching Assignments' },
      { to: '/school/timetable', icon: CalendarDays, label: 'Timetable' },
      { to: '/school/parents', icon: Users, label: 'Parents/Guardians' },
    ],
  },
  {
    title: 'Boarding',
    items: [{ to: '/school/boarding', icon: Building2, label: 'Boarding' }],
  },
  {
    title: 'Assessments',
    items: [
      { to: '/school/attendance', icon: ClipboardCheck, label: 'Attendance' },
      { to: '/school/examinations', icon: GraduationCap, label: 'Examinations' },
      { to: '/school/grading-schemes', icon: Award, label: 'Grading Schemes' },
      { to: '/school/examination-types', icon: ClipboardList, label: 'Examination Types' },
      { to: '/school/results', icon: FileText, label: 'Results' },
      { to: '/school/certificates', icon: GraduationCap, label: 'Certificates' },
    ],
  },
  {
    title: 'Finance',
    items: [
      { to: '/school/finance', icon: Wallet, label: 'Finance' },
      { to: '/school/fee-structures', icon: Receipt, label: 'Fee Structures' },
      { to: '/school/fee-assignments', icon: CreditCard, label: 'Fee Assignments' },
      { to: '/school/payments', icon: CreditCard, label: 'Payments' },
    ],
  },
  {
    title: 'Communications',
    items: [
      { to: '/school/sms', icon: MessageSquare, label: 'SMS' },
      { to: '/school/results-notifications', icon: Smartphone, label: 'Results Notifications' },
      { to: '/school/notifications', icon: Bell, label: 'Notifications' },
    ],
  },
  {
    title: 'System',
    items: [{ to: '/school/users', icon: Users, label: 'Users' }],
  },
];

export const schoolConfig: ShellConfig = {
  sections: schoolSections,
  brandName: 'School',
  headerTitle: 'School Management',
  userRole: 'School Manager',
};

export const platformItems: ShellMenuItem[] = [
  { to: '/platform/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/platform/schools', icon: School, label: 'Schools' },
  { to: '/platform/packages', icon: Package, label: 'Packages' },
  { to: '/platform/subscriptions', icon: CreditCard, label: 'Subscriptions' },
  { to: '/platform/users', icon: Users, label: 'Platform Users' },
  { to: '/platform/audit-logs', icon: FileText, label: 'Audit Logs' },
  { to: '/platform/analytics', icon: BarChart3, label: 'Analytics' },
  { to: '/platform/settings', icon: Settings, label: 'Settings' },
  { to: '/platform/sms-configurations', icon: MessageSquare, label: 'SMS Configurations' },
  { to: '/platform/notifications', icon: Bell, label: 'Notification Management' },
];

export const platformConfig: ShellConfig = {
  items: platformItems,
  brandName: 'EduManage',
  headerTitle: 'Platform Administration',
  headerDescription:
    'Manage schools, subscriptions, users and platform settings',
  userRole: 'Super Admin',
};

const navByRole: Record<DashboardRole, ShellMenuItem[]> = {
  SUPER_ADMIN: [{ to: '/platform/dashboard', label: 'Dashboard', icon: LayoutDashboard }],
  MANAGER: [{ to: '/school/dashboard', label: 'Dashboard', icon: LayoutDashboard }],
  TEACHER: [
    { to: '/school/teacher/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/teacher/classes', label: 'My Classes', icon: Users },
    { to: '/school/teacher/subjects', label: 'My Subjects', icon: BookOpen },
    { to: '/school/teacher/students', label: 'My Students', icon: GraduationCap },
    { to: '/school/teacher/assignments', label: 'Teaching Assignments', icon: NotebookPen },
    { to: '/school/teacher/timetable', label: 'Timetable', icon: Calendar },
    { to: '/school/teacher/attendance', label: 'Attendance', icon: ClipboardCheck },
    { to: '/school/teacher/examinations', label: 'Examinations', icon: GraduationCap },
    { to: '/school/teacher/results', label: 'Results', icon: FileText },
    { to: '/school/teacher/notifications', label: 'Notifications', icon: Bell },
    { to: '/school/teacher/announcements', label: 'Announcements', icon: Megaphone },
    { to: '/school/teacher/profile', label: 'My Profile', icon: User },
  ],
  STUDENT: [
    { to: '/school/student/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/student/certificates', label: 'Certificates', icon: Award },
    { to: '/school/student/change-password', label: 'Change Password', icon: KeyRound },
  ],
  PARENT: [{ to: '/school/parent/dashboard', label: 'Dashboard', icon: LayoutDashboard }],
  ACCOUNTANT: [
    { to: '/school/accountant/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/accountant/payments', label: 'Payments', icon: CreditCard },
    { to: '/school/accountant/fee-assignments', label: 'Fee Assignments', icon: Receipt },
    { to: '/school/accountant/fee-structures', label: 'Fee Structures', icon: FileText },
    { to: '/school/accountant/finance', label: 'Finance', icon: Wallet },
  ],
  LIBRARIAN: [
    { to: '/school/librarian/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/librarian/library', label: 'Library', icon: BookOpenCheck },
  ],
  HOD: [
    { to: '/school/hod/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/hod/departments', label: 'Departments', icon: Users },
    { to: '/school/hod/subjects', label: 'Subjects', icon: FileText },
    { to: '/school/hod/examinations', label: 'Examinations', icon: GraduationCap },
    { to: '/school/hod/results', label: 'Results', icon: FileText },
  ],
  WARDEN: [
    { to: '/school/warden/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/warden/boarding', label: 'Boarding', icon: Building2 },
  ],
  TRANSPORT_OFFICER: [
    { to: '/school/transport-officer/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/school/transport-officer/transport', label: 'Transport', icon: Bus },
  ],
};

export function staffConfig(role: DashboardRole): ShellConfig {
  const roleLabel = getRoleDisplayName(role);
  return {
    items: navByRole[role] ?? [],
    brandName: 'EduManage',
    headerTitle: `${roleLabel} Workspace`,
    userRole: roleLabel,
  };
}

export type { DashboardRole };
