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
import { Plus, MoreVertical, Edit, Star } from 'lucide-react';
import {
  getAcademicYears,
  createAcademicYear,
  updateAcademicYear,
  setCurrentAcademicYear,
} from '@/services/academic';
import type { AcademicYearResponse, CreateAcademicYearRequest } from '@/types/academic';
import type { UpdateAcademicYearRequest } from '@/services/academic';
import { Loader2 } from 'lucide-react';
import { getApiErrorMessage } from '@/services/api';

const academicYearSchema = z
  .object({
    name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be 100 characters or fewer'),
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
    status: z.string().optional(),
    isCurrent: z.boolean().optional(),
  })
  .superRefine((value, context) => {
    if (value.startDate && value.endDate && value.startDate > value.endDate) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['endDate'],
        message: 'End date must be on or after the start date',
      });
    }
  });

type AcademicYearFormValues = z.infer<typeof academicYearSchema>;

export function ManagerAcademicYearsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AcademicYearResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error: queryError, refetch } = useQuery({
    queryKey: ['academic-years', page, search],
    queryFn: () => getAcademicYears(page, 10, search),
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateAcademicYearRequest) => createAcademicYear(req),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['academic-years'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      setSuccess('Academic year created.');
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to create academic year')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: req }: { id: number; data: UpdateAcademicYearRequest }) =>
      updateAcademicYear(id, req),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['academic-years'] });
      setOpen(false);
      setEditing(null);
      setError(null);
      setSuccess('Academic year updated.');
      reset();
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to update academic year')),
  });

  const setCurrentMutation = useMutation({
    mutationFn: (id: number) => setCurrentAcademicYear(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['academic-years'] });
      setError(null);
      setSuccess('Current academic year updated.');
    },
    onError: (err: unknown) => setError(getApiErrorMessage(err, 'Failed to set current academic year')),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AcademicYearFormValues>({
    resolver: zodResolver(academicYearSchema),
    defaultValues: {
      name: '',
      startDate: '',
      endDate: '',
      status: 'ACTIVE',
      isCurrent: false,
    },
  });

  const openCreate = () => {
    setEditing(null);
    reset({ name: '', startDate: '', endDate: '', status: 'ACTIVE', isCurrent: false });
    setOpen(true);
    setError(null);
    setSuccess(null);
  };

  const openEdit = (item: AcademicYearResponse) => {
    setEditing(item);
    reset({
      name: item.name,
      startDate: item.startDate,
      endDate: item.endDate,
      status: item.status,
      isCurrent: item.isCurrent,
    });
    setOpen(true);
    setError(null);
    setSuccess(null);
  };

  const onFormSubmit = (values: AcademicYearFormValues) => {
    setError(null);
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: values });
    } else {
      createMutation.mutate(values);
    }
  };

  const handleSetCurrent = (id: number) => {
    setCurrentMutation.mutate(id);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Academic Years</h1>
          <p className="mt-1 text-muted-foreground">Manage academic years</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-2 h-4 w-4" /> Add Academic Year
        </Button>
      </div>

      {queryError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load academic years</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(queryError, 'Please try again.')}
            <Button className="ml-3" size="sm" variant="outline" onClick={() => void refetch()}>
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      )}
      {success && (
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
            placeholder="Search academic years..."
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
                    <TableHead>Start Date</TableHead>
                    <TableHead>End Date</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Current</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {!queryError && data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        No academic years found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>{item.startDate}</TableCell>
                      <TableCell>{item.endDate}</TableCell>
                      <TableCell>
                        <Badge variant={item.status === 'ACTIVE' ? 'success' : 'secondary'}>
                          {item.status}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {item.isCurrent && (
                          <Badge variant="success">
                            <Star className="mr-1 h-3 w-3" /> Current
                          </Badge>
                        )}
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
                          {!item.isCurrent && (
                            <DropdownMenuItem onClick={() => handleSetCurrent(item.id)}>
                              <Star className="mr-2 h-4 w-4" /> Set Current
                            </DropdownMenuItem>
                          )}
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
        <DialogContent title={editing ? 'Edit Academic Year' : 'Add Academic Year'} description={editing ? 'Update academic year details' : 'Create a new academic year'}>
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
              <label className="text-sm font-medium">Start Date *</label>
              <Input type="date" {...register('startDate')} />
              {errors.startDate && <p className="text-sm text-destructive">{errors.startDate.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">End Date *</label>
              <Input type="date" {...register('endDate')} />
              {errors.endDate && <p className="text-sm text-destructive">{errors.endDate.message}</p>}
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
            <div className="flex items-center gap-2">
              <input type="checkbox" id="isCurrent" {...register('isCurrent')} className="h-4 w-4" />
              <label htmlFor="isCurrent" className="text-sm">Set as current academic year</label>
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
