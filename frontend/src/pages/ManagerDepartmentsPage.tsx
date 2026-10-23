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
  getEligibleDepartmentHeads,
  createDepartment,
  updateDepartment,
  updateDepartmentStatus,
} from '@/services/academic';
import type { DepartmentResponse, CreateDepartmentRequest } from '@/types/academic';
import type { UpdateDepartmentRequest } from '@/services/academic';
import { Loader2 } from 'lucide-react';
import { getApiErrorMessage } from '@/services/api';

const departmentSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  code: z.string().optional(),
  description: z.string().optional(),
  headOfDepartmentId: z.number().optional(),
  status: z.string().optional(),
});

type DepartmentFormValues = z.infer<typeof departmentSchema>;

export function ManagerDepartmentsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<DepartmentResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [headSearch, setHeadSearch] = useState('');
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const { data, isLoading, error: queryError, refetch } = useQuery({
    queryKey: ['departments', user?.schoolId, page, search],
    queryFn: () => getDepartments(page, 10, search),
  });
  const {
    data: headsData,
    isLoading: headsLoading,
    error: headsError,
    refetch: refetchHeads,
  } = useQuery({
    queryKey: ['department-head-options', user?.schoolId],
    queryFn: () => getEligibleDepartmentHeads(),
    enabled: open,
  });
  const filteredHeads = (headsData ?? []).filter((user) =>
    `${user.name} ${user.email}`
      .toLocaleLowerCase()
      .includes(headSearch.trim().toLocaleLowerCase())
  );

  const createMutation = useMutation({
    mutationFn: (req: CreateDepartmentRequest) => createDepartment(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to create department')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: req }: { id: number; data: UpdateDepartmentRequest }) =>
      updateDepartment(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update department')),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => updateDepartmentStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] });
      setError(null);
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update department status')),
  });

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<DepartmentFormValues>({
    resolver: zodResolver(departmentSchema),
    defaultValues: {
      name: '',
      code: '',
      description: '',
      headOfDepartmentId: undefined,
      status: 'ACTIVE',
    },
  });

  const openCreate = () => {
    setEditing(null);
    setHeadSearch('');
    reset({ name: '', code: '', description: '', headOfDepartmentId: undefined, status: 'ACTIVE' });
    setOpen(true);
    setError(null);
  };

  const openEdit = (item: DepartmentResponse) => {
    setEditing(item);
    setHeadSearch('');
    reset({
      name: item.name,
      code: item.code || '',
      description: item.description || '',
      headOfDepartmentId: item.headOfDepartmentId || undefined,
      status: item.status,
    });
    setOpen(true);
    setError(null);
  };

  const onFormSubmit = (values: DepartmentFormValues) => {
    setError(null);
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: values });
    } else {
      createMutation.mutate(values);
    }
  };

  const handleStatusToggle = (item: DepartmentResponse) => {
    const newStatus = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    statusMutation.mutate({ id: item.id, status: newStatus });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Departments</h1>
          <p className="mt-1 text-muted-foreground">Manage departments</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" /> Add Department
        </Button>
      </div>

      {queryError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load departments</AlertTitle>
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
            placeholder="Search departments..."
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
                    <TableHead>Head</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {!queryError && data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground">
                        No departments found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>{item.code || '-'}</TableCell>
                      <TableCell>{item.headOfDepartmentName || '-'}</TableCell>
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
        <DialogContent title={editing ? 'Edit Department' : 'Add Department'} description={editing ? 'Update department details' : 'Create a new department'}>
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
              <label className="text-sm font-medium">Head of Department</label>
              {headsError && (
                <Alert variant="destructive">
                  <AlertDescription>
                    Unable to load eligible heads: {getApiErrorMessage(headsError, 'Please try again.')}
                    <Button type="button" variant="outline" size="sm" className="ml-3" onClick={() => refetchHeads()}>
                      Retry
                    </Button>
                  </AlertDescription>
                </Alert>
              )}
              {!headsError && headsData?.length === 0 && !headsLoading && (
                <Alert>
                  <AlertDescription>
                    No active HOD users are available for this school.{' '}
                    <Link className="underline" to="/school/users">Add a user with the HOD role.</Link>
                  </AlertDescription>
                </Alert>
              )}
              <Input
                aria-label="Search HOD users"
                placeholder="Search by name or email..."
                value={headSearch}
                disabled={headsLoading || Boolean(headsError)}
                onChange={(event) => setHeadSearch(event.target.value)}
              />
              <Select
                aria-label="Head of department"
                value={watch('headOfDepartmentId')?.toString() ?? ''}
                disabled={headsLoading || Boolean(headsError)}
                onChange={(event) => {
                  const id = event.target.value;
                  setValue('headOfDepartmentId', id ? Number(id) : undefined, { shouldValidate: true });
                }}
                options={[
                  {
                    value: '',
                    label: headsLoading
                      ? 'Loading eligible users...'
                      : filteredHeads.length === 0 && headSearch
                        ? 'No matching HOD users'
                        : 'No head assigned',
                  },
                  ...filteredHeads.map((user) => ({
                    value: String(user.id),
                    label: `${user.name} (${user.email})`,
                  })),
                ]}
              />
              {headsData && headsData.length > 0 && (
                <p className="text-xs text-muted-foreground">
                  Only active users with the HOD role from this school are listed.
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
