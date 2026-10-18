import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getPayments, createPayment } from '@/services/fees';
import { getStudents } from '@/services/people';
import { getFeeAssignments } from '@/services/fees';
import type { CreatePaymentRequest } from '@/types/fees';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const schema = z.object({
  studentId: z.coerce.number().positive('Student is required'),
  feeAssignmentId: z.coerce.number().optional(),
  amount: z.coerce.number().positive('Amount must be positive'),
  paymentMethod: z.string().min(1, 'Payment method is required'),
  paymentDate: z.string().min(1, 'Payment date is required'),
  referenceNumber: z.string().optional(),
  receiptNumber: z.string().optional(),
  remarks: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export function ManagerPaymentsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['payments'],
    queryFn: () => getPayments(),
  });

  const { data: studentsData } = useQuery({
    queryKey: ['students'],
    queryFn: () => getStudents(0, 200),
  });

  const { data: assignments } = useQuery({
    queryKey: ['fee-assignments'],
    queryFn: () => getFeeAssignments(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreatePaymentRequest) => createPayment(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['fee-assignments'] });
      setOpen(false);
    },
  });

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      studentId: 0,
      amount: 0,
      paymentMethod: 'CASH',
      paymentDate: new Date().toISOString().split('T')[0],
    },
  });

  const selectedStudentId = watch('studentId');

  const studentAssignments = assignments?.filter(
    (a) => a.studentId === selectedStudentId && a.status !== 'PAID'
  ) || [];

  const onSubmit = (values: FormValues) => {
    createMutation.mutate({
      studentId: values.studentId,
      feeAssignmentId: values.feeAssignmentId || undefined,
      amount: values.amount,
      paymentMethod: values.paymentMethod,
      paymentDate: values.paymentDate,
      referenceNumber: values.referenceNumber,
      receiptNumber: values.receiptNumber,
      remarks: values.remarks,
    });
  };

  const items = data?.content || [];
  const studentOptions = studentsData?.content || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Payments</h1>
        <Button onClick={() => setOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" /> Record Payment
        </Button>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-4">Student</th>
                <th className="text-left py-3 px-4">Admission No</th>
                <th className="text-left py-3 px-4">Amount</th>
                <th className="text-left py-3 px-4">Method</th>
                <th className="text-left py-3 px-4">Date</th>
                <th className="text-left py-3 px-4">Receipt</th>
                <th className="text-left py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i} className="border-b">
                    {[...Array(7)].map((_, j) => (
                      <td key={j} className="py-3 px-4"><Skeleton className="h-4 w-24" /></td>
                    ))}
                  </tr>
                ))
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-muted-foreground">
                    No payments found.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4">{item.studentName}</td>
                    <td className="py-3 px-4">{item.admissionNumber}</td>
                    <td className="py-3 px-4">{item.amount.toLocaleString()}</td>
                    <td className="py-3 px-4">{item.paymentMethod}</td>
                    <td className="py-3 px-4">{item.paymentDate}</td>
                    <td className="py-3 px-4">{item.receiptNumber || '-'}</td>
                    <td className="py-3 px-4">
                      <Badge variant={item.status === 'COMPLETED' ? 'success' : 'secondary'}>
                        {item.status}
                      </Badge>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent title="Record Payment">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Student</label>
              <Select
                options={studentOptions.map((s) => ({ value: String(s.id), label: `${s.firstName} ${s.lastName} (${s.admissionNumber})` }))}
                {...register('studentId')}
              />
              {errors.studentId && <p className="text-sm text-red-600 mt-1">{errors.studentId.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Fee Assignment</label>
              <Select
                options={[{ value: '', label: 'None' }, ...studentAssignments.map((a) => ({ value: String(a.id), label: `${a.feeStructureName} - Balance: ${a.balance.toLocaleString()}` }))]}
                {...register('feeAssignmentId')}
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Amount</label>
              <Input type="number" step="0.01" {...register('amount')} />
              {errors.amount && <p className="text-sm text-red-600 mt-1">{errors.amount.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Payment Method</label>
              <Input {...register('paymentMethod')} />
              {errors.paymentMethod && <p className="text-sm text-red-600 mt-1">{errors.paymentMethod.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Payment Date</label>
              <Input type="date" {...register('paymentDate')} />
              {errors.paymentDate && <p className="text-sm text-red-600 mt-1">{errors.paymentDate.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Receipt Number</label>
              <Input {...register('receiptNumber')} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Remarks</label>
              <Input {...register('remarks')} />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending}>Record</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

