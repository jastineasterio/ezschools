import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Skeleton } from '@/components/ui/Skeleton';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

const actionOptions = ['PACKAGE_CREATE', 'PACKAGE_UPDATE', 'PACKAGE_STATUS_UPDATE', 'PLATFORM_SETTINGS_UPDATE'];

export function AuditLogsPage() {
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [entityFilter, setEntityFilter] = useState('');
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['audit-logs', search, actionFilter, entityFilter, page],
    queryFn: () =>
      api
        .get('/platform/audit-logs', {
          params: {
            search: search || undefined,
            action: actionFilter || undefined,
            entityType: entityFilter || undefined,
            page,
            size: 10,
          },
        })
        .then((r) => r.data.data),
  });

  const totalPages = query.data?.totalPages ?? 0;
  const logs = query.data?.content ?? [];

  const metrics = useMemo(
    () => ({
      total: query.data?.totalElements ?? 0,
      page: (query.data?.pageNumber ?? 0) + 1,
    }),
    [query.data]
  );

  if (query.isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Audit Logs</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">Audit Logs</h1>
          <p className="mt-1 text-muted-foreground">Platform activity logs</p>
        </div>
        <div className="text-sm text-muted-foreground">{metrics.total} records</div>
      </div>

      <Card className="p-4">
        <div className="grid gap-3 md:grid-cols-3">
          <Input placeholder="Search action, entity or description" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
          <select
            value={actionFilter}
            onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
            className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          >
            <option value="">All actions</option>
            {actionOptions.map((action) => (
              <option key={action} value={action}>{action}</option>
            ))}
          </select>
          <Input placeholder="Filter by entity" value={entityFilter} onChange={(e) => { setEntityFilter(e.target.value); setPage(0); }} />
        </div>
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
            {logs.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground">No logs found</TableCell>
              </TableRow>
            )}
            {logs.map((log: any) => (
              <TableRow key={log.id}>
                <TableCell className="text-sm">{new Date(log.createdAt).toLocaleString()}</TableCell>
                <TableCell><Badge variant="outline">{log.action}</Badge></TableCell>
                <TableCell>{log.entityType || '-'}</TableCell>
                <TableCell className="max-w-md">{log.description}</TableCell>
                <TableCell>{log.actorUserId ? `User #${log.actorUserId}` : 'System'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <div className="flex items-center justify-between">
        <div className="text-sm text-muted-foreground">Page {metrics.page} of {Math.max(totalPages, 1)}</div>
        <div className="flex gap-2">
          <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
          <Button variant="outline" disabled={page >= totalPages - 1 || totalPages === 0} onClick={() => setPage((p) => p + 1)}>Next</Button>
        </div>
      </div>
    </div>
  );
}

