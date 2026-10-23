import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { getSchoolUsers } from '@/services/users';
import type { SchoolUserResponse } from '@/types/api';
import { SearchableSelect } from '@/components/ui/SearchableSelect';
import { getTeachers, createTeacher, updateTeacher } from '@/services/people';
import type { Teacher, CreateTeacherRequest, UpdateTeacherRequest } from '@/services/people';
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
import { Plus, MoreVertical } from 'lucide-react';
import { Loader2 } from 'lucide-react';

const teacherSchema = z.object({
  userId: z.number().min(1, 'Teacher account is required'),
  employeeNumber: z.string().optional(),
  gender: z.string().optional(),
  dateOfBirth: z.string().optional(),
  employmentStatus: z.string().optional(),
  qualification: z.string().optional(),
  experienceYears: z.number().optional(),
  photoUrl: z.string().optional(),
});

type TeacherFormValues = z.infer<typeof teacherSchema>;

const employmentStatusOptions = [
  { value: '', label: 'Select status' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_LEAVE', label: 'On Leave' },
  { value: 'TERMINATED', label: 'Terminated' },
  { value: 'RETIRED', label: 'Retired' },
];

const genderOptions = [
  { value: '', label: 'Select gender' },
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
];

export function ManagerTeachersPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editingTeacher, setEditingTeacher] = useState<Teacher | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error: teachersError, refetch: refetchTeachers } = useQuery({
    queryKey: ['teachers', page, search],
    queryFn: () => getTeachers(page, 10),
  });
  const teacherUsersQuery = useQuery({
    queryKey: ['school-users', 'teacher-profile-options'],
    queryFn: () => getSchoolUsers({ page: 0, size: 500, role: 'TEACHER' }),
    enabled: open,
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateTeacherRequest) => createTeacher(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teachers'] });
      setOpen(false);
      setEditingTeacher(null);
      setFormError(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: request }: { id: number; data: UpdateTeacherRequest }) =>
      updateTeacher(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teachers'] });
      setOpen(false);
      setEditingTeacher(null);
      setFormError(null);
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<TeacherFormValues>({
    resolver: zodResolver(teacherSchema),
    defaultValues: {
      userId: 0,
      employeeNumber: '',
      gender: '',
      dateOfBirth: '',
      employmentStatus: '',
      qualification: '',
      experienceYears: undefined,
      photoUrl: '',
    },
  });

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (!newOpen) {
      setEditingTeacher(null);
      setFormError(null);
      reset({
        userId: 0,
        employeeNumber: '',
        gender: '',
        dateOfBirth: '',
        employmentStatus: '',
        qualification: '',
        experienceYears: undefined,
        photoUrl: '',
      });
    }
  };
  const teacherAccountOptions = (teacherUsersQuery.data?.content ?? [])
    .filter((account: SchoolUserResponse) => account.roles.includes('TEACHER')
      && (account.isActive || account.id === editingTeacher?.userId)
      && !data?.content.some((profile) => profile.userId === account.id && profile.id !== editingTeacher?.id))
    .map((account: SchoolUserResponse) => ({
      value: String(account.id),
      label: `${account.firstName} ${account.lastName} (${account.email})`,
    }));

  const handleEdit = (teacher: Teacher) => {
    setEditingTeacher(teacher);
    reset({
      userId: teacher.userId,
      employeeNumber: teacher.employeeNumber || '',
      gender: teacher.gender || '',
      dateOfBirth: teacher.dateOfBirth || '',
      employmentStatus: teacher.employmentStatus || '',
      qualification: teacher.qualification || '',
      experienceYears: teacher.experienceYears || undefined,
      photoUrl: teacher.photoUrl || '',
    });
    setOpen(true);
  };

  const onSubmit = async (values: TeacherFormValues) => {
    setFormError(null);
    try {
      if (editingTeacher) {
        await updateMutation.mutateAsync({
          id: editingTeacher.id,
          data: values,
        });
      } else {
        await createMutation.mutateAsync(values);
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE':
      case 'PERMANENT':
        return 'success';
      case 'ON_LEAVE':
        return 'outline';
      case 'TERMINATED':
        return 'destructive';
      case 'RETIRED':
        return 'outline';
      default:
        return 'default';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Teachers</h1>
          <p className="mt-1 text-muted-foreground">Manage teachers in your school</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <Link to="/school/teacher-assignments" className="text-sm font-medium text-primary hover:underline">
            Manage teaching assignments
          </Link>
          <Button onClick={() => { setEditingTeacher(null); setOpen(true); }}>
            <Plus className="mr-2 h-4 w-4" /> Add Teacher
          </Button>
        </div>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input
            placeholder="Search teachers..."
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
                    <TableHead>Employee No.</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Qualification</TableHead>
                    <TableHead>Experience</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        No teachers found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((teacher) => (
                    <TableRow key={teacher.id}>
                      <TableCell className="font-medium">{teacher.employeeNumber || '-'}</TableCell>
                      <TableCell>{teacher.firstName} {teacher.lastName}</TableCell>
                      <TableCell>{teacher.qualification || '-'}</TableCell>
                      <TableCell>{teacher.experienceYears != null ? `${teacher.experienceYears} yrs` : '-'}</TableCell>
                      <TableCell>
                        <Badge variant={getStatusVariant(teacher.employmentStatus)}>
                          {teacher.employmentStatus || '-'}
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
                          <DropdownMenuItem onClick={() => handleEdit(teacher)}>
                            Edit
                          </DropdownMenuItem>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            {teachersError && (
              <Alert variant="destructive" className="mt-4">
                <AlertTitle>Unable to load teachers</AlertTitle>
                <AlertDescription>
                  {teachersError instanceof Error ? teachersError.message : 'Please try again.'}
                  <Button variant="outline" size="sm" className="ml-3" onClick={() => refetchTeachers()}>
                    Retry
                  </Button>
                </AlertDescription>
              </Alert>
            )}

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
          title={editingTeacher ? 'Edit Teacher' : 'Add Teacher'}
          description={editingTeacher ? 'Update teacher information' : 'Create a new teacher'}
        >
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          {teacherUsersQuery.error && (
            <Alert variant="destructive">
              <AlertTitle>Unable to load eligible teacher accounts</AlertTitle>
              <AlertDescription>
                {teacherUsersQuery.error instanceof Error ? teacherUsersQuery.error.message : 'Please try again.'}
                <Button variant="outline" size="sm" className="ml-3" onClick={() => teacherUsersQuery.refetch()}>
                  Retry
                </Button>
              </AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <SearchableSelect
                  label="Teacher account *"
                  value={watch('userId') ? String(watch('userId')) : ''}
                  options={teacherAccountOptions}
                  placeholder="Choose an eligible teacher account"
                  loading={teacherUsersQuery.isLoading}
                  onChange={(event) => setValue('userId', Number(event.target.value) || 0, { shouldValidate: true })}
                  emptyMessage="No eligible teacher accounts found"
                />
                {errors.userId && <p className="text-sm text-destructive">{errors.userId.message}</p>}
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Employee Number</label>
                <Input {...register('employeeNumber')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Gender</label>
                <Select
                  {...register('gender')}
                  options={genderOptions}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Date of Birth</label>
                <Input type="date" {...register('dateOfBirth')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Employment Status</label>
                <Select
                  {...register('employmentStatus')}
                  options={employmentStatusOptions}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Qualification</label>
                <Input {...register('qualification')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Experience (Years)</label>
                <Input type="number" {...register('experienceYears', { valueAsNumber: true })} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Photo URL</label>
                <Input {...register('photoUrl')} />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {(createMutation.isPending || updateMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {editingTeacher ? 'Update Teacher' : 'Create Teacher'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
