import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getSmsConfigurations, createSmsConfiguration, updateSmsConfiguration, deleteSmsConfiguration } from '@/services/sms';
import type { SmsConfiguration, CreateSmsConfigurationRequest } from '@/types/sms';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus, Pencil, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const schema = z.object({
  schoolId: z.coerce.number().min(1, 'School ID is required'),
  provider: z.string().min(1, 'Provider is required'),
  apiKey: z.string().min(1, 'API key is required'),
  apiSecret: z.string().min(1, 'API secret is required'),
  senderId: z.string().optional(),
  isActive: z.boolean().optional(),
  monthlyLimit: z.coerce.number().optional(),
  dailyLimit: z.coerce.number().optional(),
  remarks: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

const providerOptions = [
  { value: 'TWILIO', label: 'Twilio' },
  { value: 'AWS_SNS', label: 'AWS SNS' },
  { value: 'MSG91', label: 'MSG91' },
  { value: 'TEXTLOCAL', label: 'TextLocal' },
];

export function PlatformSmsConfigurationsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<SmsConfiguration | null>(null);

  const { data: configs, isLoading } = useQuery({
    queryKey: ['sms-configurations'],
    queryFn: () => getSmsConfigurations(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateSmsConfigurationRequest) => createSmsConfiguration(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sms-configurations'] });
      setOpen(false);
      setEditing(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CreateSmsConfigurationRequest }) =>
      updateSmsConfiguration(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sms-configurations'] });
      setOpen(false);
      setEditing(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteSmsConfiguration(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sms-configurations'] });
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      schoolId: 0,
      provider: 'TWILIO',
      apiKey: '',
      apiSecret: '',
      senderId: '',
      isActive: true,
      monthlyLimit: undefined,
      dailyLimit: undefined,
      remarks: '',
    },
  });

  const openCreate = () => {
    setEditing(null);
    reset({
      schoolId: 0,
      provider: 'TWILIO',
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
    reset({
      schoolId: item.schoolId,
      provider: item.provider,
      apiKey: '',
      apiSecret: '',
      senderId: item.senderId || '',
      isActive: item.isActive,
      monthlyLimit: item.monthlyLimit || undefined,
      dailyLimit: item.dailyLimit || undefined,
      remarks: item.remarks || '',
    });
    setOpen(true);
  };

  const onSubmit = (values: FormValues) => {
    const payload: CreateSmsConfigurationRequest = {
      schoolId: values.schoolId,
      provider: values.provider,
      apiKey: values.apiKey,
      apiSecret: values.apiSecret,
      senderId: values.senderId,
      isActive: values.isActive,
      monthlyLimit: values.monthlyLimit,
      dailyLimit: values.dailyLimit,
      remarks: values.remarks,
    };
    if (editing) {
      updateMutation.mutate({ id: editing.id, payload });
    } else {
      createMutation.mutate(payload);
    }
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
                <th className="text-left py-3 px-4">School ID</th>
                <th className="text-left py-3 px-4">Provider</th>
                <th className="text-left py-3 px-4">Sender ID</th>
                <th className="text-left py-3 px-4">Status</th>
                <th className="text-left py-3 px-4">Limits</th>
                <th className="text-right py-3 px-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i} className="border-b">
                    <td className="py-3 px-4"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-24" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-24" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-24" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-20 ml-auto" /></td>
                  </tr>
                ))
              ) : !configs?.content || configs.content.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-6 text-center text-muted-foreground">No SMS configurations found.</td>
                </tr>
              ) : (
                configs.content.map((config) => (
                  <tr key={config.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4 font-medium">{config.schoolId}</td>
                    <td className="py-3 px-4">{config.provider}</td>
                    <td className="py-3 px-4">{config.senderId || '-'}</td>
                    <td className="py-3 px-4">
                      <Badge variant={config.isActive ? 'success' : 'secondary'}>{config.isActive ? 'Active' : 'Inactive'}</Badge>
                    </td>
                    <td className="py-3 px-4">
                      {config.dailyLimit || config.monthlyLimit
                        ? `${config.dailyLimit || '-'}/day, ${config.monthlyLimit || '-'}/mo`
                        : '-'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Button variant="ghost" size="icon" onClick={() => openEdit(config)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => deleteMutation.mutate(config.id)}>
                        <Trash2 className="h-4 w-4 text-red-600" />
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
        <DialogContent title={editing ? 'Edit SMS Configuration' : 'Add SMS Configuration'}>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="max-h-[60vh] overflow-y-auto pr-1">
              <div>
                <label className="block text-sm font-medium mb-1">School ID</label>
                <Input type="number" {...register('schoolId')} />
                {errors.schoolId && <p className="text-sm text-red-600 mt-1">{errors.schoolId.message}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Provider</label>
                <Select
                  options={providerOptions}
                  {...register('provider')}
                />
                {errors.provider && <p className="text-sm text-red-600 mt-1">{errors.provider.message}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">API Key</label>
                <Input type="password" {...register('apiKey')} />
                {errors.apiKey && <p className="text-sm text-red-600 mt-1">{errors.apiKey.message}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">API Secret</label>
                <Input type="password" {...register('apiSecret')} />
                {errors.apiSecret && <p className="text-sm text-red-600 mt-1">{errors.apiSecret.message}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Sender ID</label>
                <Input {...register('senderId')} />
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
    </div>
  );
}

