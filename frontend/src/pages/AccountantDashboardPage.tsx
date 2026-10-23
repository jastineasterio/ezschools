import { useQuery } from '@tanstack/react-query';
import {
  BarChart,
  Bar,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  BadgeCheck,
  CalendarDays,
  CircleDollarSign,
  Clock,
  CreditCard,
  Receipt,
  Users,
} from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { getAccountantDashboard } from '@/services/dashboards';
import { getApiErrorMessage } from '@/services/api';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatAmount, formatDate, formatNumber } from '@/lib/format';

const METHOD_COLOURS = [
  '#3b82f6',
  '#10b981',
  '#f59e0b',
  '#8b5cf6',
  '#ef4444',
  '#06b6d4',
];

export function AccountantDashboardPage() {
  const { user } = useAuth();
  const displayName =
    user && (user.firstName || user.lastName)
      ? `${user.firstName} ${user.lastName}`.trim()
      : 'Accountant';

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard', 'accountant'],
    queryFn: getAccountantDashboard,
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
        <AlertTitle>Unable to load the accountant dashboard</AlertTitle>
        <AlertDescription>
          {getApiErrorMessage(error, 'The financial summary could not be loaded.')}{' '}
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
        description="The accountant dashboard returned no information for this school."
      />
    );
  }

  const periodLabel = data.academicYearName
    ? `${data.academicYearName}${
        data.periodStart ? ` (${formatDate(data.periodStart)} – ${formatDate(data.periodEnd)})` : ''
      }`
    : 'All recorded periods';

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground sm:text-3xl">
          Welcome, {displayName}
        </h1>
        <p className="mt-1 text-muted-foreground">
          {data.schoolName ? `${data.schoolName} · ` : ''}Accountant dashboard ·{' '}
          {periodLabel}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Fees collected"
          value={formatAmount(data.totalFeesCollected)}
          tone="success"
          icon={CircleDollarSign}
          hint={`${formatNumber(data.paymentCount)} payments recorded`}
        />
        <StatCard
          label="Fees billed"
          value={formatAmount(data.totalFeesBilled)}
          icon={Receipt}
        />
        <StatCard
          label="Outstanding"
          value={formatAmount(data.totalOutstanding)}
          tone={data.totalOutstanding > 0 ? 'warning' : 'success'}
          icon={CreditCard}
          hint={`${formatNumber(data.studentsWithOutstandingBalance)} students with a balance`}
        />
        <StatCard
          label="Pending verification"
          value={formatNumber(data.pendingVerificationCount)}
          tone={data.pendingVerificationCount > 0 ? 'warning' : 'default'}
          icon={BadgeCheck}
          hint={
            data.pendingVerificationCount > 0
              ? formatAmount(data.pendingVerificationAmount)
              : 'No payments awaiting verification'
          }
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <SectionCard
            title="Collections by date"
            description="Completed payments grouped by payment date"
          >
            {data.paymentsByDate.length === 0 ? (
              <EmptyState
                title="No payments yet"
                description="Collected amounts will be charted here once payments are recorded."
              />
            ) : (
              <div className="h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={data.paymentsByDate.slice(0, 14).reverse()}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip
                      formatter={(value: number, name: string) =>
                        name === 'amount' ? formatAmount(value) : formatNumber(value)
                      }
                    />
                    <Bar dataKey="amount" name="amount" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </SectionCard>
        </div>

        <SectionCard
          title="Collection by method"
          description="Completed payments grouped by payment method"
        >
          {data.paymentsByMethod.length === 0 ? (
            <EmptyState
              title="No payment methods"
              description="No completed payments have been recorded for this period."
            />
          ) : (
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={data.paymentsByMethod}
                    dataKey="amount"
                    nameKey="label"
                    innerRadius="55%"
                    outerRadius="80%"
                    label={(entry: { label?: string }) => entry?.label ?? ''}
                  >
                    {data.paymentsByMethod.map((entry, index) => (
                      <Cell
                        key={entry.label}
                        fill={METHOD_COLOURS[index % METHOD_COLOURS.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value: number) => formatAmount(value)}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </SectionCard>
      </div>

      <SectionCard
        title="Recent payments"
        description="Latest payments recorded for this school"
        action={
          <Badge variant="secondary">
            {formatNumber(data.paymentCount)} in period
          </Badge>
        }
      >
        {data.recentPayments.length === 0 ? (
          <EmptyState
            title="No payments recorded"
            description="Payments appear here as soon as they are entered by the accounts team."
          />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Student</TableHead>
                  <TableHead>Method</TableHead>
                  <TableHead>Receipt</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.recentPayments.map((payment) => (
                  <TableRow key={payment.id}>
                    <TableCell className="whitespace-nowrap">
                      {formatDate(payment.paymentDate)}
                    </TableCell>
                    <TableCell>
                      {payment.studentName ?? '—'}
                      {payment.admissionNumber ? (
                        <span className="block text-xs text-muted-foreground">
                          {payment.admissionNumber}
                        </span>
                      ) : null}
                    </TableCell>
                    <TableCell>{payment.paymentMethod}</TableCell>
                    <TableCell>{payment.receiptNumber ?? '—'}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatAmount(payment.amount)}
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={payment.status === 'COMPLETED' ? 'success' : 'warning'}
                      >
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

      <div className="grid gap-4 lg:grid-cols-2">
        <SectionCard
          title="Verification queue"
          description="Payments that are not yet confirmed as completed"
        >
          {data.pendingPayments.length === 0 ? (
            <EmptyState
              title="Nothing to verify"
              description="Every recorded payment for this school has been confirmed."
            />
          ) : (
            <ul className="space-y-2">
              {data.pendingPayments.map((payment) => (
                <li
                  key={payment.id}
                  className="flex items-center justify-between gap-3 rounded-lg border border-border p-3"
                >
                  <div>
                    <p className="text-sm font-medium">
                      {payment.studentName ?? `Payment #${payment.id}`}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {formatDate(payment.paymentDate)} · {payment.paymentMethod}
                    </p>
                  </div>
                  <span className="tabular-nums text-sm font-semibold">
                    {formatAmount(payment.amount)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </SectionCard>

        <SectionCard
          title="Fee structures"
          description="Billing and collection performance per fee structure"
        >
          {data.feeStructures.length === 0 ? (
            <EmptyState
              title="No fee structures"
              description="Fee structures created for this school will appear here."
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fee</TableHead>
                    <TableHead>Class</TableHead>
                    <TableHead className="text-right">Billed</TableHead>
                    <TableHead className="text-right">Collected</TableHead>
                    <TableHead className="text-right">Outstanding</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.feeStructures.map((structure) => (
                    <TableRow key={structure.feeStructureId}>
                      <TableCell>
                        {structure.name}
                        <span className="block text-xs text-muted-foreground">
                          {structure.feeType ?? '—'} · {structure.assignments} assigned
                        </span>
                      </TableCell>
                      <TableCell>{structure.className ?? 'All classes'}</TableCell>
                      <TableCell className="text-right tabular-nums">
                        {formatAmount(structure.billed)}
                      </TableCell>
                      <TableCell className="text-right tabular-nums">
                        {formatAmount(structure.collected)}
                      </TableCell>
                      <TableCell className="text-right tabular-nums">
                        {formatAmount(structure.outstanding)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard
          label="Students with balance"
          value={formatNumber(data.studentsWithOutstandingBalance)}
          icon={Users}
        />
        <StatCard
          label="Period"
          value={data.academicYearName ?? 'All time'}
          icon={CalendarDays}
        />
        <StatCard
          label="Pending verification value"
          value={formatAmount(data.pendingVerificationAmount)}
          icon={Clock}
        />
      </div>
    </div>
  );
}
