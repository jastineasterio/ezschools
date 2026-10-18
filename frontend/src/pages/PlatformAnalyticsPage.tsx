import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/Skeleton';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

export function PlatformAnalyticsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['platform-dashboard'],
    queryFn: () => api.get('/platform/dashboard').then(r => r.data.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Analytics</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Analytics</h1>
        <p className="mt-1 text-muted-foreground">Platform overview and trends</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <Card className="p-6">
          <p className="text-sm text-muted-foreground">Total Schools</p>
          <p className="mt-2 text-3xl font-bold">{data?.totalSchools || 0}</p>
        </Card>
        <Card className="p-6">
          <p className="text-sm text-muted-foreground">Active Schools</p>
          <p className="mt-2 text-3xl font-bold">{data?.activeSchools || 0}</p>
        </Card>
        <Card className="p-6">
          <p className="text-sm text-muted-foreground">Suspended Schools</p>
          <p className="mt-2 text-3xl font-bold">{data?.suspendedSchools || 0}</p>
        </Card>
        <Card className="p-6">
          <p className="text-sm text-muted-foreground">Total Users</p>
          <p className="mt-2 text-3xl font-bold">{data?.totalUsers || 0}</p>
        </Card>
        <Card className="p-6">
          <p className="text-sm text-muted-foreground">Active Subscriptions</p>
          <p className="mt-2 text-3xl font-bold">{data?.activeSubscriptions || 0}</p>
        </Card>
      </div>

      <Card className="p-6">
        <h2 className="text-lg font-semibold">Package Distribution</h2>
        <div className="mt-4 h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data?.packageDistribution || []}>
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill="#3b82f6" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </div>
  );
}

