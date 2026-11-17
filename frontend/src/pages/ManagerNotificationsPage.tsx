import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Bell, CheckCheck, Loader2, RefreshCw, Send, Mail, Smartphone, Users, FileText } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Dialog, DialogContent, DialogClose } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { Textarea } from '@/components/ui/Textarea';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/Tabs';
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
  CommunicationDelivery,
  CommunicationRecipientGroup,
  Notification,
} from '@/types/notifications';

const recipientOptions = [
  { value: 'STUDENTS', label: 'Students', icon: Users },
  { value: 'PARENTS', label: 'Parents and guardians', icon: Users },
  { value: 'TEACHERS', label: 'Teachers', icon: Users },
  { value: 'STAFF', label: 'School staff', icon: Users },
  { value: 'ALL_USERS', label: 'All eligible school recipients', icon: Users },
];

const channelOptions = [
  { value: 'SMS', label: 'SMS', icon: Smartphone },
  { value: 'EMAIL', label: 'Email', icon: Mail },
  { value: 'BOTH', label: 'SMS and email', icon: Mail },
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
  switch (status) {
    case 'SENT':
    case 'DELIVERED':
      return <Badge variant="success">{status}</Badge>;
    case 'FAILED':
      return <Badge variant="destructive">{status}</Badge>;
    case 'PENDING':
    case 'PARTIALLY SENT':
      return <Badge variant="warning">{status}</Badge>;
    case 'EXCLUDED':
      return <Badge variant="secondary">{status}</Badge>;
    default:
      return <Badge variant="secondary">{status}</Badge>;
  }
}

function failureDescription(failureCode?: string | null): string | null {
  if (!failureCode) return null;
  const descriptions: Record<string, string> = {
    SMS_PROVIDER_UNAVAILABLE: 'The SMS provider could not be reached. The message was not delivered.',
    SMS_PROVIDER_CONNECTION_FAILED: 'Could not connect to the SMS provider.',
    SMS_PROVIDER_CONNECT_TIMEOUT: 'Connection to the SMS provider timed out.',
    SMS_PROVIDER_READ_TIMEOUT: 'The SMS provider did not respond in time.',
    SMS_PROVIDER_TLS_FAILURE: 'Secure connection to the SMS provider failed.',
    SMS_PROVIDER_SERVER_ERROR: 'The SMS provider returned a server error.',
    SMS_PROVIDER_AUTH_FAILED: 'SMS provider authentication failed.',
    SMS_PROVIDER_ACCOUNT_DISABLED: 'SMS provider account is disabled.',
    SMS_PROVIDER_REJECTED_RECIPIENT: 'The recipient was rejected by the SMS provider.',
    SMS_PROVIDER_RATE_LIMITED: 'SMS provider rate limit was exceeded.',
    SMS_PROVIDER_REJECTED: 'The SMS provider rejected the message.',
    SMS_PROVIDER_INVALID_RESPONSE: 'The SMS provider returned an invalid response.',
    SMS_NOT_CONFIGURED: 'SMS is not configured for this school.',
    INVALID_PHONE: 'The recipient phone number is invalid.',
    RECIPIENT_NO_LONGER_AVAILABLE: 'The recipient is no longer available.',
    SMS_MAX_RETRIES_EXCEEDED: 'Maximum SMS retry attempts exceeded.',
  };
  return descriptions[failureCode] ?? `Delivery failed: ${failureCode}`;
}

function DeliveryDetails({ campaign }: { campaign: CommunicationCampaign }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['communication-deliveries', campaign.id],
    queryFn: () => getCommunicationDeliveries(campaign.id),
  });
  if (isLoading) return <p className="p-4 text-sm text-muted-foreground">Loading delivery details…</p>;
  if (error) return <p className="p-4 text-sm text-destructive">{errorMessage(error)}</p>;

  function maskPhone(phone: string | null | undefined): string | null {
    if (!phone) return null;
    const digits = phone.replace(/[^0-9]/g, '');
    if (digits.length <= 4) return '****';
    return '****' + digits.slice(digits.length - 4);
  }

  function deliveryStatusLabel(item: CommunicationDelivery): string {
    if (item.deliveryStatus) return item.deliveryStatus;
    return item.status;
  }

  function statusVariant(status: string | null | undefined) {
    switch (status) {
      case 'SENT':
      case 'DELIVERED': return 'success';
      case 'FAILED': return 'destructive';
      case 'PENDING': return 'warning';
      case 'EXCLUDED': return 'secondary';
      default: return 'secondary';
    }
  }

  return (
    <div className="space-y-2 border-t border-border px-4 py-3">
      {data?.length ? data.map((item) => (
        <div key={item.id} className="flex flex-wrap items-center justify-between gap-2 text-sm p-2 rounded-lg border border-border/50">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-1.5 text-sm">
              <span className="font-medium truncate">{item.recipientName ?? item.recipientType + ' #' + item.recipientId}</span>
              <span className="text-muted-foreground">· {item.channel}</span>
            </div>
            <div className="text-xs text-muted-foreground">
              {item.recipientPhone ? maskPhone(item.recipientPhone) : '—'}
              {item.batchId ? ` · batch ${item.batchId}` : ''}
              {item.providerMessageId ? ` · ref ${item.providerMessageId}` : ''}
            </div>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            <Badge variant={statusVariant(item.status)}>
              {deliveryStatusLabel(item)}
            </Badge>
            {(item.status === 'FAILED' || item.status === 'PENDING') && item.failureCode && (
              <span className="text-xs text-muted-foreground truncate max-w-xs">
                {failureDescription(item.failureCode)}{item.errorMessage ? ` — ${item.errorMessage}` : ''}
              </span>
            )}
            {item.deliveredAt && (
              <span className="text-xs text-muted-foreground">Delivered {new Date(item.deliveredAt).toLocaleString()}</span>
            )}
            {item.sentAt && !item.deliveredAt && (
              <span className="text-xs text-muted-foreground">Sent {new Date(item.sentAt).toLocaleString()}</span>
            )}
          </div>
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
    <div className="space-y-3">
      <div className="flex justify-end">
        <Button variant="outline" size="sm" onClick={onReadAll} className="gap-2">
          <CheckCheck className="h-4 w-4" /> Mark all read
        </Button>
      </div>
      <Card className="divide-y">
        {loading ? [...Array(4)].map((_, index) => (
          <div key={index} className="p-4">
            <Skeleton className="mb-2 h-4 w-3/4" />
            <Skeleton className="h-3 w-1/2" />
          </div>
        )) : items.length === 0 ? (
          <div className="p-8 text-center text-muted-foreground">
            <Bell className="mx-auto mb-2 h-10 w-10 opacity-50" />
            <p className="text-sm">No notifications yet.</p>
          </div>
        ) : items.map((item) => (
          <button
            key={item.id}
            type="button"
            className={`w-full p-4 text-left transition-colors hover:bg-accent ${!item.isRead ? 'bg-blue-50/50 dark:bg-blue-950/20 border-l-2 border-l-primary' : ''} border-l-2 border-l-transparent`}
            onClick={() => !item.isRead && onRead(item.id)}
          >
            <div className="flex items-start gap-3">
              <div className="shrink-0 rounded-full bg-muted p-1.5 text-muted-foreground">
                <Bell className="h-4 w-4" aria-hidden="true" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-sm text-foreground">{item.title}</span>
                  {!item.isRead && <Badge variant="warning">New</Badge>}
                </div>
                <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{item.message}</p>
                <time className="mt-1 block text-xs text-muted-foreground">
                  {new Date(item.createdAt).toLocaleString()}
                </time>
              </div>
            </div>
          </button>
        ))}
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
  const [reviewOpen, setReviewOpen] = useState(false);

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
      setReviewOpen(false);
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

  const handleReviewSend = () => {
    const preview = previewQuery.data;
    if (!preview || !preview.channelEnabled || preview.eligibleCount < 1) return;
    setReviewOpen(true);
  };

  const handleConfirmSend = () => {
    const preview = previewQuery.data;
    if (!preview || !preview.channelEnabled || preview.eligibleCount < 1) return;
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
        <h1 className="text-2xl font-bold tracking-tight">Communication Center</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Send school announcements to recipients belonging to your assigned school.
        </p>
      </div>

      <Tabs value={tab} onValueChange={(value) => setTab(value as 'compose' | 'history' | 'inbox')} defaultValue="compose">
        <TabsList className="flex w-full flex-wrap gap-1">
          {(['compose', 'history', 'inbox'] as const).map((value) => (
            <TabsTrigger
              key={value}
              value={value}
              className="rounded-lg px-4 py-2.5 text-sm font-medium capitalize"
            >
              {value === 'compose' ? 'Compose' : value}
            </TabsTrigger>
          ))}
        </TabsList>

        <TabsContent value="compose" className="mt-4">
          <Card className="max-w-3xl p-0 overflow-hidden">
            <div className="p-6 space-y-6">
              <div className="space-y-2">
                <label className="block text-sm font-semibold text-foreground">Notification title</label>
                <Input
                  value={title}
                  maxLength={255}
                  placeholder="Enter notification title"
                  onChange={(event) => setTitle(event.target.value)}
                />
              </div>
              <div className="grid gap-4 sm:grid-cols-3">
                <div className="space-y-1">
                  <label className="block text-sm font-semibold text-foreground">Category</label>
                  <Select value={category} onChange={(event) => setCategory(event.target.value)} options={categoryOptions} />
                </div>
                <div className="space-y-1">
                  <label className="block text-sm font-semibold text-foreground">Delivery channel</label>
                  <Select value={channel} onChange={(event) => setChannel(event.target.value as CommunicationChannel)} options={channelOptions} />
                </div>
                <div className="space-y-1">
                  <label className="block text-sm font-semibold text-foreground">Recipient group</label>
                  <Select value={group} onChange={(event) => setGroup(event.target.value as CommunicationRecipientGroup)} options={recipientOptions} />
                </div>
              </div>
              <div className="space-y-1">
                <label className="block text-sm font-semibold text-foreground">Message</label>
                <Textarea id="campaign-message" value={message} maxLength={1600} rows={6}
                  placeholder="Type your message here…"
                  onChange={(event) => setMessage(event.target.value)} />
                <p className="mt-1 text-xs text-muted-foreground flex justify-between">
                  <span>{message.length}/1600 characters</span>
                  <span>{Math.max(0, 1600 - message.length)} remaining</span>
                </p>
              </div>
              <div className="rounded-xl border border-border/50 bg-muted/30 p-5">
                <div className="flex items-start gap-3 mb-3">
                  <div className="shrink-0 rounded bg-primary p-1.5 text-primary-foreground">
                    <FileText className="h-4 w-4" />
                  </div>
                  <h2 className="text-sm font-semibold text-foreground">Preview and recipients</h2>
                </div>
                <div className="rounded-lg border border-input bg-input p-3 text-sm">
                  <p className="whitespace-pre-wrap break-words text-foreground">{message || 'Your message preview will appear here.'}</p>
                </div>
                <div className="mt-4 space-y-1">
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">Recipient summary</p>
                  {previewQuery.isLoading ? (
                    <p className="text-sm text-muted-foreground animate-pulse">Counting recipients…</p>
                  ) : (
                    <div className="flex items-center gap-4 text-sm">
                      <span className="flex items-center gap-1.5">
                        <span className="rounded bg-emerald-500/10 p-1.5 text-emerald-700 dark:text-emerald-400">
                          <Users className="h-3 w-3" />
                        </span>
                        <span className="font-medium text-foreground">
                          {previewQuery.data?.eligibleCount ?? 0} eligible
                        </span>
                      </span>
                      {previewQuery.data?.excludedCount && previewQuery.data.excludedCount > 0 && (
                        <span className="flex items-center gap-1.5">
                          <span className="rounded bg-amber-500/10 p-1.5 text-amber-700 dark:text-amber-400">
                            <Users className="h-3 w-3 opacity-50" />
                          </span>
                          <span className="font-medium text-foreground">
                            {previewQuery.data.excludedCount} excluded
                          </span>
                        </span>
                      )}
                    </div>
                  )}
                  {previewQuery.data?.disabledReason && (
                    <p className="mt-1 text-sm text-destructive">{previewQuery.data.disabledReason}</p>
                  )}
                  {!enabledForSelection && !previewQuery.data?.disabledReason && (
                    <p className="mt-1 text-sm text-destructive">The selected provider is disabled or not configured.</p>
                  )}
                </div>
              </div>
              {sendError && <p role="alert" className="text-sm text-destructive">{sendError}</p>}
              {sendMutation.isSuccess && (
                <p role="status" className="flex items-center gap-2 text-sm text-emerald-600 dark:text-emerald-400">
                  Campaign processed. Provider acceptance is not confirmation of delivery.
                </p>
              )}
              <Button onClick={handleReviewSend} disabled={
                !title.trim() || !message.trim() || message.length > 1600 ||
                sendMutation.isPending || !enabledForSelection ||
                !previewQuery.data?.channelEnabled || !previewQuery.data.eligibleCount
              } className="gap-2 py-2.5">
                <Send className="h-4 w-4" /> {sendMutation.isPending ? 'Sending…' : 'Review and send'}
              </Button>
            </div>
          </Card>
        </TabsContent>

        <TabsContent value="history" className="mt-4">
          <Card className="p-0 overflow-hidden">
            <div className="divide-y">
              {historyQuery.isLoading ? [...Array(4)].map((_, index) => (
                <div key={index} className="p-6"><Skeleton className="h-12 w-full" /></div>
              )) : historyQuery.isError ? (
                <p className="p-8 text-sm text-destructive text-center">{errorMessage(historyQuery.error)}</p>
              ) : campaigns.length === 0 ? (
                <div className="p-12 text-center text-muted-foreground">
                  <FileText className="mx-auto mb-3 h-10 w-10 opacity-50" />
                  <p className="text-sm">No campaigns have been sent.</p>
                </div>
              ) : campaigns.map((campaign) => (
                <div key={campaign.id} className="p-4 last:border-0">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium text-sm text-foreground">{campaign.title}</span>
                        <StatusBadge status={campaign.status} />
                      </div>
                      <p className="mt-1 text-sm text-muted-foreground">
                        {campaign.category} · {campaign.recipientGroup.replace(/_/g, " ")} · {campaign.channel}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {campaign.intendedCount} intended · {campaign.sentCount} sent · {campaign.deliveredCount} delivered · {campaign.failedCount} failed · {campaign.excludedCount} excluded
                        {' · '}{new Date(campaign.createdAt).toLocaleString()}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      {campaign.failedCount > 0 && (
                        <Button variant="outline" size="sm" disabled={retryMutation.isPending}
                          onClick={() => {
                            if (window.confirm('Retry only failed recipient deliveries?')) retryMutation.mutate(campaign.id);
                          }} className="gap-2">
                          <RefreshCw className="h-4 w-4" /> Retry failed
                        </Button>
                      )}
                      <Button variant="outline" size="sm" onClick={() =>
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
        </TabsContent>

        <TabsContent value="inbox" className="mt-4">
          <Inbox items={inboxItems} loading={inboxQuery.isLoading}
            onRead={(id) => markReadMutation.mutate(id)}
            onReadAll={() => markAllReadMutation.mutate()} />
        </TabsContent>
      </Tabs>

      <Dialog open={reviewOpen} onOpenChange={setReviewOpen}>
        <DialogContent>
          <DialogClose onClick={() => setReviewOpen(false)} />
          <div className="space-y-4 text-sm">
            <div className="flex justify-between items-start gap-4">
              <span className="text-muted-foreground">Notification title</span>
              <span className="font-medium break-words">{title.trim() || '—'}</span>
            </div>
            <div className="flex justify-between items-start gap-4">
              <span className="text-muted-foreground">Message</span>
              <span className="text-muted-foreground break-words whitespace-pre-wrap">{message.trim() || '—'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Delivery channel</span>
              <span className="font-medium text-muted-foreground">{channel}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Recipient group</span>
              <span className="font-medium text-muted-foreground">
                {recipientOptions.find((option) => option.value === group)?.label ?? group}
              </span>
            </div>
            {previewQuery.data && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Eligible recipients</span>
                <span className="font-medium">{previewQuery.data.eligibleCount}</span>
              </div>
            )}
            {previewQuery.data?.excludedCount && previewQuery.data.excludedCount > 0 && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Excluded recipients</span>
                <span className="text-muted-foreground">{previewQuery.data.excludedCount}</span>
              </div>
            )}
          </div>
          {sendError && (
            <p role="alert" className="text-sm text-destructive">{sendError}</p>
          )}
          <div className="mt-6 flex justify-end gap-2">
            <Button type="button" variant="outline" disabled={sendMutation.isPending} onClick={() => setReviewOpen(false)}>
              Cancel
            </Button>
            <Button type="button" disabled={sendMutation.isPending} onClick={handleConfirmSend}>
              {sendMutation.isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Sending…
                </>
              ) : (
                'Confirm and send'
              )}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
