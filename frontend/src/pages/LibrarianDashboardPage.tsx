import { useQuery } from '@tanstack/react-query';
import {
  BookOpen,
  BookMarked,
  CalendarCheck,
  CircleAlert,
  HandCoins,
  Library,
  Users,
} from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { getLibrarianDashboard } from '@/services/dashboards';
import { getApiErrorMessage } from '@/services/api';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatAmount, formatDate, formatNumber } from '@/lib/format';

export function LibrarianDashboardPage() {
  const { user } = useAuth();
  const displayName =
    user && (user.firstName || user.lastName)
      ? `${user.firstName} ${user.lastName}`.trim()
      : 'Librarian';

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard', 'librarian'],
    queryFn: getLibrarianDashboard,
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
        <AlertTitle>Unable to load the librarian dashboard</AlertTitle>
        <AlertDescription>
          {getApiErrorMessage(error, 'The library summary could not be loaded.')}{' '}
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
        description="The librarian dashboard returned no information for this school."
      />
    );
  }

  const copyUtilisation =
    data.totalCopies > 0
      ? Math.round((data.borrowedCopies / data.totalCopies) * 100)
      : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground sm:text-3xl">
          Welcome, {displayName}
        </h1>
        <p className="mt-1 text-muted-foreground">
          {data.schoolName ? `${data.schoolName} · ` : ''}Librarian dashboard
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Books in catalogue"
          value={formatNumber(data.totalBooks)}
          icon={BookOpen}
        />
        <StatCard
          label="Total copies"
          value={formatNumber(data.totalCopies)}
          icon={Library}
          hint={`${formatNumber(data.availableCopies)} available · ${formatNumber(
            data.borrowedCopies
          )} on loan`}
        />
        <StatCard
          label="Active loans"
          value={formatNumber(data.activeLoans)}
          icon={BookMarked}
          hint={`${copyUtilisation}% of stock on loan`}
        />
        <StatCard
          label="Overdue"
          value={formatNumber(data.overdueLoans)}
          tone={data.overdueLoans > 0 ? 'destructive' : 'success'}
          icon={CircleAlert}
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Returned this month"
          value={formatNumber(data.returnedThisMonth)}
          tone="success"
          icon={CalendarCheck}
        />
        <StatCard
          label="Library members"
          value={formatNumber(data.libraryMembers)}
          icon={Users}
        />
        <StatCard
          label="Pending fines"
          value={formatNumber(data.pendingFines)}
          tone={data.pendingFines > 0 ? 'warning' : 'default'}
          icon={HandCoins}
        />
        <StatCard
          label="Uncollected fines"
          value={formatAmount(data.pendingFineAmount)}
          tone={data.pendingFineAmount > 0 ? 'warning' : 'default'}
          icon={HandCoins}
        />
      </div>

      <SectionCard
        title="Overdue books"
        description="Loans that passed their expected return date"
      >
        {data.overdueBorrowings.length === 0 ? (
          <EmptyState
            title="No overdue books"
            description="Every loan is either returned or still within its return date."
          />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Book</TableHead>
                  <TableHead>Barcode</TableHead>
                  <TableHead>Member</TableHead>
                  <TableHead>Due date</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.overdueBorrowings.map((borrowing) => (
                  <TableRow key={borrowing.id}>
                    <TableCell>{borrowing.bookTitle ?? '—'}</TableCell>
                    <TableCell>{borrowing.barcode ?? '—'}</TableCell>
                    <TableCell>
                      {borrowing.memberName ?? '—'}
                      {borrowing.membershipNumber ? (
                        <span className="block text-xs text-muted-foreground">
                          {borrowing.membershipNumber}
                        </span>
                      ) : null}
                    </TableCell>
                    <TableCell className="whitespace-nowrap">
                      {formatDate(borrowing.dueDate)}
                    </TableCell>
                    <TableCell>
                      <Badge variant="destructive">Overdue</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </SectionCard>

      <div className="grid gap-4 lg:grid-cols-2">
        <SectionCard
          title="Recent borrowing activity"
          description="Latest issues and returns recorded at the desk"
        >
          {data.recentActivity.length === 0 ? (
            <EmptyState
              title="No borrowing activity"
              description="Issues and returns will be listed here as books are borrowed."
            />
          ) : (
            <ul className="space-y-2">
              {data.recentActivity.map((activity) => (
                <li
                  key={`${activity.type}-${activity.borrowingId}`}
                  className="flex items-start justify-between gap-3 rounded-lg border border-border p-3"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-foreground">
                      {activity.bookTitle ?? `Loan #${activity.borrowingId}`}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {activity.memberName ?? 'Unknown member'} ·{' '}
                      {activity.membershipNumber ?? '—'}
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      Borrowed {formatDate(activity.borrowDate)} · due{' '}
                      {formatDate(activity.dueDate)}
                      {activity.returnDate
                        ? ` · returned ${formatDate(activity.returnDate)}`
                        : ''}
                    </p>
                  </div>
                  <Badge variant={activity.type === 'RETURN' ? 'success' : 'secondary'}>
                    {activity.type === 'RETURN' ? 'Returned' : 'Borrowed'}
                  </Badge>
                </li>
              ))}
            </ul>
          )}
        </SectionCard>

        <SectionCard
          title="Active loans"
          description="Books currently on loan to members"
        >
          {data.recentBorrowings.filter((b) => b.status === 'BORROWED').length === 0 ? (
            <EmptyState
              title="No active loans"
              description="All borrowed books have been returned."
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Book</TableHead>
                    <TableHead>Member</TableHead>
                    <TableHead>Borrowed</TableHead>
                    <TableHead>Due</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.recentBorrowings
                    .filter((b) => b.status === 'BORROWED')
                    .map((borrowing) => (
                      <TableRow key={borrowing.id}>
                        <TableCell>{borrowing.bookTitle ?? '—'}</TableCell>
                        <TableCell>{borrowing.memberName ?? '—'}</TableCell>
                        <TableCell className="whitespace-nowrap">
                          {formatDate(borrowing.borrowDate)}
                        </TableCell>
                        <TableCell className="whitespace-nowrap">
                          {formatDate(borrowing.dueDate)}
                        </TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  );
}
