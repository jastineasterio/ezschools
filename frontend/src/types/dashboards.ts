import type { Notification } from './notifications';
import type { Payment } from './fees';
import type { Borrowing } from './library';
import type {
  BoardingAttendanceResponse,
  BoardingStudentResponse,
} from './boarding';
import type {
  RouteResponse,
  TransportAssignmentResponse,
  VehicleResponse,
} from './transport';

export interface ParentAttendanceSummary {
  total: number;
  present: number;
  absent: number;
  late: number;
  excused: number;
  attendanceRate: number | null;
}

export interface ParentAttendanceRecord {
  attendanceDate: string;
  status: string;
  className: string | null;
  subjectName: string | null;
  remarks: string | null;
}

export interface ParentChildResult {
  id: number;
  examinationName: string | null;
  subjectName: string | null;
  marksObtained: number | null;
  percentage: number | null;
  grade: string | null;
  status: string | null;
  remarks: string | null;
}

export interface ParentChildFeeItem {
  id: number;
  feeStructureName: string | null;
  amountDue: number;
  amountPaid: number;
  balance: number;
  dueDate: string | null;
  status: string | null;
}

export interface ParentChildPayment {
  id: number;
  amount: number;
  paymentMethod: string;
  paymentDate: string;
  receiptNumber: string | null;
  referenceNumber: string | null;
  status: string;
}

export interface ParentChild {
  studentId: number;
  studentName: string;
  admissionNumber: string;
  className: string | null;
  streamName: string | null;
  academicYearName: string | null;
  enrollmentStatus: string | null;
  studentType: string | null;
  status: string | null;
  attendance: ParentAttendanceSummary | null;
  recentAttendance: ParentAttendanceRecord[];
  recentResults: ParentChildResult[];
  totalFeesDue: number;
  totalFeesPaid: number;
  balance: number;
  feeItems: ParentChildFeeItem[];
  recentPayments: ParentChildPayment[];
}

export interface ParentDashboard {
  parentId: number | null;
  parentName: string;
  email: string | null;
  phone: string | null;
  schoolId: number;
  schoolName: string | null;
  academicYearId: number | null;
  academicYearName: string | null;
  profileLinked: boolean;
  childCount: number;
  totalOutstandingBalance: number;
  unreadNotificationCount: number;
  children: ParentChild[];
  notifications: Notification[];
}

export interface AccountantPaymentSummary {
  label: string;
  total: number;
  amount: number;
}

export interface AccountantFeeStructureSummary {
  feeStructureId: number;
  name: string;
  feeType: string | null;
  amount: number;
  status: string | null;
  className: string | null;
  academicYearName: string | null;
  assignments: number;
  billed: number;
  collected: number;
  outstanding: number;
}

export interface AccountantDashboard {
  schoolId: number;
  schoolName: string | null;
  academicYearId: number | null;
  academicYearName: string | null;
  periodStart: string | null;
  periodEnd: string | null;
  totalFeesBilled: number;
  totalFeesCollected: number;
  totalOutstanding: number;
  paymentCount: number;
  studentsWithOutstandingBalance: number;
  pendingVerificationCount: number;
  pendingVerificationAmount: number;
  paymentsByDate: AccountantPaymentSummary[];
  paymentsByMethod: AccountantPaymentSummary[];
  recentPayments: Payment[];
  pendingPayments: Payment[];
  feeStructures: AccountantFeeStructureSummary[];
}

export interface LibraryActivity {
  borrowingId: number;
  type: string;
  bookTitle: string | null;
  barcode: string | null;
  memberName: string | null;
  membershipNumber: string | null;
  borrowDate: string | null;
  dueDate: string | null;
  returnDate: string | null;
  status: string | null;
}

export interface LibrarianDashboard {
  schoolId: number;
  schoolName: string | null;
  totalBooks: number;
  totalCopies: number;
  availableCopies: number;
  borrowedCopies: number;
  unavailableCopies: number;
  activeLoans: number;
  overdueLoans: number;
  returnedThisMonth: number;
  libraryMembers: number;
  pendingFines: number;
  pendingFineAmount: number;
  recentBorrowings: Borrowing[];
  overdueBorrowings: Borrowing[];
  recentActivity: LibraryActivity[];
}

export interface HodDepartmentSummary {
  id: number;
  name: string;
  code: string | null;
  status: string | null;
  subjects: number;
  teachers: number;
  students: number;
}

export interface HodExaminationSummary {
  id: number;
  name: string;
  status: string | null;
  startDate: string | null;
  endDate: string | null;
}

export interface HodDashboard {
  schoolId: number;
  schoolName: string | null;
  academicYearId: number | null;
  academicYearName: string | null;
  totalDepartments: number;
  headedDepartments: number;
  subjects: number;
  teachers: number;
  students: number;
  pendingResultApprovals: number;
  publishedResults: number;
  departments: HodDepartmentSummary[];
  upcomingExaminations: HodExaminationSummary[];
}

export interface WardenDashboard {
  schoolId: number;
  schoolName: string | null;
  totalDormitories: number;
  assignedDormitories: number;
  totalRooms: number;
  totalBeds: number;
  occupiedBeds: number;
  boardingStudents: number;
  attendanceDate: string | null;
  presentToday: number;
  absentToday: number;
  students: BoardingStudentResponse[];
  recentAttendance: BoardingAttendanceResponse[];
}

export interface TransportOfficerDashboard {
  schoolId: number;
  schoolName: string | null;
  totalVehicles: number;
  activeVehicles: number;
  totalRoutes: number;
  activeRoutes: number;
  activeAssignments: number;
  studentsOnTransport: number;
  vehiclesDueForService: number;
  vehiclesWithExpiringInsurance: number;
  vehicles: VehicleResponse[];
  routes: RouteResponse[];
  assignments: TransportAssignmentResponse[];
  serviceHorizon: string | null;
}
