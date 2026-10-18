import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Skeleton } from '@/components/ui/Skeleton';
import { Input } from '@/components/ui/Input';
import { useState } from 'react';

export function AuditLogsPage() {
  const [search, setSearch] = useState('');
  const { data, isLoading } = useQuery({
    queryKey: ['audit-logs', search],
    queryFn: () => api.get(`/platform/audit-logs?action=${encodeURIComponent(search)}`).then(r => r.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Audit Logs</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Audit Logs</h1>
        <p className="mt-1 text-muted-foreground">Platform activity logs</p>
      </div>

      <Card className="p-4">
        <Input placeholder="Filter by action..." value={search} onChange={e => setSearch(e.target.value)} className="max-w-md" />
      </Card>

      <Card>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Time</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>Entity</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Actor</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.content?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground">No logs found</TableCell>
              </TableRow>
            )}
            {data?.content?.map((log: any) => (
              <TableRow key={log.id}>
                <TableCell className="text-sm">{new Date(log.createdAt).toLocaleString()}</TableCell>
                <TableCell><Badge variant="outline">{log.action}</Badge></TableCell>
                <TableCell>{log.entityType || '-'}</TableCell>
                <TableCell>{log.description}</TableCell>
                <TableCell>{log.actorEmail || '-'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>
    </div>
  );
}

