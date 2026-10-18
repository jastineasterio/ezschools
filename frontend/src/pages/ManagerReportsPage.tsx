import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getReports, createReport } from '@/services/reports';
import type { CreateReportJobRequest } from '@/types/reports';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus, Download } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const schema = z.object({
  reportType: z.string().min(1, 'Report type is required'),
  reportName: z.string().min(1, 'Report name is required'),
  parameters: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

const reportTypes = [
  { value: 'STUDENT_REPORT', label: 'Student Report' },
  { value: 'ATTENDANCE_REPORT', label: 'Attendance Report' },
  { value: 'EXAMINATION_REPORT', label: 'Examination Report' },
  { value: 'RESULT_REPORT', label: 'Result Report' },
  { value: 'FEE_REPORT', label: 'Fee Report' },
  { value: 'FINANCE_REPORT', label: 'Finance Report' },
  { value: 'SMS_REPORT', label: 'SMS Report' },
  { value: 'LIBRARY_REPORT', label: 'Library Report' },
  { value: 'BOARDING_REPORT', label: 'Boarding Report' },
  { value: 'TRANSPORT_REPORT', label: 'Transport Report' },
  { value: 'ANALYTICS_REPORT', label: 'Analytics Report' },
];

export function ManagerReportsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['reports'],
    queryFn: () => getReports(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateReportJobRequest) => createReport(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      setOpen(false);
    },
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      reportType: 'STUDENT_REPORT',
      reportName: '',
      parameters: '',
    },
  });

  const onSubmit = (values: FormValues) => {
    createMutation.mutate({
      reportType: values.reportType,
      reportName: values.reportName,
      parameters: values.parameters,
    });
  };

  const items = data?.content || [];

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'PROCESSING': return 'warning';
      case 'PENDING': return 'secondary';
      case 'FAILED': return 'destructive';
      default: return 'secondary';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Reports</h1>
        <Button onClick={() => setOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" /> Generate Report
        </Button>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-4">Report Name</th>
                <th className="text-left py-3 px-4">Type</th>
                <th className="text-left py-3 px-4">Status</th>
                <th className="text-left py-3 px-4">Started</th>
                <th className="text-left py-3 px-4">Completed</th>
                <th className="text-right py-3 px-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i} className="border-b">
                    {[...Array(6)].map((_, j) => (
                      <td key={j} className="py-3 px-4"><Skeleton className="h-4 w-24" /></td>
                    ))}
                  </tr>
                ))
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-muted-foreground">
                    No reports found. Generate one to get started.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4 font-medium">{item.reportName}</td>
                    <td className="py-3 px-4">{item.reportType}</td>
                    <td className="py-3 px-4">
                      <Badge variant={getStatusVariant(item.status)}>{item.status}</Badge>
                    </td>
                    <td className="py-3 px-4">{item.startedAt ? new Date(item.startedAt).toLocaleString() : '-'}</td>
                    <td className="py-3 px-4">{item.completedAt ? new Date(item.completedAt).toLocaleString() : '-'}</td>
                    <td className="py-3 px-4 text-right">
                      {item.status === 'COMPLETED' && item.fileUrl && (
                        <Button variant="ghost" size="sm" onClick={() => window.open(item.fileUrl as string, '_blank')}>
                          <Download className="h-4 w-4" /> Download
                        </Button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent title="Generate Report">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Report Type</label>
              <Select
                options={reportTypes}
                {...register('reportType')}
              />
              {errors.reportType && <p className="text-sm text-red-600 mt-1">{errors.reportType.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Report Name</label>
              <Input {...register('reportName')} />
              {errors.reportName && <p className="text-sm text-red-600 mt-1">{errors.reportName.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Parameters (JSON)</label>
              <Input {...register('parameters')} placeholder='{"startDate":"2024-01-01","endDate":"2024-01-31"}' />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending}>Generate</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

