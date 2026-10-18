import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Bell, CheckCheck, RefreshCw, Send } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { Textarea } from '@/components/ui/Textarea';
import {
  getCommunicationCampaigns,
  getCommunicationChannels,
  getCommunicationDeliveries,
  getNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  previewCommunicationRecipients,
  retryCommunicationCampaign,
  sendCommunicationCampaign,
} from '@/services/notifications';
import type {
  CommunicationCampaign,
  CommunicationChannel,
  CommunicationRecipientGroup,
  Notification,
} from '@/types/notifications';

const recipientOptions = [
  { value: 'STUDENTS', label: 'Students' },
  { value: 'PARENTS', label: 'Parents and guardians' },
  { value: 'TEACHERS', label: 'Teachers' },
  { value: 'STAFF', label: 'School staff' },
  { value: 'ALL_USERS', label: 'All eligible school recipients' },
];

const channelOptions = [
  { value: 'SMS', label: 'SMS' },
  { value: 'EMAIL', label: 'Email' },
  { value: 'BOTH', label: 'SMS and email' },
];

const categoryOptions = [
  { value: 'GENERAL', label: 'General information' },
  { value: 'ACADEMIC', label: 'Academic announcement' },
  { value: 'ADMISSIONS', label: 'Admissions' },
  { value: 'REGISTRATION', label: 'Registration' },
  { value: 'PARENT_ANNOUNCEMENT', label: 'Parent announcement' },
  { value: 'FEES', label: 'Fees and payments' },
  { value: 'SCHEDULE', label: 'Schedule changes' },
  { value: 'EVENT', label: 'Events and holidays' },
  { value: 'EMERGENCY', label: 'Emergency notice' },
];

const channelName = (channel: CommunicationChannel) =>
  channel === 'BOTH' ? ['SMS', 'EMAIL'] : [channel];

function errorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) return response.data.message;
  }
  return 'The request could not be completed. Please try again.';
}

function StatusBadge({ status }: { status: string }) {
  if (status === 'ACCEPTED') return <Badge variant="success">Accepted by provider</Badge>;
  if (status === 'PARTIAL') return <Badge variant="warning">Partial</Badge>;
  if (status === 'FAILED') return <Badge variant="destructive">Failed</Badge>;
  return <Badge variant="secondary">{status}</Badge>;
}

function DeliveryDetails({ campaign }: { campaign: CommunicationCampaign }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['communication-deliveries', campaign.id],
    queryFn: () => getCommunicationDeliveries(campaign.id),
  });
  if (isLoading) return <p className="p-3 text-sm text-muted-foreground">Loading delivery details…</p>;
  if (error) return <p className="p-3 text-sm text-destructive">{errorMessage(error)}</p>;
  return (
    <div className="space-y-2 border-t px-4 py-3">
      {data?.length ? data.map((item) => (
        <div key={item.id} className="flex flex-wrap items-center justify-between gap-2 text-sm">
          <span>{item.recipientType.toLowerCase()} #{item.recipientId} · {item.channel}</span>
          <span className="text-muted-foreground">
            {item.status}{item.failureCode ? ` · ${item.failureCode}` : ''}
          </span>
        </div>
      )) : <p className="text-sm text-muted-foreground">No delivery records available.</p>}
    </div>
  );
}

function Inbox({ items, loading, onRead, onReadAll }: {
  items: Notification[];
  loading: boolean;
  onRead: (id: number) => void;
  onReadAll: () => void;
}) {
  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button variant="secondary" onClick={onReadAll} className="gap-2">
          <CheckCheck className="h-4 w-4" /> Mark all read
        </Button>
      </div>
      <Card>
        <div className="divide-y">
          {loading ? [...Array(4)].map((_, index) => (
            <div key={index} className="p-4">
              <Skeleton className="mb-2 h-4 w-3/4" />
              <Skeleton className="h-3 w-1/2" />
            </div>
          )) : items.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground">
              <Bell className="mx-auto mb-2 h-12 w-12" />
              <p>No notifications yet.</p>
            </div>
          ) : items.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`w-full p-4 text-left hover:bg-accent ${!item.isRead ? 'bg-blue-50 dark:bg-blue-950/30' : ''}`}
              onClick={() => !item.isRead && onRead(item.id)}
            >
              <div className="flex items-center gap-2">
                <span className="font-medium">{item.title}</span>
                {!item.isRead && <Badge variant="warning">New</Badge>}
              </div>
              <p className="mt-1 text-sm text-muted-foreground">{item.message}</p>
              <time className="mt-1 block text-xs text-muted-foreground">
                {new Date(item.createdAt).toLocaleString()}
              </time>
            </button>
          ))}
        </div>
      </Card>
    </div>
  );
}

export function ManagerNotificationsPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'compose' | 'history' | 'inbox'>('compose');
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [category, setCategory] = useState('GENERAL');
  const [channel, setChannel] = useState<CommunicationChannel>('SMS');
  const [group, setGroup] = useState<CommunicationRecipientGroup>('STUDENTS');
  const [expandedCampaign, setExpandedCampaign] = useState<number | null>(null);
  const [sendError, setSendError] = useState('');

  const channelsQuery = useQuery({
    queryKey: ['communication-channels'],
    queryFn: getCommunicationChannels,
  });
  const previewQuery = useQuery({
    queryKey: ['communication-recipient-preview', group, channel],
    queryFn: () => previewCommunicationRecipients(group, channel),
  });
  const historyQuery = useQuery({
    queryKey: ['communication-campaigns'],
    queryFn: () => getCommunicationCampaigns(),
  });
  const inboxQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: () => getNotifications(),
  });

  const sendMutation = useMutation({
    mutationFn: sendCommunicationCampaign,
    onSuccess: () => {
      setTitle('');
      setMessage('');
      setSendError('');
      queryClient.invalidateQueries({ queryKey: ['communication-campaigns'] });
    },
    onError: (error) => setSendError(errorMessage(error)),
  });
  const retryMutation = useMutation({
    mutationFn: retryCommunicationCampaign,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['communication-campaigns'] }),
  });
  const markReadMutation = useMutation({
    mutationFn: markNotificationAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });
  const markAllReadMutation = useMutation({
    mutationFn: markAllNotificationsAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const enabledForSelection = useMemo(() => {
    const settings = channelsQuery.data;
    if (!settings?.notificationsEnabled) return false;
    return channelName(channel).every((item) =>
      item === 'SMS'
        ? settings.smsEnabled && settings.smsConfigured
        : settings.emailEnabled && settings.emailConfigured);
  }, [channel, channelsQuery.data]);

  const sendCampaign = () => {
    const preview = previewQuery.data;
    if (!preview || !preview.channelEnabled || preview.eligibleCount < 1) return;
    const confirmed = window.confirm(
      `Send ${channel} to ${preview.eligibleCount} eligible ${group.toLowerCase()}? ${preview.excludedCount} recipient(s) will be excluded because their contact details are missing or invalid.`,
    );
    if (!confirmed) return;
    sendMutation.mutate({
      title: title.trim(),
      message: message.trim(),
      category,
      channel,
      recipientGroup: group,
      idempotencyKey: crypto.randomUUID(),
    });
  };

  const campaigns = historyQuery.data?.content ?? [];
  const inboxItems = inboxQuery.data?.content ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Communication Center</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Send school announcements to recipients belonging to your assigned school.
        </p>
      </div>

      <div className="flex flex-wrap gap-2 border-b border-border">
        {(['compose', 'history', 'inbox'] as const).map((value) => (
          <button
            key={value}
            type="button"
            className={`border-b-2 px-4 py-2 text-sm font-medium capitalize ${
              tab === value ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground'
            }`}
            onClick={() => setTab(value)}
          >
            {value === 'compose' ? 'Compose' : value}
          </button>
        ))}
      </div>

      {tab === 'compose' && (
        <Card className="max-w-3xl p-6">
          <div className="space-y-4">
            <Input label="Notification title" value={title} maxLength={255}
              onChange={(event) => setTitle(event.target.value)} />
            <div className="grid gap-4 sm:grid-cols-3">
              <label className="space-y-1 text-sm font-medium">
                Category
                <Select value={category} onChange={(event) => setCategory(event.target.value)} options={categoryOptions} />
              </label>
              <label className="space-y-1 text-sm font-medium">
                Delivery channel
                <Select value={channel}
                  onChange={(event) => setChannel(event.target.value as CommunicationChannel)} options={channelOptions} />
              </label>
              <label className="space-y-1 text-sm font-medium">
                Recipient group
                <Select value={group}
                  onChange={(event) => setGroup(event.target.value as CommunicationRecipientGroup)} options={recipientOptions} />
              </label>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium" htmlFor="campaign-message">Message</label>
              <Textarea id="campaign-message" value={message} maxLength={1600} rows={6}
                onChange={(event) => setMessage(event.target.value)} />
              <p className="mt-1 text-xs text-muted-foreground">{message.length}/1600 characters</p>
            </div>
            <div className="rounded-md border border-border bg-muted/30 p-4">
              <h2 className="text-sm font-semibold">Preview and recipients</h2>
              <p className="mt-2 whitespace-pre-wrap text-sm">{message || 'Your message preview will appear here.'}</p>
              {previewQuery.isLoading ? <p className="mt-3 text-sm text-muted-foreground">Counting recipients…</p> : (
                <p className="mt-3 text-sm">
                  {previewQuery.data?.eligibleCount ?? 0} eligible · {previewQuery.data?.excludedCount ?? 0} excluded
                </p>
              )}
              {previewQuery.data?.disabledReason && (
                <p className="mt-1 text-sm text-destructive">{previewQuery.data.disabledReason}</p>
              )}
              {!enabledForSelection && !previewQuery.data?.disabledReason && (
                <p className="mt-1 text-sm text-destructive">The selected provider is disabled or not configured.</p>
              )}
            </div>
            {sendError && <p role="alert" className="text-sm text-destructive">{sendError}</p>}
            {sendMutation.isSuccess && (
              <p role="status" className="text-sm text-green-700 dark:text-green-400">
                Campaign processed. Provider acceptance is not confirmation of delivery.
              </p>
            )}
            <Button onClick={sendCampaign} disabled={
              !title.trim() || !message.trim() || message.length > 1600 ||
              sendMutation.isPending || !enabledForSelection ||
              !previewQuery.data?.channelEnabled || !previewQuery.data.eligibleCount
            } className="gap-2">
              <Send className="h-4 w-4" /> {sendMutation.isPending ? 'Sending…' : 'Review and send'}
            </Button>
          </div>
        </Card>
      )}

      {tab === 'history' && (
        <Card>
          <div className="divide-y">
            {historyQuery.isLoading ? [...Array(4)].map((_, index) => (
              <div key={index} className="p-4"><Skeleton className="h-12 w-full" /></div>
            )) : historyQuery.isError ? (
              <p className="p-6 text-sm text-destructive">{errorMessage(historyQuery.error)}</p>
            ) : campaigns.length === 0 ? (
              <p className="p-8 text-center text-sm text-muted-foreground">No campaigns have been sent.</p>
            ) : campaigns.map((campaign) => (
              <div key={campaign.id}>
                <div className="flex flex-wrap items-center justify-between gap-3 p-4">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-medium">{campaign.title}</span>
                      <StatusBadge status={campaign.status} />
                    </div>
                    <p className="mt-1 text-sm text-muted-foreground">
                      {campaign.category} · {campaign.recipientGroup.replace(/_/g, " ")} · {campaign.channel}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {campaign.intendedCount} intended · {campaign.acceptedCount} accepted · {campaign.failedCount} failed · {campaign.excludedCount} excluded
                      {' · '}{new Date(campaign.createdAt).toLocaleString()}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    {campaign.failedCount > 0 && (
                      <Button variant="secondary" disabled={retryMutation.isPending}
                        onClick={() => {
                          if (window.confirm('Retry only failed recipient deliveries?')) retryMutation.mutate(campaign.id);
                        }} className="gap-2">
                        <RefreshCw className="h-4 w-4" /> Retry failed
                      </Button>
                    )}
                    <Button variant="outline" onClick={() =>
                      setExpandedCampaign(expandedCampaign === campaign.id ? null : campaign.id)}>
                      {expandedCampaign === campaign.id ? 'Hide details' : 'Delivery details'}
                    </Button>
                  </div>
                </div>
                {expandedCampaign === campaign.id && <DeliveryDetails campaign={campaign} />}
              </div>
            ))}
          </div>
        </Card>
      )}

      {tab === 'inbox' && (
        <Inbox items={inboxItems} loading={inboxQuery.isLoading}
          onRead={(id) => markReadMutation.mutate(id)}
          onReadAll={() => markAllReadMutation.mutate()} />
      )}
    </div>
  );
}


