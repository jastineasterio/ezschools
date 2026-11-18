import { useState, type ChangeEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
  LineChart, Line,
} from 'recharts';
import { FileText, Activity, ShieldAlert, Users as UsersIcon } from 'lucide-react';
import {
  getAuditLogs,
  getAuditOverview,
  getAuditTrend,
  getAuditActionDistribution,
  getAuditResultDistribution,
} from '@/services/audit';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatNumber, formatDateTime } from '@/lib/format';
import type { AuditChartPoint, AuditDistributionItem, AuditLogResponse } from '@/types/api';
import type { LucideIcon } from 'lucide-react';

function startOfDay(date: string) {
  return `${date}T00:00:00`;
}
function endOfDay(date: string) {
  return `${date}T23:59:59`;
}

function resultVariant(result: string | null) {
  return result === 'FAILURE' ? 'destructive' : result === 'SUCCESS' ? 'success' : 'secondary';
}

function statCard(
  label: string,
  value: number | null | undefined,
  icon: LucideIcon,
  tone: 'default' | 'success' | 'warning' | 'destructive' | 'info',
  hint: string,
) {
  return (
    <StatCard label={label} value={value == null ? '—' : formatNumber(value)} icon={icon} tone={tone} hint={hint} />
  );
}

const PRESETS: { label: string; days: number }[] = [
  { label: 'Today', days: 0 },
  { label: 'Yesterday', days: 1 },
  { label: 'Last 7 days', days: 7 },
  { label: 'Last 30 days', days: 30 },
];

export function AuditLogsPage() {
  const [selectedLog, setSelectedLog] = useState<AuditLogResponse | null>(null);
  const today = new Date().toISOString().slice(0, 10);
  const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  const [filters, setFilters] = useState<{
    start: string;
    end: string;
    search: string;
    action: string;
    entityType: string;
    result: string;
    actor: string;
    school: string;
    page: number;
    size: number;
  }>({
    start: thirtyDaysAgo,
    end: today,
    search: '',
    action: '',
    entityType: '',
    result: '',
    actor: '',
    school: '',
    page: 0,
    size: 20,
  });

  const apiFilters = {
    page: filters.page,
    size: filters.size,
    search: filters.search || undefined,
    action: filters.action || undefined,
    entityType: filters.entityType || undefined,
    result: filters.result || undefined,
    actor: filters.actor && !Number.isNaN(Number(filters.actor)) ? Number(filters.actor) : undefined,
    school: filters.school && !Number.isNaN(Number(filters.school)) ? Number(filters.school) : undefined,
    start: startOfDay(filters.start),
    end: endOfDay(filters.end),
  };

  const trendParams = {
    start: apiFilters.start,
    end: apiFilters.end,
    search: apiFilters.search,
    action: apiFilters.action,
    entityType: apiFilters.entityType,
    result: apiFilters.result,
    actor: apiFilters.actor,
    school: apiFilters.school,
  };

  const enabled = !!filters.start && !!filters.end;

  const { data: overview, isLoading: overviewLoading } = useQuery({
    queryKey: ['audit-overview', trendParams],
    queryFn: () =>
      getAuditOverview(
        trendParams.start,
        trendParams.end,
        trendParams.search,
        trendParams.action,
        trendParams.entityType,
        trendParams.result,
        trendParams.actor,
        trendParams.school,
      ),
    enabled,
  });

  const { data: logsPage, isLoading: logsLoading } = useQuery({
    queryKey: ['audit-logs', apiFilters],
    queryFn: () => getAuditLogs(apiFilters),
    enabled,
  });

  const { data: trend, isLoading: trendLoading } = useQuery({
    queryKey: ['audit-trend', trendParams],
    queryFn: () =>
      getAuditTrend(
        trendParams.start,
        trendParams.end,
        trendParams.search,
        trendParams.action,
        trendParams.entityType,
        trendParams.result,
        trendParams.actor,
        trendParams.school,
      ),
    enabled,
  });

  const { data: actionDistribution, isLoading: actionLoading } = useQuery({
    queryKey: ['audit-actions', trendParams],
    queryFn: () =>
      getAuditActionDistribution(
        trendParams.start,
        trendParams.end,
        trendParams.search,
        trendParams.action,
        trendParams.entityType,
        trendParams.result,
        trendParams.actor,
        trendParams.school,
      ),
    enabled,
  });

  const { data: resultDistribution, isLoading: resultLoading } = useQuery({
    queryKey: ['audit-results', trendParams],
    queryFn: () =>
      getAuditResultDistribution(
        trendParams.start,
        trendParams.end,
        trendParams.search,
        trendParams.action,
        trendParams.entityType,
        trendParams.result,
        trendParams.actor,
        trendParams.school,
      ),
    enabled,
  });

  const loading = overviewLoading || logsLoading || trendLoading || actionLoading || resultLoading;

  const handlePreset = (days: number) => {
    const end = new Date();
    const start = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
    setFilters((f) => ({
      ...f,
      start: start.toISOString().slice(0, 10),
      end: end.toISOString().slice(0, 10),
    }));
  };

  const handleDateChange = (field: 'start' | 'end') => (e: ChangeEvent<HTMLInputElement>) => {
    setFilters((f) => ({ ...f, [field]: e.target.value, page: 0 }));
  };

  const handlePage = (page: number) => setFilters((f) => ({ ...f, page }));

  return (
    <div className="page space-y-6">
      <div className="page-header">
        <div>
          <h1 className="page-title">Audit Logs</h1>
          <p className="page-subtitle">
            Security and change monitoring across the EduManage platform. Records who did what, when, and with what result.
          </p>
        </div>
      </div>

      {loading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
          {[...Array(5)].map((_, i) => (
            <Card key={i} className="p-5">
              <Skeleton className="h-8 w-32 mb-3" />
              <Skeleton className="h-6 w-full" />
            </Card>
          ))}
        </div>
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            {statCard('Total events', overview?.totalEvents, FileText, 'info', 'Audit events in the selected period')}
            {statCard('Successful', overview?.successful, Activity, 'success', 'Completed operations')}
            {statCard('Failed', overview?.failed, ShieldAlert, 'destructive', 'Unsuccessful operations')}
            {statCard('Unique actors', overview?.uniqueActors, UsersIcon, 'default', 'Distinct authenticated actors')}
            {statCard('Security events', overview?.securityEvents, ShieldAlert, 'warning', 'Login, logout, password, access control')}
          </div>

          <SectionCard
            title="Filters"
            description="Filter by date range, action, entity, actor, school, result, and keyword search"
          >
            <div className="grid gap-3 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Date range preset</span>
                <div className="flex gap-1 flex-wrap">
                  {PRESETS.map((p) => (
                    <Button key={p.label} variant="outline" size="sm" onClick={() => handlePreset(p.days)}>
                      {p.label}
                    </Button>
                  ))}
                  <Button variant="outline" size="sm" onClick={() => handlePreset(90)}>
                    Last 90 days
                  </Button>
                </div>
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">From</span>
                <input type="date" className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm" value={filters.start} onChange={handleDateChange('start')} />
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">To</span>
                <input type="date" className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm" value={filters.end} onChange={handleDateChange('end')} />
                <p className="text-xs text-muted-foreground">End date is inclusive</p>
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Action</span>
                <Input value={filters.action} onChange={(e) => setFilters((f) => ({ ...f, action: e.target.value, page: 0 }))} placeholder="e.g. LOGIN" />
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Entity type</span>
                <Input value={filters.entityType} onChange={(e) => setFilters((f) => ({ ...f, entityType: e.target.value, page: 0 }))} placeholder="e.g. School" />
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Result</span>
                <Select
                  label="Result"
                  options={[
                    { value: 'SUCCESS', label: 'Successful' },
                    { value: 'FAILURE', label: 'Failed' },
                  ]}
                  value={filters.result}
                  onChange={(e) =>
                    setFilters((f) => ({ ...f, result: e.target.value, page: 0 }))
                  }
                />
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Actor (user id)</span>
                <Input value={filters.actor} onChange={(e) => setFilters((f) => ({ ...f, actor: e.target.value, page: 0 }))} placeholder="e.g. 42" />
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">School</span>
                <Input value={filters.school} onChange={(e) => setFilters((f) => ({ ...f, school: e.target.value, page: 0 }))} placeholder="School id" />
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">Search</span>
                <Input value={filters.search} onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value, page: 0 }))} placeholder="Action, entity, description…" />
              </div>
              <div className="flex flex-col gap-1">
                <span className="text-xs font-medium text-muted-foreground">&nbsp;</span>
                <Button>Apply</Button>
              </div>
            </div>
            <div className="mt-2 text-xs text-muted-foreground">
              Viewing <b>{filters.start}</b> to <b>{filters.end}</b> (inclusive). Older than 90 days is purged automatically.
            </div>
          </SectionCard>

          <div className="grid gap-6 lg:grid-cols-2">
            <SectionCard title="Activity trend" description="Audit events per day">
              <div className="h-72 w-full">
                <ActivityTrend data={trend ?? []} />
              </div>
            </SectionCard>
            <div className="lg:col-span-2">
              <div className="grid gap-6 md:grid-cols-2">
                <SectionCard title="Action distribution" description="Events by action type">
                  <div className="h-72 w-full"><ActionDistribution data={actionDistribution ?? []} /></div>
                </SectionCard>
                <SectionCard title="Result distribution" description="Successful vs failed">
                  <div className="h-72 w-full"><ResultDistribution data={resultDistribution ?? []} /></div>
                </SectionCard>
              </div>
            </div>
          </div>

          <SectionCard
            title="Audit trail"
            description="Detailed audit events. Click a row to inspect before/after changes, IP, and user agent."
          >
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Time</TableHead>
                    <TableHead>Actor</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Action</TableHead>
                    <TableHead>Entity</TableHead>
                    <TableHead>School</TableHead>
                    <TableHead className="text-center">Result</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead className="text-right">Details</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={9} className="h-48"><Skeleton className="h-full w-full" /></TableCell>
                    </TableRow>
                  ) : logsPage?.content.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={9} className="text-center py-8 text-muted-foreground">
                        No audit events match the selected filters.
                      </TableCell>
                    </TableRow>
                  ) : (
                    logsPage?.content.map((log: AuditLogResponse) => (
                      <TableRow
                        key={log.id}
                        onClick={() => setSelectedLog(log)}
                        className="cursor-pointer hover:bg-muted/50 transition"
                      >
                        <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                          {formatDateTime(log.createdAt)}
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm font-medium">
                          {log.actorName ?? (log.actorUserId != null ? `User #${log.actorUserId}` : 'System')}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">{log.actorRole ?? '—'}</TableCell>
                        <TableCell className="text-sm">{log.action}</TableCell>
                        <TableCell className="text-sm whitespace-nowrap">
                          {log.entityType ?? '—'} #{log.entityId ?? '—'}
                        </TableCell>
                        <TableCell className="text-sm whitespace-nowrap">
                          {log.schoolName ?? (log.schoolId != null ? `School #${log.schoolId}` : '—')}
                        </TableCell>
                        <TableCell className="text-sm text-center">
                          <Badge variant={resultVariant(log.result)}>{log.result ?? '—'}</Badge>
                        </TableCell>
                        <TableCell className="text-sm line-clamp-1">{log.description}</TableCell>
                        <TableCell className="text-right">
                          <Button variant="ghost" size="sm" onClick={(e) => { e.stopPropagation(); setSelectedLog(log); }}>
                            <FileText className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>

            {logsPage && (
              <div className="mt-3 flex items-center justify-between text-sm text-muted-foreground">
                <span>
                  Showing {logsPage?.content.length ?? 0} of {logsPage?.totalElements ?? 0} events
                </span>
                <div className="flex items-center gap-2">
                  <Button variant="outline" size="sm" onClick={() => handlePage(filters.page - 1)} disabled={filters.page === 0}>
                    Prev
                  </Button>
                  <Button variant="outline" size="sm" onClick={() => handlePage(filters.page + 1)} disabled={filters.page >= (logsPage?.totalPages ?? 1) - 1}>
                    Next
                  </Button>
                  <span>Page {filters.page + 1} of {Math.max(1, logsPage?.totalPages ?? 1)}</span>
                </div>
              </div>
            )}
          </SectionCard>

          {selectedLog && (
            <AuditDetailDialog log={selectedLog} onClose={() => setSelectedLog(null)} />
          )}
        </>
      )}
    </div>
  );
}

function ActivityTrend({ data }: { data: AuditChartPoint[] }) {
  const series = data.slice().sort((a, b) => a.period.localeCompare(b.period));
  if (series.length === 0) return <EmptyState title="No activity" description="No audit events in the selected period." />;
  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={series}>
        <XAxis dataKey="period" tick={{ fontSize: 10 }} />
        <YAxis tick={{ fontSize: 11 }} />
        <Tooltip />
        <Line type="monotone" dataKey="count" stroke="#3b82f6" strokeWidth={2} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  );
}

function ActionDistribution({ data }: { data: AuditDistributionItem[] }) {
  const series = data.slice().sort((a, b) => b.count - a.count).slice(0, 8);
  if (series.length === 0) return <EmptyState title="No data" />;
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={series}>
        <XAxis dataKey="name" tick={{ fontSize: 10, rotate: 30 }} />
        <YAxis tick={{ fontSize: 11 }} />
        <Tooltip />
        <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}

function ResultDistribution({ data }: { data: AuditDistributionItem[] }) {
  const series = data.filter((s) => s.count > 0);
  if (series.length === 0) return <EmptyState title="No data" />;
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie data={series} dataKey="count" nameKey="name" cx="50%" cy="50%" innerRadius="60%" outerRadius="80%" labelLine={false}>
          {series.map((entry) => (
            <Cell key={`cell-${entry.name}`} fill={entry.name === 'SUCCESS' ? '#22c55e' : '#ef4444'} />
          ))}
        </Pie>
        <Tooltip />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}

function AuditDetailDialog({ log, onClose }: { log: AuditLogResponse; onClose: () => void }) {
  const pretty = (value: unknown) => {
    if (value == null) return '—';
    if (typeof value === 'string') return value;
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[700px]">
        <div className="flex items-start justify-between gap-2">
          <h2 className="text-lg font-semibold text-foreground">Audit event details</h2>
          <p className="ml-2 text-xs text-muted-foreground">
            Event #{log.id} &bull; {log.action} &bull; {log.result ?? '—'}
          </p>
        </div>
        <div className="space-y-3 pb-4">
          <div className="grid gap-2 text-sm">
            <div className="font-semibold text-muted-foreground">Time (UTC)</div>
            <div className="whitespace-pre-wrap">{log.createdAt ?? '—'}</div>
            <div className="font-semibold text-muted-foreground">Actor</div>
            <div className="whitespace-pre-wrap">
              {log.actorName ?? (log.actorUserId != null ? `User #${log.actorUserId}` : 'System')}
              {log.actorRole ? ` (${log.actorRole})` : ''}
            </div>
            <div className="font-semibold text-muted-foreground">School</div>
            <div className="whitespace-pre-wrap">
              {log.schoolName ?? (log.schoolId != null ? `School #${log.schoolId}` : '—')}
            </div>
            <div className="font-semibold text-muted-foreground">Action</div>
            <div className="whitespace-pre-wrap">{log.action ?? '—'}</div>
            <div className="font-semibold text-muted-foreground">Entity</div>
            <div className="whitespace-pre-wrap">
              {log.entityType ?? '—'} #{log.entityId ?? '—'}
            </div>
            <div className="font-semibold text-muted-foreground">Result</div>
            <div className="whitespace-pre-wrap">{log.result ?? '—'}</div>
            <div className="font-semibold text-muted-foreground">IP address</div>
            <div className="whitespace-pre-wrap">{log.ipAddress ?? '—'}</div>
            <div className="font-semibold text-muted-foreground">User agent</div>
            <div className="whitespace-pre-wrap text-xs">{log.userAgent ?? '—'}</div>
          </div>

          <div className="border rounded-lg p-3 bg-muted/50 text-sm">
            <div className="font-semibold text-muted-foreground mb-1">Description</div>
            <div className="whitespace-pre-wrap break-words">{log.description ?? '—'}</div>
          </div>

          {log.beforeChanges && (
            <div className="border rounded-lg p-3 bg-muted/50 text-sm">
              <div className="font-semibold text-muted-foreground mb-1">Before changes</div>
              <div className="whitespace-pre-wrap break-words text-xs">{pretty(log.beforeChanges)}</div>
            </div>
          )}
          {log.afterChanges && (
            <div className="border rounded-lg p-3 bg-muted/50 text-sm">
              <div className="font-semibold text-muted-foreground mb-1">After changes</div>
              <div className="whitespace-pre-wrap break-words text-xs">{pretty(log.afterChanges)}</div>
            </div>
          )}
          {log.metadata && (
            <div className="border rounded-lg p-3 bg-muted/50 text-sm">
              <div className="font-semibold text-muted-foreground mb-1">Metadata</div>
              <div className="whitespace-pre-wrap break-words text-xs">{pretty(log.metadata)}</div>
            </div>
          )}
        </div>
        <div className="flex justify-end pt-3">
          <Button onClick={onClose}>Close</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
