import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import {
  School,
  Users,
  CreditCard,
  AlertTriangle,
  TrendingUp,
} from 'lucide-react';
import { getApiErrorMessage } from '@/services/api';
import { getPlatformDashboard, getPlatformSchools } from '@/services/dashboards';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatNumber, formatDate } from '@/lib/format';

const CHART_COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

export function PlatformDashboardPage() {
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['platform-dashboard'],
    queryFn: getPlatformDashboard,
  });

  const { data: schoolsPage, isLoading: schoolsLoading } = useQuery({
    queryKey: ['platform-schools'],
    queryFn: () => getPlatformSchools(0, 10),
  });

  const loading = isLoading || schoolsLoading;

  if (loading) {
    return (
      <div className="page space-y-6">
        <div className="page-header">
          <div>
            <h1 className="page-title">Platform Dashboard</h1>
            <p className="page-subtitle">Overview of your multi-school platform</p>
          </div>
        </div>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
          {[...Array(5)].map((_, i) => (
            <Card key={i} className="p-5">
              <Skeleton className="h-8 w-32 mb-3" />
              <Skeleton className="h-6 w-full" />
            </Card>
          ))}
        </div>
        <div className="grid gap-6 lg:grid-cols-2">
          <Card className="p-6"><Skeleton className="h-64" /></Card>
          <Card className="p-6"><Skeleton className="h-64" /></Card>
        </div>
        <Card className="p-6"><Skeleton className="h-48" /></Card>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="page space-y-6">
        <div className="page-header">
          <div>
            <h1 className="page-title">Platform Dashboard</h1>
            <p className="page-subtitle">Overview of your multi-school platform</p>
          </div>
        </div>
        <Alert variant="destructive">
          <AlertTitle>Unable to load platform dashboard</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(error, 'The platform data could not be loaded.')}{' '}
            <button className="underline" onClick={() => void refetch()}>
              Try again
            </button>
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (!data) {
    return null;
  }

  const stats = [
    {
      label: 'Total Schools',
      value: data.totalSchools,
      icon: School,
      tone: 'default' as const,
      hint: 'Schools registered on the platform',
    },
    {
      label: 'Active Schools',
      value: data.activeSchools,
      icon: TrendingUp,
      tone: 'success' as const,
      hint: 'Active and not suspended schools',
    },
    {
      label: 'Suspended Schools',
      value: data.suspendedSchools,
      icon: AlertTriangle,
      tone: 'destructive' as const,
      hint: 'Suspended schools',
    },
    {
      label: 'Total Users',
      value: data.totalUsers,
      icon: Users,
      tone: 'info' as const,
      hint: 'School-associated users (excludes platform-only admins)',
    },
    {
      label: 'Active Subscriptions',
      value: data.activeSubscriptions,
      icon: CreditCard,
      tone: 'success' as const,
      hint: 'Schools with active subscriptions',
    },
  ];

  const packageData = data.packageDistribution ?? [];
  const recentSchools = data.recentSchools ?? [];
  const recentAuditLogs = data.recentAuditLogs ?? [];
  const allSchools = schoolsPage?.content ?? [];

  return (
    <div className="page space-y-6">
      <div className="page-header">
        <div>
          <h1 className="page-title">Platform Dashboard</h1>
          <p className="page-subtitle">Overview of your multi-school platform</p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {stats.map((stat) => (
          <StatCard
            key={stat.label}
            label={stat.label}
            value={formatNumber(stat.value)}
            icon={stat.icon}
            tone={stat.tone}
            hint={stat.hint}
          />
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <SectionCard
          title="School Growth"
          description="Schools registered over time by package"
        >
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={packageData}>
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>

        <SectionCard
          title="Subscription Distribution"
          description="Schools by active subscription package"
        >
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={packageData}
                  dataKey="count"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  outerRadius="80%"
                  innerRadius="60%"
                  labelLine={false}
                >
                  {packageData.map((_entry, index) => (
                    <Cell key={`cell-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <SectionCard
          title="Recent School Registrations"
          description="Latest schools added to the platform"
        >
          {recentSchools.length === 0 ? (
            <EmptyState title="No schools" description="No schools have been registered yet." />
          ) : (
            <div className="space-y-3">
              {recentSchools.map((school) => (
                <div
                  key={school.id}
                  className="flex items-center justify-between gap-3 rounded-lg border p-3"
                >
                  <div>
                    <p className="font-medium text-foreground">{school.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {school.registrationNumber || 'No registration number'}
                    </p>
                  </div>
                  <Badge
                    variant={
                      school.isSuspended ? 'destructive' : school.isActive ? 'success' : 'secondary'
                    }
                  >
                    {school.isSuspended ? 'Suspended' : school.isActive ? 'Active' : 'Inactive'}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </SectionCard>

        <SectionCard
          title="Platform Activity"
          description="Recent audit events across the platform"
        >
          {recentAuditLogs.length === 0 ? (
            <EmptyState
              title="No recent activity"
              description="Platform audit events will appear here."
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Actor</TableHead>
                    <TableHead>Action</TableHead>
                    <TableHead>Entity</TableHead>
                    <TableHead className="text-right">Date</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {recentAuditLogs.map((log) => (
                    <TableRow key={log.id}>
                      <TableCell className="whitespace-nowrap">
                        {log.actorName ?? 'System'}
                      </TableCell>
                      <TableCell>{log.action}</TableCell>
                      <TableCell>{log.entityType ?? '—'}</TableCell>
                      <TableCell className="whitespace-nowrap text-right">
                        {formatDate(log.createdAt)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>

        <SectionCard
          title="All Schools"
          description="Complete list of schools registered on the platform"
          action={
            <Link to="/platform/schools">
              <Badge variant="secondary">View all {formatNumber(data.totalSchools)} schools</Badge>
            </Link>
          }
        >
          {schoolsLoading ? (
            <div className="h-48"><Skeleton className="h-full w-full" /></div>
          ) : allSchools.length === 0 ? (
            <EmptyState title="No schools" description="No schools registered yet." />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>School</TableHead>
                    <TableHead>Package</TableHead>
                    <TableHead className="text-right">Users</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                    <TableHead className="text-right">Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {allSchools.map((school) => (
                    <TableRow key={school.id}>
                      <TableCell className="font-medium whitespace-nowrap">
                        <Link to={`/platform/schools/${school.id}`}>{school.name}</Link>
                      </TableCell>
                      <TableCell>{school.packageName ?? '—'}</TableCell>
                      <TableCell className="text-right tabular-nums">
                        {formatNumber(school.userCount ?? 0)}
                      </TableCell>
                      <TableCell className="text-center">
                        <Badge
                          variant={
                            school.isSuspended
                              ? 'destructive'
                              : school.isActive
                                ? 'success'
                                : 'secondary'
                          }
                        >
                          {school.isSuspended ? 'Suspended' : school.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-right">
                        {formatDate(school.createdAt)}
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
