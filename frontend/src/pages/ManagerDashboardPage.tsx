import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Users, Package, MessageSquare } from 'lucide-react';

export function ManagerDashboardPage() {
  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['school-profile'],
    queryFn: () => api.get('/school/profile').then(r => r.data),
  });

  const { data: branding } = useQuery({
    queryKey: ['school-branding'],
    queryFn: () => api.get('/school/branding').then(r => r.data),
  });

  if (profileLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {[...Array(4)].map((_, i) => (
          <Card key={i} className="p-6"><Skeleton className="h-24" /></Card>
        ))}
      </div>
    );
  }

  const primaryColor = branding?.primaryColor || '#3b82f6';

  return (
    <div className="space-y-6">
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
          <h1 className="text-3xl font-bold">{profile?.name}</h1>
          <p className="mt-1 text-muted-foreground">School Dashboard</p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Current Package</p>
              <p className="mt-2 text-xl font-bold">{profile?.package?.displayName || 'None'}</p>
            </div>
            <Package className="h-8 w-8 text-muted-foreground" />
          </div>
          <Badge variant="outline" className="mt-4">{profile?.subscription?.status || 'No subscription'}</Badge>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Users</p>
              <p className="mt-2 text-xl font-bold">{profile?.userCount || 0}</p>
            </div>
            <Users className="h-8 w-8 text-muted-foreground" />
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">SMS Configuration</p>
              <p className="mt-2 text-xl font-bold">{profile?.smsConfigured ? 'Configured' : 'Not Configured'}</p>
            </div>
            <MessageSquare className="h-8 w-8 text-muted-foreground" />
          </div>
        </Card>
      </div>
    </div>
  );
}

