import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  FileText,
  Users,
  Loader2,
  Send,
  RefreshCw,
} from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { SearchableSelect } from '@/components/ui/SearchableSelect';
import { Skeleton } from '@/components/ui/Skeleton';
import { Textarea } from '@/components/ui/Textarea';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import {
  getResultNotificationClasses,
  getResultNotificationDeliveries,
  getResultNotificationExaminations,
  getResultNotificationHistory,
  getResultNotificationStreams,
  getResultNotificationTemplate,
  previewResultNotificationRecipients,
  restoreResultNotificationTemplate,
  saveResultNotificationTemplate,
  sendResultNotification,
} from '@/services/resultsNotifications';
import type {
  ResultNotificationDeliveryDto,
  ResultNotificationDto,
} from '@/types/resultsNotifications';

const recipientOptions = [
  { value: 'STUDENTS', label: 'Students' },
  { value: 'PARENTS', label: 'Parents/Guardians' },
  { value: 'BOTH', label: 'Both students and parents/guardians' },
];

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
      return <Badge variant="warning">{status}</Badge>;
    case 'EXCLUDED':
      return <Badge variant="secondary">{status}</Badge>;
    default:
      return <Badge variant="secondary">{status}</Badge>;
  }
}

function maskPhone(phone: string | null | undefined): string | null {
  if (!phone) return null;
  const digits = phone.replace(/[^0-9]/g, '');
  if (digits.length <= 4) return '****';
  return '****' + digits.slice(digits.length - 4);
}

const DEFAULT_SMS_TEMPLATE =
  'Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}';

function renderTemplateSample(template: string | null | undefined): string {
  const ctx = {
    studentName: 'John Doe',
    admissionNumber: 'ADM001',
    className: 'Form 1',
    streamName: 'A',
    examinationName: 'GAUGING EXAMINATIONS',
    subjectResults: 'Maths 78/100 (B), English 65/100 (C)',
    schoolName: 'MJ School',
    recipientName: 'Jane Doe (Parent)',
  };
  let result = template ?? DEFAULT_SMS_TEMPLATE;
  const subjectResults = ctx.subjectResults || '(no results)';
  result = result.replace('{student_name}', ctx.studentName);
  result = result.replace('{admission_number}', ctx.admissionNumber);
  result = result.replace('{class_name}', ctx.className);
  result = result.replace('{stream_name}', ctx.streamName);
  result = result.replace('{examination_name}', ctx.examinationName);
  result = result.replace('{subject_results}', subjectResults);
  result = result.replace('{school_name}', ctx.schoolName);
  result = result.replace('{recipient_name}', ctx.recipientName);
  return result;
}

function estimateSmsSegments(text: string): number {
  if (!text) return 0;
  const isNonGsm = /[^\x00-\x7F]/.test(text);
  const singleLimit = isNonGsm ? 70 : 160;
  const multiLimit = isNonGsm ? 67 : 153;
  return text.length <= singleLimit ? 1 : Math.ceil(text.length / multiLimit);
}

function placeholderList(): string {
  return [
    '{student_name}',
    '{admission_number}',
    '{class_name}',
    '{stream_name}',
    '{examination_name}',
    '{subject_results}',
    '{school_name}',
    '{recipient_name}',
  ].join(', ');
}

interface DeliveryDetailsProps {
  notificationId: number;
}

function DeliveryDetails({ notificationId }: DeliveryDetailsProps) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['result-notification-deliveries', notificationId],
    queryFn: () => getResultNotificationDeliveries(notificationId),
    enabled: !!notificationId,
  });

  if (isLoading) return <p className="p-4 text-sm text-muted-foreground">Loading delivery details…</p>;
  if (error) return <p className="p-4 text-sm text-destructive">{errorMessage(error)}</p>;

  const deliveries = data ?? [];

  return (
    <div className="space-y-2 border-t border-border px-4 py-3">
      {deliveries.length ? deliveries.map((item: ResultNotificationDeliveryDto) => (
        <div key={item.id} className="flex flex-wrap items-center justify-between gap-2 text-sm p-2 rounded-lg border border-border/50">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-1.5 text-sm">
              <span className="font-medium truncate">{item.recipientName}</span>
              <span className="text-muted-foreground">· {item.recipientType}</span>
            </div>
            {item.studentName && (
              <div className="text-xs text-muted-foreground">
                {item.studentName} {item.admissionNumber ? `· ${item.admissionNumber}` : ''}
              </div>
            )}
            <div className="text-xs text-muted-foreground">
              {item.recipientPhone ? maskPhone(item.recipientPhone) : '—'}
              {item.batchId ? ` · batch ${item.batchId}` : ''}
              {item.providerMessageId ? ` · ref ${item.providerMessageId}` : ''}
            </div>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            <StatusBadge status={item.status} />
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

export function ManagerResultsNotificationsPage() {
  const queryClient = useQueryClient();
  const [examinationId, setExaminationId] = useState<number | null>(null);
  const [classId, setClassId] = useState<number | null>(null);
  const [streamId, setStreamId] = useState<number | null>(null);
  const [recipientType, setRecipientType] = useState<string>('STUDENTS');
  const [reviewOpen, setReviewOpen] = useState(false);
  const [expandedNotification, setExpandedNotification] = useState<number | null>(null);
  const [sendError, setSendError] = useState('');
  const [templateError, setTemplateError] = useState('');
  const [template, setTemplate] = useState('');

  const templateQuery = useQuery({
    queryKey: ['result-notification-template'],
    queryFn: getResultNotificationTemplate,
  });

  const saveTemplateMutation = useMutation({
    mutationFn: saveResultNotificationTemplate,
    onSuccess: (data) => {
      setTemplate(data.smsTemplate);
      setTemplateError('');
      setSendError('');
    },
    onError: (error: unknown) => setTemplateError(errorMessage(error)),
  });

  const restoreTemplateMutation = useMutation({
    mutationFn: restoreResultNotificationTemplate,
    onSuccess: (data) => {
      setTemplate(data.smsTemplate);
      setTemplateError('');
    },
    onError: (error: unknown) => setTemplateError(errorMessage(error)),
  });

  useEffect(() => {
    if (templateQuery.data && template === '') {
      setTemplate(templateQuery.data.smsTemplate);
    }
  }, [templateQuery.data, template]);

  const examinationsQuery = useQuery({
    queryKey: ['result-notification-examinations'],
    queryFn: getResultNotificationExaminations,
  });
  const classesQuery = useQuery({
    queryKey: ['result-notification-classes'],
    queryFn: getResultNotificationClasses,
  });
  const streamsQuery = useQuery({
    queryKey: ['result-notification-streams', classId],
    queryFn: () => getResultNotificationStreams(classId!),
    enabled: !!classId,
  });
  const previewQuery = useQuery({
    queryKey: ['result-notification-preview', examinationId, classId, streamId, recipientType],
    queryFn: () => previewResultNotificationRecipients(examinationId!, classId!, streamId, recipientType),
    enabled: !!examinationId && !!classId,
  });
  const historyQuery = useQuery({
    queryKey: ['result-notification-history'],
    queryFn: () => getResultNotificationHistory(),
  });

  const sendMutation = useMutation({
    mutationFn: sendResultNotification,
    onSuccess: () => {
      setReviewOpen(false);
      setSendError('');
      queryClient.invalidateQueries({ queryKey: ['result-notification-history'] });
    },
    onError: (error: unknown) => setSendError(errorMessage(error)),
  });

  const selectedExamination = examinationsQuery.data?.find((e: any) => e.id === examinationId);
  const selectedClass = classesQuery.data?.find((c: any) => c.id === classId);
  const selectedStream = streamsQuery.data?.find((s: any) => s.id === streamId);

  const handleReviewSend = () => {
    const preview = previewQuery.data;
    if (!preview || !preview.canSend) return;
    setReviewOpen(true);
  };

  const handleConfirmSend = () => {
    const preview = previewQuery.data;
    if (!preview || !preview.canSend) return;
    sendMutation.mutate({
      examinationId: preview.examinationId,
      classId: preview.classId,
      streamId: preview.streamId ?? 0,
      recipientType: preview.recipientType,
      idempotencyKey: crypto.randomUUID(),
    });
  };

  const notifications = historyQuery.data?.content ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Results Notifications</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Send examination results to students and parents through personalized bulk SMS.
        </p>
      </div>

      <Card className="max-w-6xl p-0 overflow-hidden">
        <div className="p-6 space-y-6">
          <div className="space-y-1">
            <label className="block text-sm font-semibold text-foreground">Examination</label>
            <SearchableSelect
              label="Examination"
              value={examinationId ? String(examinationId) : ''}
              options={examinationsQuery.isLoading
                ? []
                : ((examinationsQuery.data ?? []) as any[]).map((exam) => ({ value: String(exam.id), label: exam.name }))}
              placeholder="Select an examination"
              onChange={(event) => {
                setExaminationId(event.target.value ? Number(event.target.value) : null);
                setClassId(null);
                setStreamId(null);
              }}
              loading={examinationsQuery.isLoading}
              disabled={examinationsQuery.isLoading}
              emptyMessage="No examinations found"
            />
          </div>
          {examinationsQuery.isError && (
            <p role="alert" className="text-sm text-destructive">
              Failed to load examinations: {errorMessage(examinationsQuery.error)}
            </p>
          )}

          <div className="space-y-1">
            <label className="block text-sm font-semibold text-foreground">Class</label>
            <SearchableSelect
              label="Class"
              value={classId ? String(classId) : ''}
              options={classesQuery.isLoading
                ? []
                : ((classesQuery.data ?? []) as any[]).map((cls) => ({ value: String(cls.id), label: cls.name }))}
              placeholder="Select a class"
              onChange={(event) => {
                setClassId(event.target.value ? Number(event.target.value) : null);
                setStreamId(null);
              }}
              loading={classesQuery.isLoading}
              disabled={classesQuery.isLoading || !examinationId}
              emptyMessage="No classes found"
            />
          </div>
          {classesQuery.isError && (
            <p role="alert" className="text-sm text-destructive">
              Failed to load classes: {errorMessage(classesQuery.error)}
            </p>
          )}

          <div className="space-y-1">
            <label className="block text-sm font-semibold text-foreground">Stream</label>
            <SearchableSelect
              label="Stream"
              value={streamId ? String(streamId) : ''}
              options={streamsQuery.isLoading
                ? []
                : ((streamsQuery.data ?? []) as any[]).map((stream) => ({ value: String(stream.id), label: stream.name }))}
              placeholder="Select a stream"
              onChange={(event) => setStreamId(event.target.value ? Number(event.target.value) : null)}
              loading={streamsQuery.isLoading}
              disabled={streamsQuery.isLoading || !classId}
              emptyMessage="No streams found"
            />
          </div>
          {streamsQuery.isError && (
            <p role="alert" className="text-sm text-destructive">
              Failed to load streams: {errorMessage(streamsQuery.error)}
            </p>
          )}

          <div className="space-y-1">
            <label className="block text-sm font-semibold text-foreground">Recipients</label>
            <SearchableSelect
              label="Recipients"
              value={recipientType}
              options={recipientOptions}
              placeholder="Select recipients"
              onChange={(event) => setRecipientType(event.target.value)}
              emptyMessage="No options available"
            />
          </div>

          {previewQuery.isError ? (
            <p role="alert" className="text-sm text-destructive">
              Failed to compute recipients: {errorMessage(previewQuery.error)}
            </p>
          ) : previewQuery.isLoading ? (
            <p className="text-sm text-muted-foreground animate-pulse">Computing eligible recipients…</p>
          ) : previewQuery.data ? (
            <div className="rounded-xl border border-border/50 bg-muted/30 p-5">
              <div className="flex items-start gap-3 mb-3">
                <div className="shrink-0 rounded bg-primary p-1.5 text-primary-foreground">
                  <FileText className="h-4 w-4" />
                </div>
                <h2 className="text-sm font-semibold text-foreground">Results preview</h2>
              </div>
              <div className="flex items-center gap-4 text-sm mb-3">
                <span className="flex items-center gap-1.5">
                  <span className="rounded bg-emerald-500/10 p-1.5 text-emerald-700 dark:text-emerald-400">
                    <Users className="h-3 w-3" />
                  </span>
                  <span className="font-medium text-foreground">
                    {previewQuery.data.eligibleCount} eligible
                  </span>
                </span>
                {previewQuery.data.excludedCount > 0 && (
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
              {previewQuery.data.reason && (
                <p className="mt-1 text-sm text-destructive">{previewQuery.data.reason}</p>
              )}

              <div className="mt-4 overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[20%]">Student</TableHead>
                      <TableHead className="w-[14%]">Admission No.</TableHead>
                      <TableHead className="w-[10%]">Class / Stream</TableHead>
                      <TableHead className="w-[20%]">Subjects &amp; Marks</TableHead>
                      <TableHead className="w-[14%]">Recipient</TableHead>
                      <TableHead className="w-[16%]">Phone</TableHead>
                      <TableHead className="w-[12%]">Eligibility</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {previewQuery.data.students && previewQuery.data.students.length > 0 ? previewQuery.data.students.map((student: any) => (
                      <TableRow key={student.studentId}>
                        <TableCell className="font-medium text-sm">
                          {student.studentName}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">{student.admissionNumber}</TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {student.className}{student.streamName ? ` / ${student.streamName}` : ''}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {student.marksObtained !== null && student.maximumMarks !== null
                            ? `${student.marksObtained}/${student.maximumMarks}`
                            : 'N/A'}
                          {student.grade ? ` (${student.grade})` : ''}
                        </TableCell>
                        <TableCell className="text-sm">
                          {recipientType === 'STUDENTS' ? student.studentName : 'Parent/Guardian'}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {maskPhone(student.phone) || 'No phone'}
                        </TableCell>
                        <TableCell className="text-sm">
                          <StatusBadge status={student.eligible ? 'ELIGIBLE' : 'EXCLUDED'} />
                          {!student.eligible && student.exclusionReason && (
                            <div className="text-xs text-muted-foreground mt-1">{student.exclusionReason}</div>
                          )}
                        </TableCell>
                      </TableRow>
                    )) : (
                      <TableRow>
                        <TableCell colSpan={7} className="text-center text-muted-foreground py-4">
                          No students found for the selected examination, class and stream.
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </div>

                {previewQuery.data.parents && previewQuery.data.parents.length > 0 && (
                  <div className="mt-4">
                    <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                      Parent/Guardian Recipients
                    </h3>
                    <div className="overflow-x-auto">
                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead className="w-[25%]">Parent/Guardian</TableHead>
                            <TableHead className="w-[20%]">Student</TableHead>
                            <TableHead className="w-[20%]">Phone</TableHead>
                            <TableHead className="w-[20%]">Eligibility</TableHead>
                            <TableHead className="w-[15%]">Reason</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {previewQuery.data.parents.map((parent: any) => (
                          <TableRow key={parent.parentId}>
                            <TableCell className="font-medium text-sm">{parent.parentName}</TableCell>
                            <TableCell className="text-sm text-muted-foreground">
                              {parent.studentName} ({parent.admissionNumber})
                            </TableCell>
                            <TableCell className="text-sm text-muted-foreground">
                              {maskPhone(parent.phone) || 'No phone'}
                            </TableCell>
                            <TableCell className="text-sm">
                              <StatusBadge status={parent.eligible ? 'ELIGIBLE' : 'EXCLUDED'} />
                              {!parent.eligible && parent.exclusionReason && (
                                <div className="text-xs text-muted-foreground mt-1">{parent.exclusionReason}</div>
                              )}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </div>
              )}

              {previewQuery.data.sampleMessage && (
                <div className="mt-4 rounded-lg border border-input bg-input p-3 text-sm">
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                    Sample SMS
                  </p>
                  <p className="whitespace-pre-wrap break-words text-foreground">{previewQuery.data.sampleMessage}</p>
                </div>
              )}
            </div>
          ) : null}

          {sendError && <p role="alert" className="text-sm text-destructive">{sendError}</p>}
          {sendMutation.isSuccess && (
            <p role="status" className="flex items-center gap-2 text-sm text-emerald-600 dark:text-emerald-400">
              Results notification queued for sending.
            </p>
          )}
          <Button
            onClick={handleReviewSend}
            disabled={
              sendMutation.isPending ||
              !previewQuery.data?.canSend ||
              !examinationId ||
              !classId
            }
            className="gap-2 py-2.5"
          >
            <Send className="h-4 w-4" />
            {sendMutation.isPending ? 'Sending…' : 'Review and send'}
          </Button>
        </div>
      </Card>

      <Dialog open={reviewOpen} onOpenChange={setReviewOpen}>
        <DialogContent title="Confirm Results Notification" description="Review the details before sending personalized result SMS messages.">
          <div className="space-y-4 text-sm mt-4">
            <div className="flex justify-between items-start gap-4">
              <span className="text-muted-foreground">Examination</span>
              <span className="font-medium">{selectedExamination?.name ?? '—'}</span>
            </div>
            <div className="flex justify-between items-start gap-4">
              <span className="text-muted-foreground">Class</span>
              <span className="font-medium">{selectedClass?.name ?? '—'}</span>
            </div>
            {selectedStream && (
              <div className="flex justify-between items-start gap-4">
                <span className="text-muted-foreground">Stream</span>
                <span className="font-medium">{selectedStream.name}</span>
              </div>
            )}
            <div className="flex justify-between items-start gap-4">
              <span className="text-muted-foreground">Recipient type</span>
              <span className="font-medium">
                {recipientOptions.find((o) => o.value === recipientType)?.label ?? recipientType}
              </span>
            </div>
            {previewQuery.data && (
              <div className="flex justify-between items-start gap-4">
                <span className="text-muted-foreground">Eligible recipients</span>
                <span className="font-medium">{previewQuery.data.eligibleCount}</span>
              </div>
            )}
            {previewQuery.data?.excludedCount && previewQuery.data.excludedCount > 0 && (
              <div className="flex justify-between items-start gap-4">
                <span className="text-muted-foreground">Excluded recipients</span>
                <span className="text-muted-foreground">{previewQuery.data.excludedCount}</span>
              </div>
            )}
            {previewQuery.data?.sampleMessage && (
              <div className="flex justify-between items-start gap-4">
                <span className="text-muted-foreground">Sample SMS</span>
                <span className="text-muted-foreground break-words whitespace-pre-wrap max-w-xs line-clamp-2">
                  {previewQuery.data.sampleMessage}
                </span>
              </div>
            )}
          </div>
          {sendError && <p role="alert" className="text-sm text-destructive">{sendError}</p>}
          <div className="flex justify-end gap-2 mt-6">
            <Button type="button" variant="outline" disabled={sendMutation.isPending} onClick={() => setReviewOpen(false)}>
              Cancel
            </Button>
            <Button type="button" disabled={sendMutation.isPending || !previewQuery.data?.canSend} onClick={handleConfirmSend}>
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

      <Card>
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                SMS Template
              </h2>
              <p className="text-xs text-muted-foreground mt-1">
                Customize the SMS message sent for Results Notifications. This template is specific to your
                school and does not affect previously sent notifications.
              </p>
            </div>
            <Badge variant={templateQuery.data?.isDefault ? 'secondary' : 'outline'}>
              {templateQuery.data?.isDefault ? 'Using default template' : 'Customized'}
            </Badge>
          </div>

          {templateQuery.isError ? (
            <p role="alert" className="text-sm text-destructive mb-4">
              Failed to load template: {errorMessage(templateQuery.error)}
            </p>
          ) : (
            <div className="space-y-4">
              <Textarea
                data-testid="result-notification-template"
                label="Template"
                value={template}
                onChange={(event) => setTemplate(event.target.value)}
                placeholder={DEFAULT_SMS_TEMPLATE}
                disabled={templateQuery.isLoading}
              />
              {templateError && (
                <p role="alert" className="text-sm text-destructive">{templateError}</p>
              )}
              <div className="flex items-center gap-4 text-xs text-muted-foreground">
                <span>{template.length} characters</span>
                <span>
                  ~{estimateSmsSegments(template)} SMS segment{' '}
                  {template.length && estimateSmsSegments(template) !== 1 ? 's' : ''}
                </span>
                <span className="mx-1 text-muted-foreground">·</span>
                <span>Placeholders: {placeholderList()}</span>
              </div>
              <div className="rounded-lg border border-input bg-input p-3 text-sm mb-4">
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">
                  Personalized sample
                </p>
                <p className="whitespace-pre-wrap break-words text-foreground">
                  {renderTemplateSample(templateQuery.isLoading ? '' : template)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={saveTemplateMutation.isPending || restoreTemplateMutation.isPending}
                  onClick={() => restoreTemplateMutation.mutate()}
                >
                  <RefreshCw className="mr-2 h-4 w-4" /> Restore default
                </Button>
                <Button
                  disabled={
                    saveTemplateMutation.isPending ||
                    restoreTemplateMutation.isPending ||
                    template.trim() === ''
                  }
                  onClick={() => saveTemplateMutation.mutate({ smsTemplate: template })}
                >
                  {saveTemplateMutation.isPending ? 'Saving…' : 'Save Template'}
                </Button>
              </div>
            </div>
          )}
        </div>
      </Card>

      <Card>
        <div className="p-6">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4">
            Notification History
          </h2>
          {historyQuery.isError ? (
            <p role="alert" className="text-sm text-destructive">
              Failed to load notification history: {errorMessage(historyQuery.error)}
            </p>
          ) : historyQuery.isLoading ? (
            [...Array(3)].map((_, i) => (
              <div key={i} className="p-6"><Skeleton className="h-12 w-full" /></div>
            ))
          ) : notifications.length === 0 ? (
            <div className="p-12 text-center text-muted-foreground">
              <FileText className="mx-auto mb-3 h-10 w-10 opacity-50" />
              <p className="text-sm">No result notifications have been sent.</p>
            </div>
          ) : (
            <div className="divide-y">
              {notifications.map((notification: ResultNotificationDto) => (
                <div key={notification.id} className="p-4 last:border-0">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium text-sm text-foreground">{notification.title}</span>
                        <StatusBadge status={notification.status} />
                      </div>
                      <p className="mt-1 text-sm text-muted-foreground">
                        {notification.recipientType} · examination {notification.examinationId} · class {notification.classId}
                        {notification.streamId ? ` · stream ${notification.streamId}` : ''}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {notification.totalRecipients} total · {notification.successfulCount} sent ·
                        {notification.failedCount} failed · {notification.pendingCount} pending ·
                        {notification.excludedCount} excluded
                        {' · '}{new Date(notification.createdAt).toLocaleString()}
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          setExpandedNotification(expandedNotification === notification.id ? null : notification.id)
                        }
                      >
                        {expandedNotification === notification.id ? 'Hide details' : 'Delivery details'}
                      </Button>
                    </div>
                  </div>
                  {expandedNotification === notification.id && (
                    <div className="mt-4 pt-4 border-t border-border/50">
                      <DeliveryDetails notificationId={notification.id} />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
