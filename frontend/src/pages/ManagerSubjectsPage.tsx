import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { useAuth } from '@/store/AuthContext';
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
  getDepartments,
  getSubjects,
  createSubject,
  updateSubject,
  updateSubjectStatus,
} from '@/services/academic';
import type { SubjectResponse, CreateSubjectRequest } from '@/types/academic';
import type { UpdateSubjectRequest } from '@/services/academic';
import { Loader2 } from 'lucide-react';
import { getApiErrorMessage } from '@/services/api';

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
  const [departmentSearch, setDepartmentSearch] = useState('');
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const { data, isLoading, error: queryError, refetch } = useQuery({
    queryKey: ['subjects', user?.schoolId, page, search],
    queryFn: () => getSubjects(page, 10, search),
  });
  const {
    data: departmentsData,
    isLoading: departmentsLoading,
    error: departmentsError,
    refetch: refetchDepartments,
  } = useQuery({
    queryKey: ['departments', user?.schoolId, 'options'],
    queryFn: () => getDepartments(0, 100),
    enabled: open,
  });
  const filteredDepartments = (departmentsData?.content ?? []).filter((department) =>
    `${department.name} ${department.code ?? ''}`
      .toLocaleLowerCase()
      .includes(departmentSearch.trim().toLocaleLowerCase())
  );

  const createMutation = useMutation({
    mutationFn: (req: CreateSubjectRequest) => createSubject(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subjects'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to create subject')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: req }: { id: number; data: UpdateSubjectRequest }) =>
      updateSubject(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subjects'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update subject')),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => updateSubjectStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subjects'] });
      setError(null);
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update subject status')),
  });

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
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
    setDepartmentSearch('');
    reset({ name: '', code: '', description: '', departmentId: undefined, status: 'ACTIVE' });
    setOpen(true);
    setError(null);
  };

  const openEdit = (item: SubjectResponse) => {
    setEditing(item);
    setDepartmentSearch('');
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
    const request = {
      ...values,
      departmentId: Number.isFinite(values.departmentId) ? values.departmentId : undefined,
    };
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: request });
    } else {
      createMutation.mutate(request);
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
        <div className="flex flex-wrap items-center gap-3">
          <Link to="/school/stream-subjects" className="text-sm font-medium text-primary hover:underline">
            Configure stream subjects
          </Link>
          <Button onClick={openCreate}>
            <Plus className="mr-2 h-4 w-4" /> Add Subject
          </Button>
        </div>
      </div>

      {queryError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load subjects</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(queryError, 'Please try again.')}
            <Button variant="outline" size="sm" className="ml-3" onClick={() => refetch()}>
              Retry
            </Button>
          </AlertDescription>
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
                  {!queryError && data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground">
                        No subjects found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((item) => (
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
              <label className="text-sm font-medium">Department</label>
              {departmentsError && (
                <Alert variant="destructive">
                  <AlertDescription>
                    Unable to load departments: {getApiErrorMessage(departmentsError, 'Please try again.')}
                    <Button type="button" variant="outline" size="sm" className="ml-3" onClick={() => refetchDepartments()}>
                      Retry
                    </Button>
                  </AlertDescription>
                </Alert>
              )}
              {!departmentsError && departmentsData?.content.length === 0 && !departmentsLoading && (
                <Alert>
                  <AlertDescription>
                    No departments are available for this school.{' '}
                    <Link className="underline" to="/school/departments">Create a department first.</Link>
                  </AlertDescription>
                </Alert>
              )}
              <Input
                aria-label="Search departments"
                placeholder="Search departments by name or code..."
                value={departmentSearch}
                disabled={departmentsLoading || Boolean(departmentsError)}
                onChange={(event) => setDepartmentSearch(event.target.value)}
              />
              <Select
                aria-label="Department"
                value={watch('departmentId')?.toString() ?? ''}
                disabled={departmentsLoading || Boolean(departmentsError)}
                onChange={(event) => {
                  const id = event.target.value;
                  setValue('departmentId', id ? Number(id) : undefined, { shouldValidate: true });
                }}
                options={[
                  {
                    value: '',
                    label: departmentsLoading
                      ? 'Loading departments...'
                      : filteredDepartments.length === 0 && departmentSearch
                        ? 'No matching departments'
                        : 'No department',
                  },
                  ...filteredDepartments.map((department) => ({
                    value: String(department.id),
                    label: `${department.name}${department.code ? ` (${department.code})` : ''}`,
                  })),
                ]}
              />
              {departmentsData && departmentsData.content.length > 0 && (
                <p className="text-xs text-muted-foreground">
                  Select a department from this school. Search above to filter the list.
                </p>
              )}
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
