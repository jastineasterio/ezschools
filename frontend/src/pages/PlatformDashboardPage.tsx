import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/Skeleton';
import { Badge } from '@/components/ui/Badge';
import { Users, School, CreditCard, AlertTriangle, TrendingUp } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

export function PlatformDashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['platform-dashboard'],
    queryFn: () => api.get('/platform/dashboard').then(r => r.data.data),
  });

  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {[...Array(6)].map((_, i) => (
          <Card key={i}><Skeleton className="h-32" /></Card>
        ))}
      </div>
    );
  }

  const stats = [
    { label: 'Total Schools', value: data?.totalSchools || 0, icon: School, color: 'bg-blue-500' },
    { label: 'Active Schools', value: data?.activeSchools || 0, icon: TrendingUp, color: 'bg-green-500' },
    { label: 'Suspended Schools', value: data?.suspendedSchools || 0, icon: AlertTriangle, color: 'bg-red-500' },
    { label: 'Total Users', value: data?.totalUsers || 0, icon: Users, color: 'bg-purple-500' },
    { label: 'Active Subscriptions', value: data?.activeSubscriptions || 0, icon: CreditCard, color: 'bg-orange-500' },
  ];

  const packageData = data?.packageDistribution || [];
  const recentSchools = data?.recentSchools || [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Platform Dashboard</h1>
        <p className="mt-1 text-muted-foreground">Overview of your multi-school platform</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {stats.map((stat) => (
          <Card key={stat.label} className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">{stat.label}</p>
                <p className="mt-2 text-3xl font-bold text-foreground">{stat.value}</p>
              </div>
              <div className={`rounded-lg p-3 ${stat.color} bg-opacity-10`}>
                <stat.icon className={`h-6 w-6 ${stat.color.replace('bg-', 'text-')}`} />
              </div>
            </div>
          </Card>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="text-lg font-semibold">Package Distribution</h2>
          {packageData.length === 0 ? (
            <p className="mt-4 text-muted-foreground">No package distribution data available.</p>
          ) : (
            <div className="mt-4 h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={packageData}>
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="count" fill="#3b82f6" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <Card className="p-6">
          <h2 className="text-lg font-semibold">Recent Schools</h2>
          <div className="mt-4 space-y-3">
            {recentSchools.length === 0 && <p className="text-muted-foreground">No schools yet</p>}
            {recentSchools.map((school: any) => (
              <div key={school.id} className="flex items-center justify-between rounded-lg border p-3">
                <div>
                  <p className="font-medium text-foreground">{school.name}</p>
                  <p className="text-sm text-muted-foreground">{school.region || 'No region'}</p>
                </div>
                <Badge variant={school.isSuspended ? 'destructive' : 'success'}>
                  {school.isSuspended ? 'Suspended' : 'Active'}
                </Badge>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
