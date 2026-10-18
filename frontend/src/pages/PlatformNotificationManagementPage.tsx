import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { CheckCircle2, Mail, MessageSquare, ShieldAlert } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  getManagerChannelPermissions,
  getNotificationConfiguration,
  getPlatformCommunicationHistory,
  sendTestEmail,
  updateManagerChannelPermissions,
  updateNotificationConfiguration,
} from '@/services/notifications';
import type { CommunicationCampaign, NotificationChannels } from '@/types/notifications';

function requestError(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return 'The request could not be completed.';
}

function ProviderStatus({ configured, enabled }: { configured: boolean; enabled: boolean }) {
  if (!configured) return <Badge variant="destructive">Not configured</Badge>;
  return enabled
    ? <Badge variant="success">Enabled</Badge>
    : <Badge variant="secondary">Disabled</Badge>;
}

function CampaignHistory({ campaign }: { campaign: CommunicationCampaign }) {
  return (
    <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
      <span>School #{campaign.schoolId} · Sender #{campaign.senderUserId}</span>
      <span>{campaign.intendedCount} intended · {campaign.acceptedCount} accepted · {campaign.failedCount} failed</span>
    </div>
  );
}

export function PlatformNotificationManagementPage() {
  const queryClient = useQueryClient();
  const [testRecipient, setTestRecipient] = useState('');
  const configQuery = useQuery({
    queryKey: ['platform-notification-configuration'],
    queryFn: getNotificationConfiguration,
  });
  const managersQuery = useQuery({
    queryKey: ['platform-notification-managers'],
    queryFn: getManagerChannelPermissions,
  });
  const historyQuery = useQuery({
    queryKey: ['platform-notification-history'],
    queryFn: () => getPlatformCommunicationHistory(),
  });
  const configMutation = useMutation({
    mutationFn: updateNotificationConfiguration,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['platform-notification-configuration'] }),
  });
  const managerMutation = useMutation({
    mutationFn: ({ id, smsEnabled, emailEnabled }: { id: number; smsEnabled: boolean; emailEnabled: boolean }) =>
      updateManagerChannelPermissions(id, { smsEnabled, emailEnabled }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['platform-notification-managers'] }),
  });
  const testMutation = useMutation({ mutationFn: sendTestEmail });

  const config = configQuery.data;
  const updateFlag = (field: keyof Pick<NotificationChannels, 'notificationsEnabled' | 'smsEnabled' | 'emailEnabled'>,
    checked: boolean) => {
    if (!config) return;
    configMutation.mutate({ ...config, [field]: checked });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Notification Management</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Control global channel availability, school-manager access, provider readiness, and sending history.
        </p>
      </div>

      <Card className="space-y-5 p-6">
        <h2 className="text-lg font-semibold">Global channels</h2>
        {configQuery.isLoading ? <Skeleton className="h-32 w-full" /> : config ? (
          <div className="grid gap-4 md:grid-cols-3">
            {([
              ['notificationsEnabled', 'All notifications'],
              ['smsEnabled', 'SMS'],
              ['emailEnabled', 'Email'],
            ] as const).map(([field, label]) => (
              <label key={field} className="flex items-center justify-between gap-3 rounded-md border p-4">
                <span className="text-sm font-medium">{label}</span>
                <input type="checkbox" checked={config[field]}
                  disabled={configMutation.isPending}
                  onChange={(event) => updateFlag(field, event.target.checked)}
                  aria-label={`Enable ${label}`} />
              </label>
            ))}
          </div>
        ) : (
          <p className="text-sm text-destructive">{requestError(configQuery.error)}</p>
        )}
        {config && (
          <div className="flex flex-wrap gap-4 text-sm">
            <span className="flex items-center gap-2">
              <MessageSquare className="h-4 w-4" /> DARSMS <ProviderStatus configured={config.smsConfigured} enabled={config.smsEnabled} />
            </span>
            <span className="flex items-center gap-2">
              <Mail className="h-4 w-4" /> Gmail SMTP <ProviderStatus configured={config.emailConfigured} enabled={config.emailEnabled} />
            </span>
          </div>
        )}
        {configMutation.isError && <p role="alert" className="text-sm text-destructive">{requestError(configMutation.error)}</p>}
        <p className="text-xs text-muted-foreground">
          Provider credentials are read only from backend environment variables and are never returned by this screen.
        </p>
      </Card>

      <Card className="p-6">
        <h2 className="mb-4 text-lg font-semibold">Manager channel access</h2>
        {managersQuery.isLoading ? [...Array(3)].map((_, index) =>
          <Skeleton key={index} className="mb-3 h-12 w-full" />) : managersQuery.isError ? (
          <p className="text-sm text-destructive">{requestError(managersQuery.error)}</p>
        ) : managersQuery.data?.length ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="border-b text-left">
                <th className="py-2 pr-4">Manager</th><th className="py-2 pr-4">School</th>
                <th className="py-2 pr-4">SMS</th><th className="py-2">Email</th>
              </tr></thead>
              <tbody>
                {managersQuery.data.map((manager) => (
                  <tr key={manager.managerUserId} className="border-b">
                    <td className="py-3 pr-4">{manager.managerEmail}</td>
                    <td className="py-3 pr-4">#{manager.schoolId}</td>
                    <td className="py-3 pr-4">
                      <input type="checkbox" checked={manager.smsEnabled} disabled={managerMutation.isPending}
                        aria-label={`SMS access for ${manager.managerEmail}`}
                        onChange={(event) => managerMutation.mutate({
                          id: manager.managerUserId, smsEnabled: event.target.checked, emailEnabled: manager.emailEnabled,
                        })} />
                    </td>
                    <td className="py-3">
                      <input type="checkbox" checked={manager.emailEnabled} disabled={managerMutation.isPending}
                        aria-label={`Email access for ${manager.managerEmail}`}
                        onChange={(event) => managerMutation.mutate({
                          id: manager.managerUserId, smsEnabled: manager.smsEnabled, emailEnabled: event.target.checked,
                        })} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <p className="text-sm text-muted-foreground">No school managers found.</p>}
        {managerMutation.isError && <p role="alert" className="mt-3 text-sm text-destructive">{requestError(managerMutation.error)}</p>}
      </Card>

      <Card className="space-y-3 p-6">
        <h2 className="text-lg font-semibold">Test email</h2>
        <p className="text-sm text-muted-foreground">Sends a plain-text test message to the address you enter.</p>
        <div className="flex max-w-xl flex-col gap-3 sm:flex-row">
          <Input type="email" label="Test recipient" value={testRecipient}
            onChange={(event) => setTestRecipient(event.target.value)} />
          <Button className="self-end" disabled={!testRecipient.includes('@') || testMutation.isPending}
            onClick={() => {
              if (window.confirm(`Send a test email to ${testRecipient}?`)) testMutation.mutate(testRecipient);
            }}>
            {testMutation.isPending ? 'Sending…' : 'Send test'}
          </Button>
        </div>
        {testMutation.isSuccess && <p role="status" className="flex items-center gap-2 text-sm text-green-700"><CheckCircle2 className="h-4 w-4" /> Accepted by SMTP server.</p>}
        {testMutation.isError && <p role="alert" className="flex items-center gap-2 text-sm text-destructive"><ShieldAlert className="h-4 w-4" /> {requestError(testMutation.error)}</p>}
      </Card>

      <Card>
        <div className="border-b p-6"><h2 className="text-lg font-semibold">Campaign history and usage</h2></div>
        {historyQuery.isLoading ? [...Array(3)].map((_, index) =>
          <div className="p-4" key={index}><Skeleton className="h-12 w-full" /></div>) : historyQuery.isError ? (
          <p className="p-6 text-sm text-destructive">{requestError(historyQuery.error)}</p>
        ) : historyQuery.data?.content.length ? (
          <div className="divide-y">
            {historyQuery.data.content.map((campaign) => (
              <div key={campaign.id} className="space-y-2 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-medium">{campaign.title}</span>
                  <Badge variant={campaign.status === 'FAILED' ? 'destructive' : campaign.status === 'ACCEPTED' ? 'success' : 'secondary'}>
                    {campaign.status}
                  </Badge>
                </div>
                <CampaignHistory campaign={campaign} />
                <p className="text-xs text-muted-foreground">
                  {campaign.category} · {campaign.channel} · {new Date(campaign.createdAt).toLocaleString()}
                </p>
              </div>
            ))}
          </div>
        ) : <p className="p-6 text-sm text-muted-foreground">No notification campaigns yet.</p>}
      </Card>
    </div>
  );
}
