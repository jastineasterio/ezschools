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
  getStreams,
  getStreamsByClass,
  createStream,
  updateStream,
  updateStreamStatus,
} from '@/services/academic';
import type { StreamResponse, CreateStreamRequest } from '@/types/academic';
import type { UpdateStreamRequest } from '@/services/academic';
import { Loader2 } from 'lucide-react';
import { getApiErrorMessage } from '@/services/api';

const streamSchema = z.object({
  classId: z.number().min(1, 'Class is required'),
  name: z.string().min(1, 'Name is required'),
  code: z.string().optional(),
  capacity: z.number().optional(),
  status: z.string().optional(),
});

type StreamFormValues = z.infer<typeof streamSchema>;

export function ManagerStreamsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [classFilter, setClassFilter] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<StreamResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error: queryError } = useQuery({
    queryKey: ['streams', page, search, classFilter],
    queryFn: async () => {
      if (classFilter) {
        const data = await getStreamsByClass(Number(classFilter));
        const filtered = data.filter((stream) =>
          `${stream.name} ${stream.code || ''}`.toLowerCase().includes(search.trim().toLowerCase())
        );
        return { content: filtered, totalPages: 1, hasNext: false };
      }
      return getStreams(page, 10, search);
    },
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateStreamRequest) => createStream(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['streams'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to create stream')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: req }: { id: number; data: UpdateStreamRequest }) =>
      updateStream(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['streams'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update stream')),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => updateStreamStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['streams'] });
      setError(null);
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update stream status')),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<StreamFormValues>({
    resolver: zodResolver(streamSchema),
    defaultValues: {
      classId: 0,
      name: '',
      code: '',
      capacity: undefined,
      status: 'ACTIVE',
    },
  });

  const openCreate = () => {
    setEditing(null);
    reset({ classId: 0, name: '', code: '', capacity: undefined, status: 'ACTIVE' });
    setOpen(true);
    setError(null);
  };

  const openEdit = (item: StreamResponse) => {
    setEditing(item);
    reset({
      classId: item.classId,
      name: item.name,
      code: item.code || '',
      capacity: item.capacity || undefined,
      status: item.status,
    });
    setOpen(true);
    setError(null);
  };

  const onFormSubmit = (values: StreamFormValues) => {
    setError(null);
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: values });
    } else {
      createMutation.mutate(values);
    }
  };

  const handleStatusToggle = (item: StreamResponse) => {
    const newStatus = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    statusMutation.mutate({ id: item.id, status: newStatus });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Streams</h1>
          <p className="mt-1 text-muted-foreground">Manage class streams</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" /> Add Stream
        </Button>
      </div>

      {queryError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load streams</AlertTitle>
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
        <div className="mb-4 flex flex-col gap-4 sm:flex-row">
          <Input
            placeholder="Search streams..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            className="max-w-md"
          />
          <Select
            value={classFilter}
            onChange={(e) => { setClassFilter(e.target.value); setPage(0); }}
            options={[
              { value: '', label: 'All Classes' },
            ]}
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
                    <TableHead>Class</TableHead>
                    <TableHead>Code</TableHead>
                    <TableHead>Capacity</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        No streams found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>{item.className}</TableCell>
                      <TableCell>{item.code || '-'}</TableCell>
                      <TableCell>{item.capacity || '-'}</TableCell>
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

            {!classFilter && (
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
            )}
          </>
        )}
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent title={editing ? 'Edit Stream' : 'Add Stream'} description={editing ? 'Update stream details' : 'Create a new stream'}>
          {error && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Class *</label>
              <Input type="number" {...register('classId', { valueAsNumber: true })} />
              {errors.classId && <p className="text-sm text-destructive">{errors.classId.message}</p>}
            </div>
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
              <label className="text-sm font-medium">Capacity</label>
              <Input type="number" {...register('capacity', { valueAsNumber: true })} />
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
