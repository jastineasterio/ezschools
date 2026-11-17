import { useQuery } from '@tanstack/react-query';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import {
  useAuth } from '@/store/AuthContext';
import {
  getStudentSelfProfile,
  getStudentSelfAttendance,
  getStudentSelfResults,
  getStudentSelfFeeAssignments,
  getStudentSelfPayments,
  getStudentSelfTimetable,
} from '@/services/dashboards';
import { getApiErrorMessage } from '@/services/api';
import { formatAmount, formatDate } from '@/lib/format';
import { ClipboardCheck, Wallet, Clock } from 'lucide-react';

export function StudentDashboardPage() {
  const { user } = useAuth();
  const name = user ? `${user.firstName} ${user.lastName}` : 'Student';

  const profileQuery = useQuery({
    queryKey: ['student-profile'],
    queryFn: getStudentSelfProfile,
  });
  const attendanceQuery = useQuery({
    queryKey: ['student-attendance'],
    queryFn: getStudentSelfAttendance,
  });
  const resultsQuery = useQuery({
    queryKey: ['student-results'],
    queryFn: () => getStudentSelfResults(0, 10),
  });
  const feeAssignmentsQuery = useQuery({
    queryKey: ['student-fee-assignments'],
    queryFn: getStudentSelfFeeAssignments,
  });
  const paymentsQuery = useQuery({
    queryKey: ['student-payments'],
    queryFn: getStudentSelfPayments,
  });
  const timetableQuery = useQuery({
    queryKey: ['student-timetable'],
    queryFn: getStudentSelfTimetable,
  });

  const profile = profileQuery.data;
  const attendanceRecords = attendanceQuery.data ?? [];
  const results = resultsQuery.data?.content ?? [];
  const feeAssignments = feeAssignmentsQuery.data ?? [];
  const payments = paymentsQuery.data ?? [];
  const timetable = timetableQuery.data ?? [];

  const totalAttendance = attendanceRecords.length;
  const presentCount = attendanceRecords.filter(a => a.status?.toUpperCase() === 'PRESENT').length;
  const attendanceRate = totalAttendance > 0 ? Math.round((presentCount / totalAttendance) * 100) : null;

  const totalFeesDue = feeAssignments.reduce((sum, f) => sum + f.amountDue, 0);
  const totalFeesPaid = feeAssignments.reduce((sum, f) => sum + f.amountPaid, 0);
  const totalBalance = totalFeesDue - totalFeesPaid;

  if (profileQuery.isLoading || attendanceQuery.isLoading || resultsQuery.isLoading || feeAssignmentsQuery.isLoading || paymentsQuery.isLoading || timetableQuery.isLoading) {
    return (
      <div className="page space-y-6">
        <div className="page-header">
          <div>
            <h1 className="page-title">Welcome, {name}</h1>
            <p className="page-subtitle">Your academic overview at a glance</p>
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i} className="p-5">
              <Skeleton className="h-8 w-32 mb-3" />
              <Skeleton className="h-6 w-full" />
            </Card>
          ))}
        </div>
        <Card className="p-6"><Skeleton className="h-48" /></Card>
        <Card className="p-6"><Skeleton className="h-48" /></Card>
        <Card className="p-6"><Skeleton className="h-48" /></Card>
      </div>
    );
  }

  if (profileQuery.isError) {
    return (
      <div className="page space-y-6">
        <div className="page-header">
          <div>
            <h1 className="page-title">Welcome, {name}</h1>
            <p className="page-subtitle">Your academic overview at a glance</p>
          </div>
        </div>
        <Alert variant="destructive">
          <AlertTitle>Unable to load student dashboard</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(profileQuery.error, 'Your dashboard data could not be loaded.')}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  return (
    <div className="page space-y-6">
      <div className="page-header">
        <div className="flex items-center gap-4">
          <div
            className="flex h-10 w-10 items-center justify-center rounded-xl text-sm font-bold text-white shadow-sm"
            style={{ backgroundColor: '#3b82f6' }}
          >
            {user ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase() || 'S' : 'S'}
          </div>
          <div>
            <h1 className="page-title">Welcome, {name}</h1>
            <p className="page-subtitle">
              {profile?.schoolName ? `${profile.schoolName} · ` : ''}Student Dashboard
              {profile?.academicYearName ? ` · ${profile.academicYearName}` : ''}
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Attendance Rate"
          value={attendanceRate === null ? '—' : `${attendanceRate}%`}
          tone={
            attendanceRate === null
              ? 'default'
              : attendanceRate >= 90
                ? 'success'
                : attendanceRate >= 75
                  ? 'warning'
                  : 'destructive'
          }
          icon={ClipboardCheck}
          hint={`${presentCount} of ${totalAttendance} days present`}
        />
        <StatCard
          label="Fees Due"
          value={formatAmount(totalFeesDue)}
          icon={Wallet}
          hint="Total amount across all fee assignments"
        />
        <StatCard
          label="Fees Paid"
          value={formatAmount(totalFeesPaid)}
          tone="success"
          icon={Wallet}
          hint="Amount paid to date"
        />
        <StatCard
          label="Outstanding Balance"
          value={formatAmount(totalBalance)}
          tone={totalBalance > 0 ? 'warning' : 'success'}
          icon={Clock}
          hint={totalBalance > 0 ? 'Balance to clear' : 'All fees cleared'}
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <SectionCard
          title="Fee Status"
          description="Your current fee assignments and payment status"
        >
          {feeAssignments.length === 0 ? (
            <EmptyState title="No fee assignments" description="Fee assignments will appear here once they are created by the school." />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fee</TableHead>
                    <TableHead className="text-right">Due</TableHead>
                    <TableHead className="text-right">Paid</TableHead>
                    <TableHead className="text-right">Balance</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {feeAssignments.map((fee) => (
                    <TableRow key={fee.id}>
                      <TableCell className="font-medium whitespace-nowrap">{fee.feeStructureName}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatAmount(fee.amountDue)}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatAmount(fee.amountPaid)}</TableCell>
                      <TableCell className="text-right tabular-nums font-medium">{formatAmount(fee.balance)}</TableCell>
                      <TableCell className="text-center">
                        <Badge
                          variant={
                            fee.status?.toUpperCase() === 'PAID' ? 'success' : fee.status?.toUpperCase() === 'PARTIAL' ? 'warning' : 'destructive'
                          }
                        >
                          {fee.status}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>

        <SectionCard
          title="Recent Examination Results"
          description="Your latest published results"
        >
          {results.length === 0 ? (
            <EmptyState title="No results" description="Results will appear here once they are published by the school." />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Examination</TableHead>
                    <TableHead>Subject</TableHead>
                    <TableHead className="text-right">Marks</TableHead>
                    <TableHead className="text-right">Max</TableHead>
                    <TableHead className="text-center">Grade</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {results.map((result) => (
                    <TableRow key={result.id}>
                      <TableCell>{result.examinationName}</TableCell>
                      <TableCell>{result.subjectName}</TableCell>
                      <TableCell className="text-right tabular-nums">{result.marksObtained ?? '—'}</TableCell>
                      <TableCell className="text-right tabular-nums">{result.maximumMarks ?? '—'}</TableCell>
                      <TableCell className="text-center">
                        <Badge variant="secondary">{result.grade ?? '—'}</Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <SectionCard
          title="Recent Attendance"
          description="Your latest attendance records"
        >
          {attendanceRecords.length === 0 ? (
            <EmptyState title="No attendance" description="Attendance records will appear here once they are marked." />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Class</TableHead>
                    <TableHead>Subject</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {attendanceRecords.slice(0, 10).map((record) => (
                    <TableRow key={record.id}>
                      <TableCell className="whitespace-nowrap">{formatDate(record.date)}</TableCell>
                      <TableCell>{record.className ?? '—'}</TableCell>
                      <TableCell>{record.subjectName ?? '—'}</TableCell>
                      <TableCell className="text-center">
                        <Badge
                          variant={
                            record.status?.toUpperCase() === 'PRESENT' ? 'success' :
                            record.status?.toUpperCase() === 'ABSENT' ? 'destructive' :
                            record.status?.toUpperCase() === 'LATE' ? 'warning' : 'secondary'
                          }
                        >
                          {record.status}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>

        <SectionCard
          title="Timetable"
          description="Your class schedule"
        >
          {timetable.length === 0 ? (
            <EmptyState title="No timetable" description="Timetable entries will appear here once they are scheduled." />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Day</TableHead>
                    <TableHead>Subject</TableHead>
                    <TableHead>Class</TableHead>
                    <TableHead className="text-right">Time</TableHead>
                    <TableHead>Room</TableHead>
                    <TableHead>Teacher</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {timetable.map((lesson) => (
                    <TableRow key={lesson.id}>
                      <TableCell>{lesson.dayOfWeek}</TableCell>
                      <TableCell className="font-medium">{lesson.subjectName}</TableCell>
                      <TableCell>{lesson.className}{lesson.streamName ? ` · ${lesson.streamName}` : ''}</TableCell>
                      <TableCell className="text-right tabular-nums">{lesson.startTime} – {lesson.endTime}</TableCell>
                      <TableCell>{lesson.room ?? '—'}</TableCell>
                      <TableCell>{lesson.teacherName ?? '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>
      </div>

      <SectionCard
        title="Recent Payments"
        description="Payments recorded for your fee assignments"
      >
        {payments.length === 0 ? (
          <EmptyState title="No payments" description="Payments will appear here once they are recorded." />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead>Method</TableHead>
                  <TableHead>Receipt</TableHead>
                  <TableHead className="text-center">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {payments.map((payment) => (
                  <TableRow key={payment.id}>
                    <TableCell className="whitespace-nowrap">{formatDate(payment.paymentDate)}</TableCell>
                    <TableCell className="text-right tabular-nums">{formatAmount(payment.amount)}</TableCell>
                    <TableCell>{payment.paymentMethod}</TableCell>
                    <TableCell>{payment.receiptNumber ?? '—'}</TableCell>
                    <TableCell className="text-center">
                      <Badge variant={payment.status === 'COMPLETED' ? 'success' : 'warning'}>
                        {payment.status}
                      </Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </SectionCard>
    </div>
  );
}
