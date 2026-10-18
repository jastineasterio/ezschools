import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFinancialCategoriesByType, createFinancialCategory, updateFinancialCategory } from '@/services/finance';
import type { FinancialCategory, CreateFinancialCategoryRequest } from '@/types/finance';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
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
  categoryType: z.string().min(1, 'Category type is required'),
  description: z.string().optional(),
  status: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export function ManagerFinancePage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<FinancialCategory | null>(null);

  const { data: incomeCategories, isLoading: incomeLoading } = useQuery({
    queryKey: ['finance-categories', 'INCOME'],
    queryFn: () => getFinancialCategoriesByType('INCOME'),
  });

  const { data: expenseCategories, isLoading: expenseLoading } = useQuery({
    queryKey: ['finance-categories', 'EXPENSE'],
    queryFn: () => getFinancialCategoriesByType('EXPENSE'),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateFinancialCategoryRequest) => createFinancialCategory(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['finance-categories'] });
      setOpen(false);
      setEditing(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CreateFinancialCategoryRequest }) =>
      updateFinancialCategory(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['finance-categories'] });
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
      categoryType: 'INCOME',
      description: '',
      status: 'ACTIVE',
    },
  });

  const openCreate = () => {
    setEditing(null);
    reset({ name: '', categoryType: 'INCOME', description: '', status: 'ACTIVE' });
    setOpen(true);
  };

  const openEdit = (item: FinancialCategory) => {
    setEditing(item);
    reset({
      name: item.name,
      categoryType: item.categoryType,
      description: item.description || '',
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

  const renderCategoryTable = (categories: FinancialCategory[] | undefined, loading: boolean, type: string) => (
    <div className="mb-8">
      <h3 className="text-lg font-semibold mb-4 capitalize">{type} Categories</h3>
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-4">Name</th>
                <th className="text-left py-3 px-4">Description</th>
                <th className="text-left py-3 px-4">Status</th>
                <th className="text-right py-3 px-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                [...Array(3)].map((_, i) => (
                  <tr key={i} className="border-b">
                    <td className="py-3 px-4"><Skeleton className="h-4 w-32" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-48" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-4"><Skeleton className="h-4 w-20 ml-auto" /></td>
                  </tr>
                ))
              ) : !categories || categories.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-6 text-center text-muted-foreground">No categories found.</td>
                </tr>
              ) : (
                categories.map((cat) => (
                  <tr key={cat.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4 font-medium">{cat.name}</td>
                    <td className="py-3 px-4">{cat.description || '-'}</td>
                    <td className="py-3 px-4">
                      <Badge variant={cat.status === 'ACTIVE' ? 'success' : 'secondary'}>{cat.status}</Badge>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Button variant="ghost" size="icon" onClick={() => openEdit(cat)}>
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
    </div>
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Finance</h1>
        <Button onClick={openCreate} className="gap-2">
          <Plus className="h-4 w-4" /> Add Category
        </Button>
      </div>

      {renderCategoryTable(incomeCategories, incomeLoading, 'Income')}
      {renderCategoryTable(expenseCategories, expenseLoading, 'Expense')}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent title={editing ? 'Edit Category' : 'Add Category'}>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Name</label>
              <Input {...register('name')} />
              {errors.name && <p className="text-sm text-red-600 mt-1">{errors.name.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Type</label>
              <Select
                options={[
                  { value: 'INCOME', label: 'Income' },
                  { value: 'EXPENSE', label: 'Expense' },
                ]}
                {...register('categoryType')}
              />
              {errors.categoryType && <p className="text-sm text-red-600 mt-1">{errors.categoryType.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Description</label>
              <Input {...register('description')} />
            </div>
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

