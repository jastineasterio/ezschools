import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { DropdownMenu, DropdownMenuItem } from '@/components/ui/DropdownMenu';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import { Plus, MoreVertical, Edit, Trash2 } from 'lucide-react';
import {
  getClasses,
  createClass,
  updateClass,
  updateClassStatus,
} from '@/services/academic';
import type { ClassResponse, CreateClassRequest } from '@/types/academic';
import type { UpdateClassRequest } from '@/services/academic';
import { Loader2 } from 'lucide-react';
import { getApiErrorMessage } from '@/services/api';

const classSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  code: z.string().optional(),
  description: z.string().optional(),
  status: z.string().optional(),
});

type ClassFormValues = z.infer<typeof classSchema>;

export function ManagerClassesPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<ClassResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error: queryError } = useQuery({
    queryKey: ['classes', page, search],
    queryFn: () => getClasses(page, 10, search),
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateClassRequest) => createClass(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to create class')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: req }: { id: number; data: UpdateClassRequest }) =>
      updateClass(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update class')),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => updateClassStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classes'] });
      setError(null);
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update class status')),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ClassFormValues>({
    resolver: zodResolver(classSchema),
    defaultValues: {
      name: '',
      code: '',
      description: '',
      status: 'ACTIVE',
    },
  });

  const openCreate = () => {
    setEditing(null);
    reset({ name: '', code: '', description: '', status: 'ACTIVE' });
    setOpen(true);
    setError(null);
  };

  const openEdit = (item: ClassResponse) => {
    setEditing(item);
    reset({
      name: item.name,
      code: item.code || '',
      description: item.description || '',
      status: item.status,
    });
    setOpen(true);
    setError(null);
  };

  const onFormSubmit = (values: ClassFormValues) => {
    setError(null);
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: values });
    } else {
      createMutation.mutate(values);
    }
  };

  const handleStatusToggle = (item: ClassResponse) => {
    const newStatus = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    statusMutation.mutate({ id: item.id, status: newStatus });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Classes</h1>
          <p className="mt-1 text-muted-foreground">Manage school classes</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" /> Add Class
        </Button>
      </div>

      {queryError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load classes</AlertTitle>
          <AlertDescription>{getApiErrorMessage(queryError, 'Please try again.')}</AlertDescription>
        </Alert>
      )}
      {error && !open && (
        <Alert variant="destructive">
          <AlertTitle>Request failed</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Card className="p-4">
        <div className="mb-4">
          <Input
            placeholder="Search classes..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            className="max-w-md"
          />
        </div>

        {isLoading ? (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <>
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Code</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground">
                        No classes found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>{item.code || '-'}</TableCell>
                      <TableCell>{item.description || '-'}</TableCell>
                      <TableCell>
                        <Badge variant={item.status === 'ACTIVE' ? 'success' : 'secondary'}>
                          {item.status}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={
                            <button className="rounded-full p-1 hover:bg-muted">
                              <MoreVertical className="h-4 w-4" />
                            </button>
                          }
                        >
                          <DropdownMenuItem onClick={() => openEdit(item)}>
                            <Edit className="mr-2 h-4 w-4" /> Edit
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => handleStatusToggle(item)}>
                            <Trash2 className="mr-2 h-4 w-4" /> {item.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                          </DropdownMenuItem>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                Page {page + 1} of {data?.totalPages || 1}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage(p => p - 1)}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= (data?.totalPages || 1) - 1}
                  onClick={() => setPage(p => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent title={editing ? 'Edit Class' : 'Add Class'} description={editing ? 'Update class details' : 'Create a new class'}>
          {error && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Name *</label>
              <Input {...register('name')} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Code</label>
              <Input {...register('code')} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Description</label>
              <Input {...register('description')} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Status</label>
              <Select
                {...register('status')}
                options={[
                  { value: 'ACTIVE', label: 'Active' },
                  { value: 'INACTIVE', label: 'Inactive' },
                ]}
              />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {(createMutation.isPending || updateMutation.isPending) && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                {editing ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
