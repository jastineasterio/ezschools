import { useQuery } from '@tanstack/react-query';
import {
  Bell,
  CalendarDays,
  ClipboardCheck,
  Receipt,
  Users,
  Wallet,
} from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { getParentDashboard } from '@/services/dashboards';
import { getApiErrorMessage } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatAmount, formatDate, formatPercent } from '@/lib/format';
import type { ParentChild } from '@/types/dashboards';

const attendanceTone: Record<string, string> = {
  PRESENT: 'bg-emerald-500',
  LATE: 'bg-amber-500',
  ABSENT: 'bg-destructive',
  EXCUSED: 'bg-sky-500',
};

function attendanceBadge(status: string) {
  const normalized = (status || '').toUpperCase();
  if (normalized === 'PRESENT') return 'success' as const;
  if (normalized === 'ABSENT') return 'destructive' as const;
  if (normalized === 'LATE') return 'warning' as const;
  return 'secondary' as const;
}

export function ParentDashboardPage() {
  const { user } = useAuth();
  const displayName =
    user && (user.firstName || user.lastName)
      ? `${user.firstName} ${user.lastName}`.trim()
      : 'Parent';

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard', 'parent'],
    queryFn: getParentDashboard,
  });

  if (isLoading) {
    return (
      <div className="space-y-6" aria-busy="true" aria-live="polite">
        <Skeleton className="h-10 w-64" />
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </div>
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Unable to load the parent dashboard</AlertTitle>
        <AlertDescription>
          {getApiErrorMessage(error, 'The dashboard data could not be loaded.')}{' '}
          <button className="underline" onClick={() => void refetch()}>
            Try again
          </button>
        </AlertDescription>
      </Alert>
    );
  }

  if (!data) {
    return (
      <EmptyState
        title="No dashboard data"
        description="The parent dashboard returned no information for this account."
      />
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground sm:text-3xl">
          Welcome, {displayName}
        </h1>
        <p className="mt-1 text-muted-foreground">
          {data.schoolName ? `${data.schoolName} · ` : ''}Parent dashboard
          {data.academicYearName ? ` · ${data.academicYearName}` : ''}
        </p>
      </div>

      {!data.profileLinked ? (
        <Alert>
          <AlertTitle>Parent profile not linked</AlertTitle>
          <AlertDescription>
            This account is not linked to a parent record yet, so no children can
            be displayed. Please ask your school administrator to link your
            account.
          </AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Children" value={data.childCount} icon={Users} />
        <StatCard
          label="Outstanding balance"
          value={formatAmount(data.totalOutstandingBalance)}
          tone={data.totalOutstandingBalance > 0 ? 'warning' : 'success'}
          icon={Wallet}
        />
        <StatCard
          label="Unread notifications"
          value={data.unreadNotificationCount}
          icon={Bell}
        />
        <StatCard
          label="Academic year"
          value={data.academicYearName ?? '—'}
          icon={CalendarDays}
        />
      </div>

      {data.children.length === 0 ? (
        <Card className="p-6">
          <EmptyState
            title="No children linked"
            description="Once your account is linked to your children, their attendance, results and fee information will appear here."
          />
        </Card>
      ) : (
        <div className="space-y-6">
          {data.children.map((child) => (
            <ChildCard key={child.studentId} child={child} />
          ))}
        </div>
      )}

      <SectionCard
        title="Notifications"
        description="Recent updates sent to your account"
      >
        {data.notifications.length === 0 ? (
          <EmptyState title="No notifications" description="You have no recent notifications." />
        ) : (
          <ul className="space-y-3">
            {data.notifications.map((notification) => (
              <li
                key={notification.id}
                className="flex flex-col gap-1 rounded-lg border border-border p-3 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <p className="text-sm font-medium text-foreground">
                    {notification.title}
                    {!notification.isRead ? (
                      <Badge variant="default" className="ml-2">
                        New
                      </Badge>
                    ) : null}
                  </p>
                  <p className="mt-0.5 text-sm text-muted-foreground">
                    {notification.message}
                  </p>
                </div>
                <span className="shrink-0 text-xs text-muted-foreground">
                  {formatDate(notification.createdAt)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </SectionCard>
    </div>
  );
}

function ChildCard({ child }: { child: ParentChild }) {
  const attendance = child.attendance;
  const rate = attendance?.attendanceRate ?? null;

  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-foreground">
            {child.studentName}
          </h2>
          <p className="text-sm text-muted-foreground">
            {child.admissionNumber}
            {child.className ? ` · ${child.className}` : ''}
            {child.streamName ? ` · ${child.streamName}` : ''}
            {child.academicYearName ? ` · ${child.academicYearName}` : ''}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {child.enrollmentStatus ? (
            <Badge variant="secondary">{child.enrollmentStatus}</Badge>
          ) : null}
          {child.status ? <Badge variant="outline">{child.status}</Badge> : null}
        </div>
      </div>

      <div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Attendance rate"
          value={formatPercent(rate)}
          tone={
            rate === null
              ? 'default'
              : rate >= 90
                ? 'success'
                : rate >= 75
                  ? 'warning'
                  : 'destructive'
          }
          icon={ClipboardCheck}
        />
        <StatCard
          label="Fees due"
          value={formatAmount(child.totalFeesDue)}
          icon={Receipt}
        />
        <StatCard
          label="Fees paid"
          value={formatAmount(child.totalFeesPaid)}
          tone="success"
          icon={Wallet}
        />
        <StatCard
          label="Balance"
          value={formatAmount(child.balance)}
          tone={child.balance > 0 ? 'warning' : 'success'}
          icon={Receipt}
        />
      </div>

      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <SectionCard title="Recent attendance">
          {child.recentAttendance.length === 0 ? (
            <EmptyState
              title="No attendance recorded"
              description="Attendance records will appear here once they are marked."
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Class</TableHead>
                    <TableHead>Subject</TableHead>
                    <TableHead>Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {child.recentAttendance.map((record, index) => (
                    <TableRow key={`${record.attendanceDate}-${index}`}>
                      <TableCell className="whitespace-nowrap">
                        {formatDate(record.attendanceDate)}
                      </TableCell>
                      <TableCell>{record.className ?? '—'}</TableCell>
                      <TableCell>{record.subjectName ?? '—'}</TableCell>
                      <TableCell>
                        <Badge variant={attendanceBadge(record.status)}>
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

        <SectionCard title="Recent results">
          {child.recentResults.length === 0 ? (
            <EmptyState
              title="No released results"
              description="Results appear here once the school publishes them."
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Examination</TableHead>
                    <TableHead>Subject</TableHead>
                    <TableHead>Marks</TableHead>
                    <TableHead>Grade</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {child.recentResults.map((result) => (
                    <TableRow key={result.id}>
                      <TableCell>{result.examinationName ?? '—'}</TableCell>
                      <TableCell>{result.subjectName ?? '—'}</TableCell>
                      <TableCell className="tabular-nums">
                        {result.marksObtained ?? '—'}
                        {result.percentage !== null
                          ? ` (${formatPercent(result.percentage)})`
                          : ''}
                      </TableCell>
                      <TableCell>
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

      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <SectionCard title="Fee breakdown">
          {child.feeItems.length === 0 ? (
            <EmptyState
              title="No fee assignments"
              description="No fees have been assigned to this student yet."
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fee</TableHead>
                    <TableHead>Due</TableHead>
                    <TableHead>Paid</TableHead>
                    <TableHead>Balance</TableHead>
                    <TableHead>Due date</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {child.feeItems.map((fee) => (
                    <TableRow key={fee.id}>
                      <TableCell>{fee.feeStructureName ?? '—'}</TableCell>
                      <TableCell className="tabular-nums">
                        {formatAmount(fee.amountDue)}
                      </TableCell>
                      <TableCell className="tabular-nums">
                        {formatAmount(fee.amountPaid)}
                      </TableCell>
                      <TableCell className="tabular-nums">
                        {formatAmount(fee.balance)}
                      </TableCell>
                      <TableCell className="whitespace-nowrap">
                        {formatDate(fee.dueDate)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>

        <SectionCard title="Payment history">
          {child.recentPayments.length === 0 ? (
            <EmptyState
              title="No payments recorded"
              description="Payments recorded by the school will appear here."
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Amount</TableHead>
                    <TableHead>Method</TableHead>
                    <TableHead>Receipt</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {child.recentPayments.map((payment) => (
                    <TableRow key={payment.id}>
                      <TableCell className="whitespace-nowrap">
                        {formatDate(payment.paymentDate)}
                      </TableCell>
                      <TableCell className="tabular-nums">
                        {formatAmount(payment.amount)}
                      </TableCell>
                      <TableCell>{payment.paymentMethod}</TableCell>
                      <TableCell>{payment.receiptNumber ?? '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>
      </div>

      {attendance && attendance.total > 0 ? (
        <div className="mt-4">
          <SectionCard
            title="Attendance breakdown"
            description={`${attendance.present} present · ${attendance.absent} absent · ${attendance.late} late · ${attendance.excused} excused`}
          >
            <div
              className="flex h-3 w-full overflow-hidden rounded-full bg-muted"
              role="img"
              aria-label={`Attendance rate ${formatPercent(rate)}`}
            >
              {(
                [
                  ['present', attendance.present],
                  ['late', attendance.late],
                  ['excused', attendance.excused],
                  ['absent', attendance.absent],
                ] as const
              ).map(([key, value]) =>
                value > 0 ? (
                  <div
                    key={key}
                    className={attendanceTone[key.toUpperCase()]}
                    style={{ width: `${(value / attendance.total) * 100}%` }}
                  />
                ) : null
              )}
            </div>
          </SectionCard>
        </div>
      ) : null}
    </Card>
  );
}
