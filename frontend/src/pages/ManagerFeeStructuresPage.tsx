import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFeeStructures, createFeeStructure, updateFeeStructure } from '@/services/fees';
import type { FeeStructure, CreateFeeStructureRequest } from '@/types/fees';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus, Pencil } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
  amount: z.coerce.number().positive('Amount must be positive'),
  dueDate: z.string().optional(),
  academicYearId: z.coerce.number().optional(),
  classId: z.coerce.number().optional(),
  feeType: z.string().min(1, 'Fee type is required'),
  isMandatory: z.boolean().optional(),
  status: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export function ManagerFeeStructuresPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<FeeStructure | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['fee-structures'],
    queryFn: () => getFeeStructures(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateFeeStructureRequest) => createFeeStructure(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-structures'] });
      setOpen(false);
      setEditing(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CreateFeeStructureRequest }) =>
      updateFeeStructure(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-structures'] });
      setOpen(false);
      setEditing(null);
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
      name: '',
      description: '',
      amount: 0,
      feeType: 'TUITION',
      isMandatory: true,
      status: 'ACTIVE',
    },
  });

  const openCreate = () => {
    setEditing(null);
    reset({
      name: '',
      description: '',
      amount: 0,
      feeType: 'TUITION',
      isMandatory: true,
      status: 'ACTIVE',
    });
    setOpen(true);
  };

  const openEdit = (item: FeeStructure) => {
    setEditing(item);
    reset({
      name: item.name,
      description: item.description || '',
      amount: item.amount,
      dueDate: item.dueDate || '',
      academicYearId: item.academicYearId || undefined,
      classId: item.classId || undefined,
      feeType: item.feeType,
      isMandatory: item.isMandatory,
      status: item.status,
    });
    setOpen(true);
  };

  const onSubmit = (values: FormValues) => {
    if (editing) {
      updateMutation.mutate({ id: editing.id, payload: values });
    } else {
      createMutation.mutate(values);
    }
  };

  const items = data?.content || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Fee Structures</h1>
        <Button onClick={openCreate} className="gap-2">
          <Plus className="h-4 w-4" /> Add Fee Structure
        </Button>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-4">Name</th>
                <th className="text-left py-3 px-4">Type</th>
                <th className="text-left py-3 px-4">Amount</th>
                <th className="text-left py-3 px-4">Due Date</th>
                <th className="text-left py-3 px-4">Status</th>
                <th className="text-right py-3 px-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i} className="border-b">
                    <td className="py-3 px-4"><Skeleton className="h-4 w-32" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-20" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-24" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-20 ml-auto" /></td>
                  </tr>
                ))
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-muted-foreground">
                    No fee structures found. Create one to get started.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4 font-medium">{item.name}</td>
                    <td className="py-3 px-4">{item.feeType}</td>
                    <td className="py-3 px-4">{item.amount.toLocaleString()}</td>
                    <td className="py-3 px-4">{item.dueDate || '-'}</td>
                    <td className="py-3 px-4">
                      <Badge variant={item.status === 'ACTIVE' ? 'success' : 'secondary'}>
                        {item.status}
                      </Badge>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Button variant="ghost" size="icon" onClick={() => openEdit(item)}>
                        <Pencil className="h-4 w-4" />
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
        <DialogContent title={editing ? 'Edit Fee Structure' : 'Add Fee Structure'}>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input label="Name" {...register('name')} />
            {errors.name && <p className="text-sm text-red-600">{errors.name.message}</p>}
            <Input label="Amount" type="number" step="0.01" {...register('amount')} />
            {errors.amount && <p className="text-sm text-red-600">{errors.amount.message}</p>}
            <Input label="Due Date" type="date" {...register('dueDate')} />
            <Input label="Fee Type" {...register('feeType')} />
            {errors.feeType && <p className="text-sm text-red-600">{errors.feeType.message}</p>}
            <Input label="Description" {...register('description')} />
            <div className="flex justify-end gap-2 pt-2">
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

