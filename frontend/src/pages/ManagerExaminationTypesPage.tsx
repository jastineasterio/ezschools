import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { DropdownMenu, DropdownMenuItem } from '@/components/ui/DropdownMenu';
import { Skeleton } from '@/components/ui/Skeleton';
import { Select } from '@/components/ui/Select';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import {
  getExaminationTypes,
  createExaminationType,
  updateExaminationType,
  setExaminationTypeStatus,
  deleteExaminationType,
} from '@/services/assessments';
import type {
  ExaminationType,
  CreateExaminationTypeRequest,
  UpdateExaminationTypeRequest,
} from '@/services/assessments';
import { Loader2, Plus, MoreVertical, Edit, Eye, Trash2 } from 'lucide-react';
import { getApiErrorMessage } from '@/services/api';

const examinationTypeSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be 100 characters or fewer'),
  description: z.string().optional(),
  active: z.string().default('true'),
});

type ExaminationTypeFormValues = z.infer<typeof examinationTypeSchema>;

const statusOptions = [
  { value: 'true', label: 'Active' },
  { value: 'false', label: 'Inactive' },
];

export function ManagerExaminationTypesPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [viewOpen, setViewOpen] = useState(false);
  const [editing, setEditing] = useState<ExaminationType | null>(null);
  const [viewing, setViewing] = useState<ExaminationType | null>(null);
  const [deleting, setDeleting] = useState<ExaminationType | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error: queryError, refetch } = useQuery({
    queryKey: ['examination-types', page, search],
    queryFn: () => getExaminationTypes(page, 10, search || undefined, undefined),
  });

  const createMutation = useMutation({
    mutationFn: (req: ExaminationTypeFormValues) => {
      const payload: CreateExaminationTypeRequest = {
        name: req.name,
        description: req.description,
        active: req.active === 'true',
      };
      return createExaminationType(payload);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['examination-types'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      setSuccess('Examination type created.');
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to create examination type')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: req }: { id: number; data: ExaminationTypeFormValues }) => {
      const payload: UpdateExaminationTypeRequest = {
        name: req.name,
        description: req.description,
        active: req.active === 'true',
      };
      return updateExaminationType(id, payload);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['examination-types'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      setSuccess('Examination type updated.');
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update examination type')),
  });

  const setStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      setExaminationTypeStatus(id, active),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['examination-types'] });
      setDeleting(null);
      setSuccess('Examination type status updated.');
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update examination type status')),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteExaminationType(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['examination-types'] });
      setDeleting(null);
      setError(null);
      setSuccess('Examination type deleted.');
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to delete examination type')),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ExaminationTypeFormValues>({
    resolver: zodResolver(examinationTypeSchema),
    defaultValues: {
      name: '',
      description: '',
      active: 'true',
    },
  });

  const openCreate = () => {
    setEditing(null);
    reset({ name: '', description: '', active: 'true' });
    setOpen(true);
    setError(null);
    setSuccess(null);
  };

  const openEdit = (item: ExaminationType) => {
    setEditing(item);
    reset({ name: item.name, description: item.description || '', active: item.active ? 'true' : 'false' });
    setOpen(true);
    setError(null);
    setSuccess(null);
  };

  const openView = (item: ExaminationType) => {
    setViewing(item);
    setViewOpen(true);
  };

  const onFormSubmit = (values: ExaminationTypeFormValues) => {
    setError(null);
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: values });
    } else {
      createMutation.mutate(values);
    }
  };

  const handleSetStatus = (item: ExaminationType) => {
    setDeleting(item);
    setStatusMutation.mutate({ id: item.id, active: !item.active });
  };

  const handleDelete = (item: ExaminationType) => {
    setDeleting(item);
    deleteMutation.mutate(item.id);
  };

  const content = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Examination Types</h1>
          <p className="mt-1 text-muted-foreground">
            Manage the examination types used when creating examinations for this school.
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" /> Add Examination Type
        </Button>
      </div>

      {queryError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load examination types</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(queryError, 'Please try again.')}
            <Button className="ml-3" size="sm" variant="outline" onClick={() => void refetch()}>
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      )}
      {success && !open && (
        <Alert>
          <AlertTitle>Saved</AlertTitle>
          <AlertDescription>{success}</AlertDescription>
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
            placeholder="Search examination types..."
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
                    <TableHead>Type Name</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Date Created</TableHead>
                    <TableHead className="w-24">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {content.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center text-muted-foreground">
                        No examination types found
                      </TableCell>
                    </TableRow>
                  )}
                  {content.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>{item.description || '-'}</TableCell>
                      <TableCell>
                        <Badge variant={item.active ? 'success' : 'secondary'}>
                          {item.active ? 'Active' : 'Inactive'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '-'}
                      </TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={
                            <button className="rounded-full p-1 hover:bg-muted">
                              <MoreVertical className="h-4 w-4" />
                            </button>
                          }
                        >
                          <DropdownMenuItem onClick={() => openView(item)}>
                            <Eye className="mr-2 h-4 w-4" /> View
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => openEdit(item)}>
                            <Edit className="mr-2 h-4 w-4" /> Edit
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => handleSetStatus(item)}
                            className={item.active ? 'text-destructive' : 'text-green-600 dark:text-green-400'}
                          >
                            {item.active ? <Trash2 className="mr-2 h-4 w-4" /> : <Plus className="mr-2 h-4 w-4" />}
                            {item.active ? 'Deactivate' : 'Activate'}
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => handleDelete(item)} className="text-destructive">
                            <Trash2 className="mr-2 h-4 w-4" /> Delete
                          </DropdownMenuItem>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-muted-foreground">
                Page {page + 1} of {data?.totalPages || 1}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page + 1 >= (data?.totalPages || 1)}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent
          title={editing ? 'Edit Examination Type' : 'Add Examination Type'}
          description={editing ? 'Update examination type details' : 'Create a new examination type'}
        >
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
              <label className="text-sm font-medium">Description</label>
              <Input {...register('description')} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Status</label>
              <Select {...register('active')} options={statusOptions} />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {(createMutation.isPending || updateMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {editing ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={viewOpen} onOpenChange={setViewOpen}>
        <DialogContent title="Examination Type Details" description={viewing ? viewing.name : ''}>
          <div className="space-y-3">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1">
                <p className="text-xs text-muted-foreground">Type Name</p>
                <p className="text-sm font-medium">{viewing?.name}</p>
              </div>
              <div className="space-y-1">
                <p className="text-xs text-muted-foreground">Status</p>
                <Badge variant={viewing?.active ? 'success' : 'secondary'}>
                  {viewing?.active ? 'Active' : 'Inactive'}
                </Badge>
              </div>
            </div>
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Description</p>
              <p className="text-sm">{viewing?.description || '-'}</p>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1">
                <p className="text-xs text-muted-foreground">Date Created</p>
                <p className="text-sm">{viewing?.createdAt ? new Date(viewing.createdAt).toLocaleDateString() : '-'}</p>
              </div>
              <div className="space-y-1">
                <p className="text-xs text-muted-foreground">Last Updated</p>
                <p className="text-sm">{viewing?.updatedAt ? new Date(viewing.updatedAt).toLocaleDateString() : '-'}</p>
              </div>
            </div>
          </div>
          <div className="flex justify-end mt-4">
            <Button type="button" variant="outline" onClick={() => setViewOpen(false)}>
              Close
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {deleting && (
        <Dialog open onOpenChange={(open) => { if (!open) setDeleting(null); }}>
          <DialogContent
            title="Delete Examination Type"
            description={`Are you sure you want to delete "${deleting.name}"? This cannot be undone if the type is in use.`}
          >
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setDeleting(null)}>
                Cancel
              </Button>
              <Button
                type="button"
                variant="destructive"
                onClick={() => handleDelete(deleting)}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : ''} Delete
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}
