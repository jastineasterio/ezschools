import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { getParents, createParent, updateParent } from '@/services/people';
import type { ParentResponse, CreateParentRequest } from '@/types/academic';
import type { PaginatedResponse } from '@/types/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { DropdownMenu, DropdownMenuItem } from '@/components/ui/DropdownMenu';
import { Skeleton } from '@/components/ui/Skeleton';
import { Select } from '@/components/ui/Select';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import { Plus, MoreVertical } from 'lucide-react';
import { Loader2 } from 'lucide-react';

const parentSchema = z.object({
  firstName: z.string().min(1, 'First name is required'),
  lastName: z.string().min(1, 'Last name is required'),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  phone: z.string().min(1, 'Phone is required'),
  gender: z.string().optional(),
  occupation: z.string().optional(),
  address: z.string().optional(),
  userId: z.number().optional(),
});

type ParentFormValues = z.infer<typeof parentSchema>;

const genderOptions = [
  { value: '', label: 'Select gender' },
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
];

export function ManagerParentsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editingParent, setEditingParent] = useState<ParentResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['parents', page, search],
    queryFn: async () => {
      const result = await getParents(page, 10);
      return result as PaginatedResponse<ParentResponse>;
    },
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateParentRequest) => createParent(req as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['parents'] });
      setOpen(false);
      setEditingParent(null);
      setFormError(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CreateParentRequest> }) =>
      updateParent(id, data as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['parents'] });
      setOpen(false);
      setEditingParent(null);
      setFormError(null);
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ParentFormValues>({
    resolver: zodResolver(parentSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      gender: '',
      occupation: '',
      address: '',
      userId: undefined,
    },
  });

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (!newOpen) {
      setEditingParent(null);
      setFormError(null);
      reset({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        gender: '',
        occupation: '',
        address: '',
        userId: undefined,
      });
    }
  };

  const handleEdit = (parent: ParentResponse) => {
    setEditingParent(parent);
    reset({
      firstName: parent.firstName,
      lastName: parent.lastName,
      email: parent.email || '',
      phone: parent.phone,
      gender: parent.gender || '',
      occupation: parent.occupation || '',
      address: parent.address || '',
      userId: parent.userId || undefined,
    });
    setOpen(true);
  };

  const onSubmit = async (values: ParentFormValues) => {
    setFormError(null);
    try {
      if (editingParent) {
        await updateMutation.mutateAsync({
          id: editingParent.id,
          data: values,
        });
      } else {
        await createMutation.mutateAsync(values);
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Parents</h1>
          <p className="mt-1 text-muted-foreground">Manage parents and guardians</p>
        </div>
        <Button onClick={() => { setEditingParent(null); setOpen(true); }}>
          <Plus className="mr-2 h-4 w-4" /> Add Parent
        </Button>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input
            placeholder="Search parents..."
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
                    <TableHead>Email</TableHead>
                    <TableHead>Phone</TableHead>
                    <TableHead>Gender</TableHead>
                    <TableHead>Occupation</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        No parents found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((parent: any) => (
                    <TableRow key={parent.id}>
                      <TableCell className="font-medium">{parent.fullName || `${parent.firstName} ${parent.lastName}`}</TableCell>
                      <TableCell>{parent.email || '-'}</TableCell>
                      <TableCell>{parent.phone || '-'}</TableCell>
                      <TableCell>{parent.gender || '-'}</TableCell>
                      <TableCell>{parent.occupation || '-'}</TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={
                            <button className="rounded-full p-1 hover:bg-muted">
                              <MoreVertical className="h-4 w-4" />
                            </button>
                          }
                        >
                          <DropdownMenuItem onClick={() => handleEdit(parent)}>
                            Edit
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

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent
          title={editingParent ? 'Edit Parent' : 'Add Parent'}
          description={editingParent ? 'Update parent information' : 'Create a new parent'}
        >
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">First Name *</label>
                <Input {...register('firstName')} />
                {errors.firstName && <p className="text-sm text-destructive">{errors.firstName.message}</p>}
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Last Name *</label>
                <Input {...register('lastName')} />
                {errors.lastName && <p className="text-sm text-destructive">{errors.lastName.message}</p>}
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Email</label>
                <Input type="email" {...register('email')} />
                {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Phone *</label>
                <Input {...register('phone')} />
                {errors.phone && <p className="text-sm text-destructive">{errors.phone.message}</p>}
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Gender</label>
                <Select
                  {...register('gender')}
                  options={genderOptions}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Occupation</label>
                <Input {...register('occupation')} />
              </div>
              <div className="space-y-2 md:col-span-2">
                <label className="text-sm font-medium">Address</label>
                <Input {...register('address')} />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {(createMutation.isPending || updateMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {editingParent ? 'Update Parent' : 'Create Parent'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

