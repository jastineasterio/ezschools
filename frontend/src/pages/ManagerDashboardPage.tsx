import { useQuery } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import type { ApiResponse, SchoolResponse, SchoolBrandingResponse } from '@/types/api';
import { getSchoolUsers } from '@/services/users';
import { Users, Package, Building2, CreditCard } from 'lucide-react';
import { StatCard } from '@/components/dashboard/StatCard';
import { SectionCard } from '@/components/dashboard/EmptyState';
import { formatNumber } from '@/lib/format';

export function ManagerDashboardPage() {
  const profileQuery = useQuery({
    queryKey: ['school-profile'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<SchoolResponse>>('/school/profile')),
  });

  const brandingQuery = useQuery({
    queryKey: ['school-branding'],
    queryFn: () =>
      unwrapApiResponse(
        api.get<ApiResponse<SchoolBrandingResponse>>('/school/branding')
      ),
  });

  const usersQuery = useQuery({
    queryKey: ['school-users', 'dashboard-count'],
    queryFn: () => getSchoolUsers({ page: 0, size: 1 }),
  });

  if (profileQuery.isLoading) {
    return (
      <div className="page space-y-6">
        <div className="page-header">
          <div>
            <h1 className="page-title">School Dashboard</h1>
            <p className="page-subtitle">Overview of your school</p>
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
        <Card className="p-6"><Skeleton className="h-64" /></Card>
        <Card className="p-6"><Skeleton className="h-48" /></Card>
      </div>
    );
  }

  const profile = profileQuery.data;
  const branding = brandingQuery.data;
  const primaryColor = branding?.primaryColor || '#3b82f6';

  const userCount = usersQuery.data?.totalElements ?? 0;

  if (!profile) {
    return (
      <div className="page space-y-6">
        <div className="page-header">
          <div>
            <h1 className="page-title">School Dashboard</h1>
            <p className="page-subtitle">Overview of your school</p>
          </div>
        </div>
        <Alert variant="destructive">
          <AlertTitle>Unable to load school dashboard</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(profileQuery.error, 'The school information could not be loaded.')}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  return (
    <div className="page space-y-6">
      <div className="page-header">
        <div className="flex items-center gap-4">
          {branding?.logoUrl ? (
            <img src={branding.logoUrl} alt="Logo" className="h-10 w-10 rounded-xl object-contain" />
          ) : (
            <div
              className="flex h-10 w-10 items-center justify-center rounded-xl text-sm font-bold text-white shadow-sm"
              style={{ backgroundColor: primaryColor }}
            >
              {profile.name[0] || 'S'}
            </div>
          )}
          <div>
            <h1 className="page-title">{profile.name}</h1>
            <p className="page-subtitle">School overview and key metrics</p>
          </div>
        </div>
      </div>

      {profileQuery.isError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load school dashboard</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(profileQuery.error, 'The school information could not be loaded.')}
          </AlertDescription>
        </Alert>
      )}
      {brandingQuery.isError && (
        <Alert variant="destructive">
          <AlertTitle>Branding could not be loaded</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(brandingQuery.error, 'The school logo and colors are unavailable.')}
          </AlertDescription>
        </Alert>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Platform Users"
          value={formatNumber(userCount)}
          icon={Users}
          hint="Users with school access"
        />
        <StatCard
          label="Package"
          value={profile.packageName || 'None'}
          icon={Package}
          hint="Current subscription package"
        />
        <StatCard
          label="Subscription"
          value={profile.subscriptionStatus || 'Inactive'}
          tone={
            profile.subscriptionStatus === 'ACTIVE'
              ? 'success'
              : profile.subscriptionStatus === 'EXPIRED'
                ? 'destructive'
                : 'warning'
          }
          icon={CreditCard}
          hint="Subscription status"
        />
        <StatCard
          label="School Status"
          value={profile.isSuspended ? 'Suspended' : profile.isActive ? 'Active' : 'Inactive'}
          tone={profile.isSuspended ? 'destructive' : profile.isActive ? 'success' : 'default'}
          icon={Building2}
          hint="Operational status"
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <SectionCard
          title="School Information"
          description="Key details about your institution"
        >
          <div className="space-y-3">
            <div className="flex items-center justify-between py-2 border-b border-border">
              <span className="text-sm text-muted-foreground">Package</span>
              <span className="text-sm font-medium text-foreground">
                {profile.packageName || 'No package assigned'}
              </span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-border">
              <span className="text-sm text-muted-foreground">Subscription</span>
              <Badge
                variant={
                  profile.subscriptionStatus === 'ACTIVE'
                    ? 'success'
                    : profile.subscriptionStatus === 'EXPIRED'
                      ? 'destructive'
                      : 'warning'
                }
              >
                {profile.subscriptionStatus || 'No subscription'}
              </Badge>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-border">
              <span className="text-sm text-muted-foreground">Email</span>
              <span className="text-sm text-foreground truncate max-w-xs">
                {profile.email || 'Email not provided'}
              </span>
            </div>
            <div className="flex items-center justify-between py-2">
              <span className="text-sm text-muted-foreground">Phone</span>
              <span className="text-sm text-foreground truncate max-w-xs">
                {profile.phone || 'Phone not provided'}
              </span>
            </div>
          </div>
        </SectionCard>

        <SectionCard
          title="Quick Actions"
          description="Common tasks for school management"
        >
          <div className="grid gap-3 sm:grid-cols-2">
            <button className="btn-outline w-full justify-between">
              <span>Manage Students</span>
              <Users className="h-4 w-4" />
            </button>
            <button className="btn-outline w-full justify-between">
              <span>Manage Teachers</span>
              <Building2 className="h-4 w-4" />
            </button>
            <button className="btn-outline w-full justify-between">
              <span>Record Attendance</span>
              <CreditCard className="h-4 w-4" />
            </button>
            <button className="btn-outline w-full justify-between">
              <span>View Payments</span>
              <Users className="h-4 w-4" />
            </button>
          </div>
        </SectionCard>
      </div>
    </div>
  );
}
