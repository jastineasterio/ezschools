import { useQuery } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import type { ApiResponse, SchoolBrandingResponse, SchoolResponse } from '@/types/api';
import { getSchoolUsers } from '@/services/users';
import { Users, Package } from 'lucide-react';

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
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {[...Array(4)].map((_, i) => (
          <Card key={i} className="p-6"><Skeleton className="h-24" /></Card>
        ))}
      </div>
    );
  }

  const profile = profileQuery.data;
  const branding = brandingQuery.data;
  const primaryColor = branding?.primaryColor || '#3b82f6';

  return (
    <div className="space-y-6">
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
      <div className="flex items-center gap-4">
        {branding?.logoUrl ? (
          <img src={branding.logoUrl} alt="Logo" className="h-12 w-12 rounded-lg object-contain" />
        ) : (
          <div
            className="flex h-12 w-12 items-center justify-center rounded-xl text-white font-bold"
            style={{ backgroundColor: primaryColor }}
          >
            {profile?.name?.[0] || 'S'}
          </div>
        )}
        <div>
          <h1 className="text-3xl font-bold">{profile?.name || 'School Dashboard'}</h1>
          <p className="mt-1 text-muted-foreground">School Dashboard</p>
        </div>
      </div>

      {profile && (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Current Package</p>
              <p className="mt-2 text-xl font-bold">{profile.packageName || 'No package assigned'}</p>
            </div>
            <Package className="h-8 w-8 text-muted-foreground" />
          </div>
          <Badge variant="outline" className="mt-4">{profile.subscriptionStatus || 'No subscription'}</Badge>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Users</p>
              {usersQuery.isLoading ? (
                <Skeleton className="mt-2 h-7 w-16" />
              ) : usersQuery.isError || !usersQuery.data ? (
                <p className="mt-2 text-sm text-muted-foreground">Unavailable</p>
              ) : (
                <p className="mt-2 text-xl font-bold">{usersQuery.data.totalElements}</p>
              )}
            </div>
            <Users className="h-8 w-8 text-muted-foreground" />
          </div>
        </Card>

        <Card className="p-6">
          <p className="text-sm text-muted-foreground">School contact</p>
          <p className="mt-2 font-medium">{profile.email || 'Email not provided'}</p>
          <p className="mt-1 font-medium">{profile.phone || 'Phone not provided'}</p>
        </Card>
      </div>
      )}
    </div>
  );
}
