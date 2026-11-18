import { useState, type Dispatch, type SetStateAction } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
  BarChart, Bar,
} from 'recharts';
import { Users, UserCheck, Clock, Building, School, AlertTriangle, TrendingUp } from 'lucide-react';
import {
  getUserOverview,
  getSchoolUserDistribution,
  getPackageUserDistribution,
  getRegistrationTrends,
} from '@/services/analytics';
import { Card } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/Skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatNumber } from '@/lib/format';
import type { LucideIcon } from 'lucide-react';
import type {
  RoleDistributionDto,
  ActiveInactiveDto,
  TrendDto,
  SchoolUserDistributionDto,
  PackageUserDistributionDto,
} from '@/types/api';

type AnalyticsPageFilters = {
  start: string;
  end: string;
  school: string;
  packageId: string;
  role: string;
  status: string;
  search: string;
  page: number;
  size: number;
  sort: string;
};

function startOfDay(date: string) {
  return `${date}T00:00:00`;
}function endOfDay(date: string) {
  return `${date}T23:59:59`;
}

function chartColor(index: number) {
  const colors = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#14b8a6', '#f97316'];
  return colors[index % colors.length];
}

function statCard(
  label: string,
  value: number | null | undefined,
  icon: LucideIcon,
  tone: 'default' | 'success' | 'warning' | 'destructive' | 'info',
  hint: string,
) {
  return (
    <StatCard
      label={label}
      value={value == null ? '—' : formatNumber(value)}
      icon={icon}
      tone={tone}
      hint={hint}
    />
  );
}

export function PlatformUserAnalyticsPage() {
  const [filters, setFilters] = useState<AnalyticsPageFilters>({
    start: '',
    end: '',
    school: '',
    packageId: '',
    role: '',
    status: '',
    search: '',
    page: 0,
    size: 20,
    sort: 'totalUsers,desc',
  });

  const today = new Date().toISOString().slice(0, 10);
  const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  if (!filters.start) {
    setFilters((f) => ({ ...f, start: thirtyDaysAgo, end: today }));
  }

  const apiFilters = {
    start: startOfDay(filters.start),
    end: endOfDay(filters.end),
    school: filters.school || undefined,
    package: filters.packageId || undefined,
    role: filters.role || undefined,
    status: filters.status || undefined,
  };

  const { data: overview, isLoading: overviewLoading } = useQuery({
    queryKey: ['platform-user-overview', apiFilters],
    queryFn: () => getUserOverview(apiFilters),
    enabled: !!filters.start && !!filters.end,
  });

  const { data: schoolPage, isLoading: schoolsLoading } = useQuery({
    queryKey: [
      'platform-user-schools',
      { ...apiFilters, page: filters.page, size: filters.size, sort: filters.sort, search: filters.search },
    ],
    queryFn: () =>
      getSchoolUserDistribution({
        ...apiFilters,
        page: filters.page,
        size: filters.size,
        search: filters.search || undefined,
        sort: filters.sort,
      }),
    enabled: !!filters.start && !!filters.end,
  });

  const { data: packages, isLoading: packagesLoading } = useQuery({
    queryKey: ['platform-user-packages', apiFilters],
    queryFn: () => getPackageUserDistribution(apiFilters),
    enabled: !!filters.start && !!filters.end,
  });

  const { data: trends, isLoading: trendsLoading } = useQuery({
    queryKey: ['platform-user-trends', apiFilters],
    queryFn: () => getRegistrationTrends({ ...apiFilters, granularity: 'day' }),
    enabled: !!filters.start && !!filters.end,
  });

  const loading = overviewLoading || schoolsLoading || packagesLoading || trendsLoading;

  return (
    <div className="page space-y-6">
      <div className="page-header">
        <div>
          <h1 className="page-title">Platform User Analytics</h1>
          <p className="page-subtitle">
            Aggregated distribution, composition, growth, and status of users across the EduManage platform.
            Individual user profiles are not shown here.
          </p>
        </div>
      </div>

      {loading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
          {[...Array(8)].map((_, i) => (
            <Card key={i} className="p-5">
              <Skeleton className="h-8 w-32 mb-3" />
              <Skeleton className="h-6 w-full" />
            </Card>
          ))}
        </div>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            {statCard('Total registered users', overview?.totalUsers, Users, 'info',
              'Distinct non-deleted users across all schools')}
            {statCard('Active users', overview?.activeUsers, UserCheck, 'success',
              'Users with active accounts')}
            {statCard('Inactive users', overview?.inactiveUsers, AlertTriangle, 'warning',
              'Disabled non-deleted accounts')}
            {statCard('Deleted / archived', overview?.deletedUsers, Clock, 'destructive',
              'Soft-deleted accounts excluded from totals')}
            {statCard('Total schools', overview?.totalSchools, School, 'default',
              'Schools registered on the platform')}
            {statCard('Avg users per school', Math.round(overview?.averageUsersPerSchool ?? 0), Building, 'default',
              'Total users divided by total schools')}
            {statCard('New users (period)', overview?.newUsersPeriod, TrendingUp, 'success',
              'Users registered during the selected period')}
            {statCard('Growth (%)', overview?.growthPercent, TrendingUp,
              overview?.growthPercent == null ? 'default' : overview.growthPercent >= 0 ? 'success' : 'destructive',
              overview?.growthPercent == null ? 'Insufficient historical data' : 'Change vs prior period')}
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <SectionCard
              title="User distribution by role"
              description="Each platform user is counted once under their primary role"
            >
              <div className="h-72 w-full">
                <RoleDonut data={overview?.roleDistribution ?? []} />
              </div>
            </SectionCard>
            <SectionCard
              title="Active vs inactive"
              description="Status of non-deleted platform users"
            >
              <div className="h-72 w-full">
                <StatusDonut data={overview?.statusDistribution ?? []} />
              </div>
            </SectionCard>
          </div>

          <SectionCard
            title="User registration trend"
            description="New non-deleted users registered per day"
          >
            <div className="h-72 w-full">
              <TrendLine data={trends ?? []} />
            </div>
          </SectionCard>

          <SchoolSection
            filters={filters}
            setFilters={setFilters}
            schoolPage={schoolPage}
            loading={schoolsLoading}
          />

          <PackageSection packages={packages ?? []} loading={packagesLoading} />
        </>
      )}
    </div>
  );
}

function RoleDonut({ data }: { data: RoleDistributionDto[] }) {
  const total = data.reduce((sum, d) => sum + d.userCount, 0);
  return (
    <div className="h-full">
      {total === 0 ? (
        <EmptyState title="No users" description="No user records in the selected period." />
      ) : (
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              dataKey="userCount"
              nameKey="role"
              cx="50%"
              cy="50%"
              innerRadius="60%"
              outerRadius="80%"
              labelLine={false}
            >
              {data.map((entry) => (
                <Cell key={`cell-${entry.role}`} fill={chartColor(data.indexOf(entry))} />
              ))}
            </Pie>
            <Tooltip content={({ active, payload }: { active?: boolean; payload?: readonly { payload?: unknown }[] | null }) => {
              if (active && payload?.length) {
                const entry = payload[0].payload as RoleDistributionDto;
                return (
                  <div className="p-2 border rounded bg-background text-sm">
                    <p className="font-medium">{entry.displayName}</p>
                    <p className="text-muted-foreground">{entry.userCount} users ({entry.percent.toFixed(1)}%)</p>
                  </div>
                );
              }
              return null;
            }} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

function StatusDonut({ data }: { data: ActiveInactiveDto[] }) {
  const total = data.reduce((sum, d) => sum + d.count, 0);
  return (
    <div className="h-full">
      {total === 0 ? (
        <EmptyState title="No users" description="No user records in the selected period." />
      ) : (
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              dataKey="count"
              nameKey="status"
              cx="50%"
              cy="50%"
              innerRadius="60%"
              outerRadius="80%"
              labelLine={false}
            >
              {data.map((entry) => (
                <Cell key={`cell-${entry.status}`} fill={
                  entry.status === 'active' ? '#22c55e' : entry.status === 'inactive' ? '#f59e0b' : '#ef4444'
                } />
              ))}
            </Pie>
            <Tooltip content={({ active, payload }: { active?: boolean; payload?: readonly { payload?: unknown }[] | null }) => {
              if (active && payload?.length) {
                const entry = payload[0].payload as ActiveInactiveDto;
                return (
                  <div className="p-2 border rounded bg-background text-sm">
                    <p className="font-medium">{entry.status}</p>
                    <p className="text-muted-foreground">{entry.count} users ({entry.percent.toFixed(1)}%)</p>
                  </div>
                );
              }
              return null;
            }} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

function TrendLine({ data }: { data: TrendDto[] }) {
  return (
    <div className="h-full">
      {data.length === 0 ? (
        <EmptyState title="No registrations" description="No users registered in the selected period." />
      ) : (
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data}>
            <XAxis dataKey="period" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 11 }} />
            <Tooltip
              content={({ active, payload }: { active?: boolean; payload?: readonly { payload?: unknown }[] | null }) => {
                if (active && payload?.length) {
                  const point = payload[0].payload as TrendDto;
                  return (
                    <div className="p-2 border rounded bg-background text-sm">
                      <p className="font-medium">{point.period}</p>
                      <p className="text-muted-foreground">{point.userCount} new users</p>
                    </div>
                  );
                }
                return null;
              }}
            />
            <Line type="monotone" dataKey="userCount" stroke="#3b82f6" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

interface SchoolSectionProps {
  filters: AnalyticsPageFilters;
  setFilters: Dispatch<SetStateAction<AnalyticsPageFilters>>;
  schoolPage: {
    content: SchoolUserDistributionDto[];
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
  } | null | undefined;
  loading: boolean;
}
function SchoolSection({ filters, setFilters, schoolPage, loading }: SchoolSectionProps) {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const selected = schoolPage?.content.find((s) => s.schoolId === selectedId);

  const handlePage = (page: number) =>
    setFilters((f) => ({ ...f, page }));
  const handleSort = (sort: string) =>
    setFilters((f) => ({ ...f, sort }));

  return (
    <SectionCard
      title="School-wise user distribution"
      description="Aggregated users per school. Search by school name, sort by user count, paginated."
      action={
        <div className="flex items-center gap-2">
          <Input placeholder="Search schools…" value={filters.search}
            onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value, page: 0 }))} />
        </div>
      }
    >
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <div className="h-64 w-full">
            <TopSchoolsBar data={schoolPage?.content ?? []} />
          </div>
        </div>
        <div>
          {selected && (
            <div className="space-y-3">
              <h3 className="text-sm font-semibold text-foreground">
                Role distribution — {selected.schoolName}
              </h3>
              <div className="h-48 w-full">
                <SchoolRoleDonut school={selected} />
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="mt-4 overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="cursor-pointer" onClick={() => handleSort('totalUsers,desc')}>
                School
              </TableHead>
              <TableHead className="cursor-pointer text-right" onClick={() => handleSort('totalUsers,desc')}>
                Total users
              </TableHead>
              <TableHead className="text-right">Managers</TableHead>
              <TableHead className="text-right">Teachers</TableHead>
              <TableHead className="text-right">Students</TableHead>
              <TableHead className="text-right">Parents</TableHead>
              <TableHead className="text-right">Accountants</TableHead>
              <TableHead className="text-right">HODs</TableHead>
              <TableHead className="text-right">Librarians</TableHead>
              <TableHead className="text-right">Wardens</TableHead>
              <TableHead className="text-right">Transport</TableHead>
              <TableHead className="text-right">Other</TableHead>
              <TableHead className="text-right">Active</TableHead>
              <TableHead className="text-right">Inactive</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={13} className="h-32">
                  <Skeleton className="h-full w-full" />
                </TableCell>
              </TableRow>
            ) : schoolPage?.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={13} className="text-center py-4 text-muted-foreground">
                  No schools match the selected filters.
                </TableCell>
              </TableRow>
            ) : (
              schoolPage?.content.map((school) => (
                <TableRow
                  key={school.schoolId}
                  onClick={() => setSelectedId(selectedId === school.schoolId ? null : school.schoolId)}
                  className="cursor-pointer hover:bg-muted/50 transition"
                >
                  <TableCell className="font-medium whitespace-nowrap">{school.schoolName}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.totalUsers)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.managers)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.teachers)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.students)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.parents)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.accountants)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.hods)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.librarians)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.wardens)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.transportOfficers)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.otherRoles)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.activeUsers)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(school.inactiveUsers)}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {schoolPage && (
        <div className="mt-3 flex items-center justify-between text-sm text-muted-foreground">
          <span>
            Showing {schoolPage?.content.length ?? 0} of {schoolPage?.totalElements ?? 0} schools
          </span>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={() => handlePage(filters.page - 1)} disabled={filters.page === 0}>
              Prev
            </Button>
            <Button variant="outline" size="sm" onClick={() => handlePage(filters.page + 1)} disabled={filters.page >= schoolPage.totalPages - 1}>
              Next
            </Button>
            <span>Page {filters.page + 1} of {Math.max(1, schoolPage.totalPages)}</span>
          </div>
        </div>
      )}
    </SectionCard>
  );
}

function TopSchoolsBar({ data }: { data: SchoolUserDistributionDto[] }) {
  const top = [...data].sort((a, b) => b.totalUsers - a.totalUsers).slice(0, 10);
  return (
    <SectionCard title="Schools with highest user population" description="Top 10 schools by total users">
      <div className="h-64 w-full">
        {top.length === 0 ? (
          <EmptyState title="No schools" description="No school data available." />
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={top}>
              <XAxis dataKey="schoolName" tick={{ fontSize: 10, rotate: 30 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip />
              <Bar dataKey="totalUsers" fill="#3b82f6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </SectionCard>
  );
}

function SchoolRoleDonut({ school }: { school: SchoolUserDistributionDto }) {
  const roles = [
    { role: 'MANAGER', displayName: 'Managers', count: school.managers },
    { role: 'TEACHER', displayName: 'Teachers', count: school.teachers },
    { role: 'STUDENT', displayName: 'Students', count: school.students },
    { role: 'PARENT', displayName: 'Parents', count: school.parents },
    { role: 'ACCOUNTANT', displayName: 'Accountants', count: school.accountants },
    { role: 'HOD', displayName: 'HODs', count: school.hods },
    { role: 'LIBRARIAN', displayName: 'Librarians', count: school.librarians },
    { role: 'WARDEN', displayName: 'Wardens', count: school.wardens },
    { role: 'TRANSPORT_OFFICER', displayName: 'Transport Officers', count: school.transportOfficers },
    { role: 'OTHER', displayName: 'Other', count: school.otherRoles },
  ].filter((r) => r.count > 0);

  const total = roles.reduce((sum, r) => sum + r.count, 0);
  return (
    <div className="h-full">
      {total === 0 ? (
        <EmptyState title="No users" description="No users assigned to this school." />
      ) : (
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={roles}
              dataKey="count"
              nameKey="displayName"
              cx="50%"
              cy="50%"
              innerRadius="60%"
              outerRadius="80%"
              labelLine={false}
            >
              {roles.map((entry) => (
                <Cell key={`cell-${entry.role}`} fill={chartColor(roles.indexOf(entry))} />
              ))}
            </Pie>
            <Tooltip content={({ active, payload }: { active?: boolean; payload?: readonly { payload?: unknown }[] | null }) => {
              if (active && payload?.length) {
                const entry = payload[0].payload as { displayName: string; count: number };
                return (
                  <div className="p-2 border rounded bg-background text-sm">
                    <p className="font-medium">{entry.displayName}</p>
                    <p className="text-muted-foreground">{entry.count} users</p>
                  </div>
                );
              }
              return null;
            }} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

interface PackageSectionProps {
  packages: PackageUserDistributionDto[];
  loading: boolean;
}
function PackageSection({ packages, loading }: PackageSectionProps) {
  const totalSchools = packages.reduce((sum, p) => sum + p.schoolCount, 0);
  const totalUsers = packages.reduce((sum, p) => sum + p.totalUsers, 0);
  return (
    <SectionCard
      title="Package-wise user distribution"
      description="Users grouped by the school's currently assigned subscription package"
    >
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="h-72 w-full">
          <PackageDonut data={packages} />
        </div>
        <div className="h-72 w-full">
          <PackageBar data={packages} />
        </div>
      </div>

      <div className="mt-4 overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Package</TableHead>
              <TableHead className="text-right">Subscribed schools</TableHead>
              <TableHead className="text-right">Total users</TableHead>
              <TableHead className="text-right">Avg users / school</TableHead>
              <TableHead className="text-right">Active</TableHead>
              <TableHead className="text-right">Inactive</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={6} className="h-32"><Skeleton className="h-full w-full" /></TableCell>
              </TableRow>
            ) : packages.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-4 text-muted-foreground">
                  No package data available.
                </TableCell>
              </TableRow>
            ) : (
              packages.map((pkg) => (
                <TableRow key={pkg.packageId ?? 'unassigned'}>
                  <TableCell className="font-medium whitespace-nowrap">
                    {pkg.unassigned ? 'Unassigned' : pkg.displayName}
                    {pkg.unassigned ? (
                      <span className="ml-2 text-xs text-muted-foreground">(no active subscription)</span>
                    ) : null}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(pkg.schoolCount)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(pkg.totalUsers)}</TableCell>
                  <TableCell className="text-right tabular-nums">
                    {pkg.schoolCount > 0 ? formatNumber(Math.round(pkg.averageUsersPerSchool)) : '—'}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(pkg.activeUsers)}</TableCell>
                  <TableCell className="text-right tabular-nums">{formatNumber(pkg.inactiveUsers)}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
      <div className="mt-2 text-xs text-muted-foreground">
        Total: {formatNumber(totalUsers)} users across {formatNumber(totalSchools)} schools.
      </div>
    </SectionCard>
  );
}

function PackageDonut({ data }: { data: PackageUserDistributionDto[] }) {
  const total = data.reduce((sum, p) => sum + p.totalUsers, 0);
  return (
    <div className="h-full">
      {total === 0 ? (
        <EmptyState title="No users" description="No package data available." />
      ) : (
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              dataKey="totalUsers"
              nameKey="displayName"
              cx="50%"
              cy="50%"
              innerRadius="60%"
              outerRadius="80%"
              labelLine={false}
            >
              {data.map((entry) => (
                <Cell key={`cell-${entry.packageId ?? 'unassigned'}`} fill={chartColor(data.indexOf(entry))} />
              ))}
            </Pie>
            <Tooltip content={({ active, payload }: { active?: boolean; payload?: readonly { payload?: unknown }[] | null }) => {
              if (active && payload?.length) {
                const entry = payload[0].payload as PackageUserDistributionDto;
                return (
                  <div className="p-2 border rounded bg-background text-sm">
                    <p className="font-medium">{entry.unassigned ? 'Unassigned' : entry.displayName}</p>
                    <p className="text-muted-foreground">{entry.totalUsers} users</p>
                  </div>
                );
              }
              return null;
            }} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}

function PackageBar({ data }: { data: PackageUserDistributionDto[] }) {
  const sorted = [...data].sort((a, b) => b.totalUsers - a.totalUsers);
  return (
    <SectionCard title="Users by package" description="Bar comparison of package populations">
      <div className="h-64 w-full">
        {sorted.length === 0 ? (
          <EmptyState title="No data" description="No package data available." />
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={sorted}>
              <XAxis dataKey="displayName" tick={{ fontSize: 10, rotate: 30 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip />
              <Bar dataKey="totalUsers" fill="#3b82f6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </SectionCard>
  );
}
