import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { useState } from 'react';
import {
  getSmsConfigurations,
  createSmsConfiguration,
  updateSmsConfiguration,
  deleteSmsConfiguration,
  fetchSenderIds,
  testSmsConfiguration,
} from '@/services/sms';
import { api, getApiErrorMessage } from '@/services/api';
import type { SmsConfiguration, CreateSmsConfigurationRequest, UpdateSmsConfigurationRequest, SmsTestResult } from '@/types/sms';
import type { SchoolResponse } from '@/types/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus, Pencil, Trash2, Phone, RefreshCw } from 'lucide-react';
import { useForm, Controller } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const schema = z.object({
  schoolId: z.coerce.number().min(1, 'School is required'),
  provider: z.string().min(1, 'Provider is required'),
  apiKey: z.string().optional(),
  apiSecret: z.string().optional(),
  senderId: z.string().optional(),
  isActive: z.boolean().optional(),
  monthlyLimit: z.coerce.number().optional(),
  dailyLimit: z.coerce.number().optional(),
  remarks: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

const providerOptions = [
  { value: 'DARSMS', label: 'DARSMS' },
];

export function PlatformSmsConfigurationsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<SmsConfiguration | null>(null);
  const [testOpen, setTestOpen] = useState(false);
  const [testResult, setTestResult] = useState<SmsTestResult | null>(null);
  const [testError, setTestError] = useState<string | null>(null);
  const [testingId, setTestingId] = useState<number | null>(null);

  const [senderIdOptions, setSenderIdOptions] = useState<string[]>([]);
  const [fetchingSenderIds, setFetchingSenderIds] = useState(false);
  const [senderIdFetchError, setSenderIdFetchError] = useState<string | null>(null);

  const { data: configs, isLoading } = useQuery({
    queryKey: ['sms-configurations'],
    queryFn: () => getSmsConfigurations(),
  });

  const { data: schoolsData, isLoading: schoolsLoading } = useQuery({
    queryKey: ['schools-all'],
    queryFn: () => api.get('/platform/schools?page=0&size=1000').then(r => r.data.data),
  });

  const schools = (schoolsData?.content || []) as SchoolResponse[];

  const createMutation = useMutation({
    mutationFn: (payload: CreateSmsConfigurationRequest) => createSmsConfiguration(payload),
    onSuccess: () => {
      setApiError(null);
      queryClient.invalidateQueries({ queryKey: ['sms-configurations'] });
      setOpen(false);
      setEditing(null);
    },
    onError: (error: any) => {
      setApiError(getApiErrorMessage(error, 'Failed to create SMS configuration'));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateSmsConfigurationRequest }) =>
      updateSmsConfiguration(id, payload),
    onSuccess: () => {
      setApiError(null);
      queryClient.invalidateQueries({ queryKey: ['sms-configurations'] });
      setOpen(false);
      setEditing(null);
    },
    onError: (error: any) => {
      setApiError(getApiErrorMessage(error, 'Failed to update SMS configuration'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteSmsConfiguration(id),
    onSuccess: () => {
      setApiError(null);
      queryClient.invalidateQueries({ queryKey: ['sms-configurations'] });
    },
    onError: (error: any) => {
      setApiError(getApiErrorMessage(error, 'Failed to delete SMS configuration'));
    },
  });

  const testMutation = useMutation({
    mutationFn: ({ id, recipient }: { id: number; recipient?: string }) =>
      testSmsConfiguration(id, recipient),
    onSuccess: (data) => {
      setTestResult(data.data);
      setTestError(null);
      setTestOpen(true);
    },
    onError: (error: any) => {
      setTestError(getApiErrorMessage(error, 'Test connection failed'));
      setTestResult(null);
      setTestOpen(true);
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    control,
    watch,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      schoolId: 0,
      provider: 'DARSMS',
      apiKey: '',
      apiSecret: '',
      senderId: '',
      isActive: true,
      monthlyLimit: undefined,
      dailyLimit: undefined,
      remarks: '',
    },
  });

  const [apiError, setApiError] = useState<string | null>(null);

  const openCreate = () => {
    setEditing(null);
    setSenderIdOptions([]);
    setSenderIdFetchError(null);
    reset({
      schoolId: 0,
      provider: 'DARSMS',
      apiKey: '',
      apiSecret: '',
      senderId: '',
      isActive: true,
      monthlyLimit: undefined,
      dailyLimit: undefined,
      remarks: '',
    });
    setOpen(true);
  };

  const openEdit = (item: SmsConfiguration) => {
    setEditing(item);
    setSenderIdOptions([]);
    setSenderIdFetchError(null);
    reset({
      schoolId: item.schoolId,
      provider: item.provider,
      apiKey: '',
      apiSecret: '',
      senderId: item.senderId || '',
      isActive: item.active,
      monthlyLimit: item.monthlyLimit || undefined,
      dailyLimit: item.dailyLimit || undefined,
      remarks: item.remarks || '',
    });
    setOpen(true);
  };

  const handleFetchSenderIds = (apiKey: string) => {
    if (!apiKey) {
      setSenderIdFetchError('API key is required to fetch sender IDs');
      return;
    }
    setFetchingSenderIds(true);
    setSenderIdFetchError(null);
    fetchSenderIds(apiKey)
      .then((ids) => {
        if (!Array.isArray(ids)) {
          setSenderIdFetchError('Unexpected response from DARSMS.');
          return;
        }
        setSenderIdOptions(ids);
        if (ids.length === 0) {
          setSenderIdFetchError(null);
        }
      })
      .catch((error) => {
        if (axios.isAxiosError(error)) {
          const status = error.response?.status ?? null;
          if (status === 401) {
            setSenderIdFetchError('Invalid DARSMS API key. Please check your API key.');
          } else if (status === 403) {
            setSenderIdFetchError('DARSMS account is inactive or unauthorized.');
          } else if (status === 429) {
            setSenderIdFetchError('DARSMS rate limit exceeded. Try again later.');
          } else if (status === 502) {
            setSenderIdFetchError('DARSMS is temporarily unreachable. Try again later.');
          } else if (status === null) {
            setSenderIdFetchError('Network connection failed. Check your internet connection.');
          } else {
            setSenderIdFetchError(getApiErrorMessage(error, 'Unable to fetch sender IDs from DARSMS.'));
          }
        } else {
          setSenderIdFetchError('Unexpected response from DARSMS.');
        }
      })
      .finally(() => setFetchingSenderIds(false));
  };

  const onSubmit = (values: FormValues) => {
    setApiError(null);
    setSenderIdFetchError(null);
    if (!editing) {
      if (!values.apiKey) {
        setError('apiKey', { type: 'manual', message: 'API key is required' });
      }
      if (!values.apiKey) return;
    }
    const basePayload: UpdateSmsConfigurationRequest = {
      provider: values.provider,
      apiKey: values.apiKey || '',
      apiSecret: values.apiSecret || undefined,
      senderId: values.senderId || undefined,
      isActive: values.isActive,
      monthlyLimit: values.monthlyLimit,
      dailyLimit: values.dailyLimit,
      remarks: values.remarks,
    };
    if (editing) {
      updateMutation.mutate({ id: editing.id, payload: basePayload });
    } else {
      createMutation.mutate({ ...basePayload, schoolId: values.schoolId });
    }
  };

  const handleTestConnection = (id: number) => {
    setTestingId(id);
    setTestResult(null);
    setTestError(null);
    testMutation.mutate({ id });
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">SMS Configurations</h1>
        <Button onClick={openCreate} className="gap-2">
          <Plus className="h-4 w-4" /> Add Configuration
        </Button>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-4">School Name</th>
                <th className="text-left py-3 px-4">Provider</th>
                <th className="text-left py-3 px-4">Sender ID</th>
                <th className="text-left py-3 px-4">Status</th>
                <th className="text-left py-3 px-4">Monthly Limit</th>
                <th className="text-left py-3 px-4">Daily Limit</th>
                <th className="text-right py-3 px-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i} className="border-b">
                    <td className="py-3 px-4"><Skeleton className="h-4 w-32" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-20" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-20" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-20 ml-auto" /></td>
                  </tr>
                ))
              ) : !configs?.content || configs.content.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-6 text-center text-muted-foreground">No SMS configurations found.</td>
                </tr>
              ) : (
                configs.content.map((config) => (
                  <tr key={config.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4 font-medium">{config.schoolName || config.schoolId}</td>
                    <td className="py-3 px-4">{config.provider}</td>
                    <td className="py-3 px-4">{config.senderId || '-'}</td>
                    <td className="py-3 px-4">
                      <Badge variant={config.active ? 'success' : 'secondary'}>
                        {config.active ? 'Active' : 'Inactive'}
                      </Badge>{' '}
                      {config.credentialsConfigured ? (
                        <span className="text-xs text-muted-foreground">Configured</span>
                      ) : (
                        <span className="text-xs text-muted-foreground">Not configured</span>
                      )}
                    </td>
                    <td className="py-3 px-4">{config.monthlyLimit ?? '-'}</td>
                    <td className="py-3 px-4">{config.dailyLimit ?? '-'}</td>
                    <td className="py-3 px-4 text-right">
                      <Button variant="ghost" size="icon" onClick={() => openEdit(config)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleTestConnection(config.id)}
                        disabled={config.id === testingId}
                        title="Test connection"
                      >
                        {config.id === testingId ? (
                          <RefreshCw className="h-4 w-4 animate-spin" />
                        ) : (
                          <Phone className="h-4 w-4" />
                        )}
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => {
                        if (window.confirm('Delete this SMS configuration?')) {
                          deleteMutation.mutate(config.id);
                        }
                      }}>
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent title={editing ? 'Edit SMS Configuration' : 'Add SMS Configuration'} description="Configure DARSMS SMS delivery for a school. API Key is required; API Secret is optional.">
          {apiError && (
            <Alert variant="destructive" className="mb-4">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{apiError}</AlertDescription>
            </Alert>
          )}
          {senderIdFetchError && (
            <Alert variant="destructive" className="mb-4">
              <AlertTitle>Sender IDs</AlertTitle>
              <AlertDescription>{senderIdFetchError}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="max-h-[60vh] overflow-y-auto pr-1">
              <div>
                <label className="block text-sm font-medium mb-1">School Name</label>
                <Controller
                  name="schoolId"
                  control={control}
                  render={({ field }) => (
                    <Select
                      options={schools.map((s) => ({ value: String(s.id), label: s.name }))}
                      value={String(field.value || '')}
                      onChange={(e) => field.onChange(Number(e.target.value))}
                      disabled={schoolsLoading}
                    />
                  )}
                />
                {schoolsLoading && <p className="text-sm text-muted-foreground mt-1">Loading schools...</p>}
                {!schoolsLoading && schools.length === 0 && (
                  <p className="text-sm text-destructive mt-1">No schools available.</p>
                )}
                {errors.schoolId && <p className="text-sm text-destructive mt-1">{errors.schoolId.message}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Provider</label>
                <Select options={providerOptions} {...register('provider')} />
                {errors.provider && <p className="text-sm text-destructive mt-1">{errors.provider.message}</p>}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">API Key</label>
                  <Input type="password" {...register('apiKey')} />
                  <Button
                    type="button"
                    size="sm"
                    onClick={() => handleFetchSenderIds(watch('apiKey') || '')}
                    disabled={fetchingSenderIds}
                    className="mt-2"
                  >
                    {fetchingSenderIds ? <RefreshCw className="h-3 w-3 animate-spin" /> : 'Fetch approved Sender IDs'}
                  </Button>
                  {errors.apiKey && <p className="text-sm text-destructive mt-1">{errors.apiKey.message}</p>}
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">API Secret</label>
                  <Input type="password" {...register('apiSecret')} />
                  <p className="text-xs text-muted-foreground">Optional for DARSMS</p>
                  {errors.apiSecret && <p className="text-sm text-destructive mt-1">{errors.apiSecret.message}</p>}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Sender ID</label>
                <Controller
                  name="senderId"
                  control={control}
                  render={({ field }) => (
                    <Select
                      options={senderIdOptions.map((s: string) => ({ value: s, label: s }))}
                      value={field.value || ''}
                      onChange={(e: React.ChangeEvent<HTMLSelectElement>) => field.onChange(e.target.value)}
                      disabled={senderIdOptions.length === 0}
                    />
                  )}
                />
                {senderIdOptions.length > 0 && (
                  <p className="text-xs text-muted-foreground mt-1">
                    Approved sender IDs fetched from DARSMS. Select one to use with this configuration.
                  </p>
                )}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Monthly Limit</label>
                  <Input type="number" {...register('monthlyLimit')} />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Daily Limit</label>
                  <Input type="number" {...register('dailyLimit')} />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Remarks</label>
                <Input {...register('remarks')} />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-4 border-t">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {editing ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={testOpen} onOpenChange={setTestOpen}>
        <DialogContent title="Test Connection">
          {testError && (
            <Alert variant="destructive" className="mb-4">
              <AlertTitle>Test failed</AlertTitle>
              <AlertDescription>{testError}</AlertDescription>
            </Alert>
          )}
          {testResult && (
            <>
              <Alert variant={testResult.success ? 'default' : 'destructive'} className="mb-4">
                <AlertTitle>{testResult.success ? 'Connection successful' : 'Connection failed'}</AlertTitle>
                <AlertDescription>{testResult.failureCode ? `Provider: ${testResult.failureCode}` : 'DARSMS is reachable.'}</AlertDescription>
              </Alert>
              {testResult.balance && (
                <div className="mb-4 p-3 rounded bg-muted border border-border text-sm">
                  <p className="font-medium mb-1">Provider balance</p>
                  <pre className="text-sm overflow-x-auto">{JSON.stringify(testResult.balance, null, 2)}</pre>
                </div>
              )}
              {(testResult.senderIds ?? []).length > 0 && (
                <div className="mb-4 p-3 rounded bg-muted border border-border text-sm">
                  <p className="font-medium mb-1">Approved sender IDs</p>
                  <div className="flex flex-wrap gap-2">
                    {(testResult.senderIds ?? []).map((sid: string) => (
                      <span key={sid} className="px-2 py-1 rounded border border-input text-xs">{sid}</span>
                    ))}
                  </div>
                </div>
              )}
              {!testResult.success && !testResult.failureCode && (
                <p className="text-sm text-muted-foreground">Unable to determine connection status.</p>
              )}
            </>
          )}
          <div className="flex justify-end pt-4 border-t">
            <Button onClick={() => setTestOpen(false)}>Close</Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
