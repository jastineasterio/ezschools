import { useQuery } from '@tanstack/react-query';
import {
  BedDouble,
  Building2,
  CalendarCheck,
  UserCheck,
  UserMinus,
  Users,
} from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { getWardenDashboard } from '@/services/dashboards';
import { getApiErrorMessage } from '@/services/api';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatDate, formatNumber } from '@/lib/format';

export function WardenDashboardPage() {
  const { user } = useAuth();
  const displayName =
    user && (user.firstName || user.lastName)
      ? `${user.firstName} ${user.lastName}`.trim()
      : 'Warden';

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard', 'warden'],
    queryFn: getWardenDashboard,
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
        <Skeleton className="h-72 w-full" />
      </div>
    );
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Unable to load the warden dashboard</AlertTitle>
        <AlertDescription>
          {getApiErrorMessage(error, 'The boarding summary could not be loaded.')}{' '}
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
        description="The warden dashboard returned no information for this school."
      />
    );
  }

  const occupancy =
    data.totalBeds > 0 ? Math.round((data.occupiedBeds / data.totalBeds) * 100) : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground sm:text-3xl">
          Welcome, {displayName}
        </h1>
        <p className="mt-1 text-muted-foreground">
          {data.schoolName ? `${data.schoolName} · ` : ''}Boarding warden dashboard
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Boarding students"
          value={formatNumber(data.boardingStudents)}
          icon={Users}
        />
        <StatCard
          label="Dormitories"
          value={formatNumber(data.totalDormitories)}
          icon={Building2}
          hint={`${formatNumber(data.assignedDormitories)} assigned to you`}
        />
        <StatCard
          label="Bed occupancy"
          value={`${occupancy}%`}
          icon={BedDouble}
          hint={`${formatNumber(data.occupiedBeds)} of ${formatNumber(data.totalBeds)} beds`}
        />
        <StatCard
          label="Present today"
          value={formatNumber(data.presentToday)}
          tone="success"
          icon={UserCheck}
          hint={`${formatNumber(data.absentToday)} absent`}
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Attendance date"
          value={formatDate(data.attendanceDate)}
          icon={CalendarCheck}
        />
        <StatCard
          label="Absent today"
          value={formatNumber(data.absentToday)}
          tone={data.absentToday > 0 ? 'warning' : 'default'}
          icon={UserMinus}
        />
      </div>

      <SectionCard
        title="Boarding students"
        description="Students currently allocated to a bed"
      >
        {data.students.length === 0 ? (
          <EmptyState
            title="No boarding students"
            description="Students allocated to dormitories will be listed here."
          />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Student</TableHead>
                  <TableHead>Dormitory</TableHead>
                  <TableHead>Room</TableHead>
                  <TableHead>Bed</TableHead>
                  <TableHead>Allocated</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.students.map((student) => (
                  <TableRow key={student.id}>
                    <TableCell>
                      {student.studentName ?? '—'}
                      {student.admissionNumber ? (
                        <span className="block text-xs text-muted-foreground">
                          {student.admissionNumber}
                        </span>
                      ) : null}
                    </TableCell>
                    <TableCell>{student.dormitoryName ?? '—'}</TableCell>
                    <TableCell>{student.roomNumber ?? '—'}</TableCell>
                    <TableCell>{student.bedNumber ?? '—'}</TableCell>
                    <TableCell className="whitespace-nowrap">
                      {formatDate(student.allocatedDate)}
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{student.status ?? '—'}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </SectionCard>

      <SectionCard
        title="Recent boarding attendance"
        description="Latest attendance marks recorded for the dormitories"
      >
        {data.recentAttendance.length === 0 ? (
          <EmptyState
            title="No attendance recorded"
            description="Boarding attendance will appear here once it is marked."
          />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Student</TableHead>
                  <TableHead>Dormitory</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Remarks</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.recentAttendance.map((attendance) => (
                  <TableRow key={attendance.id}>
                    <TableCell className="whitespace-nowrap">
                      {formatDate(attendance.attendanceDate)}
                    </TableCell>
                    <TableCell>{attendance.studentName ?? '—'}</TableCell>
                    <TableCell>{attendance.dormitoryName ?? '—'}</TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          attendance.status === 'PRESENT'
                            ? 'success'
                            : attendance.status === 'ABSENT'
                              ? 'destructive'
                              : 'secondary'
                        }
                      >
                        {attendance.status}
                      </Badge>
                    </TableCell>
                    <TableCell>{attendance.remarks ?? '—'}</TableCell>
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
