import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFeeStructures, getFeeAssignments, assignFee, bulkAssignFee } from '@/services/fees';
import { getStudents } from '@/services/people';
import type { CreateFeeAssignmentRequest } from '@/types/fees';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus, Upload } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const schema = z.object({
  studentId: z.coerce.number().positive('Student is required'),
  feeStructureId: z.coerce.number().positive('Fee structure is required'),
  dueDate: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export function ManagerFeeAssignmentsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [bulkOpen, setBulkOpen] = useState(false);

  const { data: assignments, isLoading: assignmentsLoading } = useQuery({
    queryKey: ['fee-assignments'],
    queryFn: () => getFeeAssignments(),
  });

  const { data: feeStructures } = useQuery({
    queryKey: ['fee-structures'],
    queryFn: () => getFeeStructures(),
  });

  const { data: studentsData } = useQuery({
    queryKey: ['students'],
    queryFn: () => getStudents(0, 200),
  });

  const assignMutation = useMutation({
    mutationFn: (payload: CreateFeeAssignmentRequest) => assignFee(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-assignments'] });
      setOpen(false);
    },
  });

  const bulkMutation = useMutation({
    mutationFn: ({ feeStructureId, studentIds }: { feeStructureId: number; studentIds: number[] }) =>
      bulkAssignFee(feeStructureId, studentIds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-assignments'] });
      setBulkOpen(false);
    },
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  const [bulkStructureId, setBulkStructureId] = useState('');
  const [bulkStudentIds, setBulkStudentIds] = useState('');

  const onSubmit = (values: FormValues) => {
    assignMutation.mutate({
      studentId: values.studentId,
      feeStructureId: values.feeStructureId,
      dueDate: values.dueDate || undefined,
    });
  };

  const handleBulkAssign = () => {
    const ids = bulkStudentIds.split(',').map((s) => Number(s.trim())).filter((n) => !isNaN(n));
    if (!bulkStructureId || ids.length === 0) return;
    bulkMutation.mutate({ feeStructureId: Number(bulkStructureId), studentIds: ids });
  };

  const items = assignments || [];
  const studentOptions = studentsData?.content || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Fee Assignments</h1>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => setBulkOpen(true)} className="gap-2">
            <Upload className="h-4 w-4" /> Bulk Assign
          </Button>
          <Button onClick={() => setOpen(true)} className="gap-2">
            <Plus className="h-4 w-4" /> Assign Fee
          </Button>
        </div>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-4">Student</th>
                <th className="text-left py-3 px-4">Admission No</th>
                <th className="text-left py-3 px-4">Fee</th>
                <th className="text-left py-3 px-4">Due</th>
                <th className="text-left py-3 px-4">Paid</th>
                <th className="text-left py-3 px-4">Balance</th>
                <th className="text-left py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody>
              {assignmentsLoading ? (
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
                    No fee assignments found.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4">{item.studentName}</td>
                    <td className="py-3 px-4">{item.admissionNumber}</td>
                    <td className="py-3 px-4">{item.feeStructureName}</td>
                    <td className="py-3 px-4">{item.amountDue.toLocaleString()}</td>
                    <td className="py-3 px-4">{item.amountPaid.toLocaleString()}</td>
                    <td className="py-3 px-4 font-medium">{item.balance.toLocaleString()}</td>
                    <td className="py-3 px-4">
                      <Badge variant={item.status === 'PAID' ? 'success' : item.status === 'PENDING' ? 'warning' : 'secondary'}>
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
        <DialogContent title="Assign Fee">
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
              <label className="block text-sm font-medium mb-1">Fee Structure</label>
              <Select
                options={(feeStructures?.content || []).map((f) => ({ value: String(f.id), label: `${f.name} - ${f.amount.toLocaleString()}` }))}
                {...register('feeStructureId')}
              />
              {errors.feeStructureId && <p className="text-sm text-red-600 mt-1">{errors.feeStructureId.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Due Date</label>
              <Input type="date" {...register('dueDate')} />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={assignMutation.isPending}>Assign</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={bulkOpen} onOpenChange={setBulkOpen}>
        <DialogContent title="Bulk Assign Fee">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Fee Structure</label>
              <Select
                value={bulkStructureId}
                onChange={(e) => setBulkStructureId(e.target.value)}
                options={(feeStructures?.content || []).map((f) => ({ value: String(f.id), label: `${f.name} - ${f.amount.toLocaleString()}` }))}
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Student IDs (comma separated)</label>
              <Input value={bulkStudentIds} onChange={(e) => setBulkStudentIds(e.target.value)} placeholder="1, 2, 3" />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setBulkOpen(false)}>Cancel</Button>
              <Button onClick={handleBulkAssign} disabled={bulkMutation.isPending}>Assign</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

