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
  getSubjects,
  createSubject,
  updateSubject,
  updateSubjectStatus,
} from '@/services/academic';
import type { SubjectResponse, CreateSubjectRequest } from '@/types/academic';
import type { UpdateSubjectRequest } from '@/services/academic';
import { Loader2 } from 'lucide-react';

const subjectSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  code: z.string().optional(),
  description: z.string().optional(),
  departmentId: z.number().optional(),
  status: z.string().optional(),
});

type SubjectFormValues = z.infer<typeof subjectSchema>;

export function ManagerSubjectsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<SubjectResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['subjects', page, search],
    queryFn: () => getSubjects(page, 10),
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateSubjectRequest) => createSubject(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subjects'] });
      setOpen(false);
      setEditing(null);
      reset();
    },
    onError: (err: any) => setError(err?.message || 'Failed to create subject'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: req }: { id: number; data: UpdateSubjectRequest }) =>
      updateSubject(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subjects'] });
      setOpen(false);
      setEditing(null);
      reset();
    },
    onError: (err: any) => setError(err?.message || 'Failed to update subject'),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => updateSubjectStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subjects'] });
    },
    onError: (err: any) => setError(err?.message || 'Failed to update subject status'),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<SubjectFormValues>({
    resolver: zodResolver(subjectSchema),
    defaultValues: {
      name: '',
      code: '',
      description: '',
      departmentId: undefined,
      status: 'ACTIVE',
    },
  });

  const openCreate = () => {
    setEditing(null);
    reset({ name: '', code: '', description: '', departmentId: undefined, status: 'ACTIVE' });
    setOpen(true);
    setError(null);
  };

  const openEdit = (item: SubjectResponse) => {
    setEditing(item);
    reset({
      name: item.name,
      code: item.code || '',
      description: item.description || '',
      departmentId: item.departmentId || undefined,
      status: item.status,
    });
    setOpen(true);
    setError(null);
  };

  const onFormSubmit = (values: SubjectFormValues) => {
    setError(null);
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: values });
    } else {
      createMutation.mutate(values);
    }
  };

  const handleStatusToggle = (item: SubjectResponse) => {
    const newStatus = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    statusMutation.mutate({ id: item.id, status: newStatus });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Subjects</h1>
          <p className="mt-1 text-muted-foreground">Manage subjects</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" /> Add Subject
        </Button>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input
            placeholder="Search subjects..."
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
                    <TableHead>Department</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground">
                        No subjects found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((item: any) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>{item.code || '-'}</TableCell>
                      <TableCell>{item.departmentName || '-'}</TableCell>
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
        <DialogContent title={editing ? 'Edit Subject' : 'Add Subject'} description={editing ? 'Update subject details' : 'Create a new subject'}>
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
              <label className="text-sm font-medium">Department ID</label>
              <Input type="number" {...register('departmentId', { valueAsNumber: true })} />
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

