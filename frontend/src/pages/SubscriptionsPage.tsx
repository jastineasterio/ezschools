import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Skeleton } from '@/components/ui/Skeleton';
import { AlertCircle } from 'lucide-react';

export function SubscriptionsPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['subscriptions'],
    queryFn: () => api.get('/platform/subscriptions').then(r => r.data.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Subscriptions</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Subscriptions</h1>
        <Card className="p-6">
          <div className="flex items-center gap-2 text-red-600">
            <AlertCircle className="h-5 w-5" />
            <p>Failed to load subscriptions. Please try again later.</p>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Subscriptions</h1>
        <p className="mt-1 text-muted-foreground">Manage school subscriptions</p>
      </div>

      <Card>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>School</TableHead>
              <TableHead>Package</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Start Date</TableHead>
              <TableHead>End Date</TableHead>
              <TableHead>Payment</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">No subscriptions found</TableCell>
              </TableRow>
            )}
            {data?.map((sub: any) => (
              <TableRow key={sub.id}>
                <TableCell className="font-medium">{sub.school?.name}</TableCell>
                <TableCell>{sub.package?.displayName}</TableCell>
                <TableCell><Badge variant={sub.status === 'ACTIVE' ? 'success' : 'warning'}>{sub.status}</Badge></TableCell>
                <TableCell>{new Date(sub.startDate).toLocaleDateString()}</TableCell>
                <TableCell>{sub.endDate ? new Date(sub.endDate).toLocaleDateString() : '-'}</TableCell>
                <TableCell><Badge variant="outline">{sub.paymentStatus}</Badge></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
    </div>
  );
}

