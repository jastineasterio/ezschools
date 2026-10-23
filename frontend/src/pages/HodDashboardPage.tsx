import { useQuery } from '@tanstack/react-query';
import {
  BookOpen,
  Building,
  CalendarDays,
  FileCheck2,
  GraduationCap,
  Users,
  UserCheck,
} from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { getHodDashboard } from '@/services/dashboards';
import { getApiErrorMessage } from '@/services/api';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatDate, formatNumber } from '@/lib/format';

export function HodDashboardPage() {
  const { user } = useAuth();
  const displayName =
    user && (user.firstName || user.lastName)
      ? `${user.firstName} ${user.lastName}`.trim()
      : 'Head of Department';

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard', 'hod'],
    queryFn: getHodDashboard,
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
        <AlertTitle>Unable to load the department dashboard</AlertTitle>
        <AlertDescription>
          {getApiErrorMessage(error, 'The department overview could not be loaded.')}{' '}
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
        description="The department dashboard returned no information for this school."
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
          {data.schoolName ? `${data.schoolName} · ` : ''}
          Head of Department dashboard
          {data.academicYearName ? ` · ${data.academicYearName}` : ''}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Departments headed"
          value={formatNumber(data.headedDepartments)}
          icon={Building}
          hint={`${formatNumber(data.totalDepartments)} in school`}
        />
        <StatCard
          label="Subjects"
          value={formatNumber(data.subjects)}
          icon={BookOpen}
        />
        <StatCard
          label="Teachers"
          value={formatNumber(data.teachers)}
          icon={UserCheck}
        />
        <StatCard
          label="Students"
          value={formatNumber(data.students)}
          icon={Users}
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Results awaiting approval"
          value={formatNumber(data.pendingResultApprovals)}
          tone={data.pendingResultApprovals > 0 ? 'warning' : 'success'}
          icon={FileCheck2}
        />
        <StatCard
          label="Published results"
          value={formatNumber(data.publishedResults)}
          tone="success"
          icon={GraduationCap}
        />
      </div>

      <SectionCard
        title="Department overview"
        description="Subjects, teachers and students per department"
      >
        {data.departments.length === 0 ? (
          <EmptyState
            title="No departments"
            description="Departments configured for this school will appear here."
          />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Department</TableHead>
                  <TableHead>Code</TableHead>
                  <TableHead className="text-right">Subjects</TableHead>
                  <TableHead className="text-right">Teachers</TableHead>
                  <TableHead className="text-right">Students</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.departments.map((department) => (
                  <TableRow key={department.id}>
                    <TableCell className="font-medium">{department.name}</TableCell>
                    <TableCell>{department.code ?? '—'}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatNumber(department.subjects)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatNumber(department.teachers)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatNumber(department.students)}
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{department.status ?? '—'}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </SectionCard>

      <SectionCard
        title="Upcoming examinations"
        description="Scheduled examinations that have not started yet"
      >
        {data.upcomingExaminations.length === 0 ? (
          <EmptyState
            title="No upcoming examinations"
            description="Scheduled examinations will be listed here once they are created."
          />
        ) : (
          <ul className="space-y-2">
            {data.upcomingExaminations.map((examination) => (
              <li
                key={examination.id}
                className="flex items-center justify-between gap-3 rounded-lg border border-border p-3"
              >
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {examination.name}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatDate(examination.startDate)} → {formatDate(examination.endDate)}
                  </p>
                </div>
                <Badge variant="secondary">{examination.status ?? '—'}</Badge>
              </li>
            ))}
          </ul>
        )}
      </SectionCard>

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Academic year"
          value={data.academicYearName ?? '—'}
          icon={CalendarDays}
        />
        <StatCard
          label="Departments in school"
          value={formatNumber(data.totalDepartments)}
          icon={Building}
        />
      </div>
    </div>
  );
}
