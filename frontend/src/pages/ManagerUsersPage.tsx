import { useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AlertCircle, CheckCircle2, Loader2, Plus, Search, Users } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Dialog, DialogClose, DialogContent } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { getApiErrorMessage } from '@/services/api';
import { createSchoolUser, getSchoolUsers } from '@/services/users';
import type { CreateSchoolUserRequest } from '@/types/api';
import { useDebounce } from '@/hooks/useDebounce';

const roles = [
  'TEACHER',
  'STUDENT',
  'ACCOUNTANT',
  'PARENT',
  'HOD',
  'LIBRARIAN',
  'WARDEN',
  'TRANSPORT_OFFICER',
] as const;

const roleLabels: Record<(typeof roles)[number], string> = {
  TEACHER: 'Teacher',
  STUDENT: 'Student',
  ACCOUNTANT: 'Accountant',
  PARENT: 'Parent',
  HOD: 'Head of Department',
  LIBRARIAN: 'Librarian',
  WARDEN: 'Warden',
  TRANSPORT_OFFICER: 'Transport Officer',
};

const userSchema = z
  .object({
    role: z.enum(roles),
    firstName: z.string().trim().min(1, 'First name is required').max(100),
    lastName: z.string().trim().min(1, 'Last name is required').max(100),
    email: z.string().trim().email('Enter a valid email address'),
    phone: z.string().trim().optional(),
    password: z.string().min(6, 'Password must be at least 6 characters'),
    admissionNumber: z.string().trim().optional(),
    gender: z.string().optional(),
    dateOfBirth: z.string().optional(),
    employmentStatus: z.string().optional(),
    qualification: z.string().trim().optional(),
    experienceYears: z.string().optional(),
    studentType: z.string().optional(),
    address: z.string().trim().optional(),
    emergencyContactName: z.string().trim().optional(),
    emergencyContactPhone: z.string().trim().optional(),
    occupation: z.string().trim().optional(),
  })
  .superRefine((value, context) => {
    const required = (field: keyof typeof value, label: string) => {
      if (!value[field]?.trim()) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: [field],
          message: `${label} is required`,
        });
      }
    };

    if (value.role === 'STUDENT') {
      required('admissionNumber', 'Admission number');
    }
    if (value.role === 'TEACHER') {
      required('gender', 'Gender');
      required('dateOfBirth', 'Date of birth');
      required('employmentStatus', 'Employment status');
      required('qualification', 'Qualification');
      required('experienceYears', 'Years of experience');
      if (value.experienceYears && (!/^\d+$/.test(value.experienceYears) || Number(value.experienceYears) > 100)) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['experienceYears'],
          message: 'Enter a whole number between 0 and 100',
        });
      }
    }
    if (value.role === 'PARENT') {
      required('phone', 'Phone number');
    }
  });

type UserFormValues = z.infer<typeof userSchema>;

const roleOptions = [
  { value: '', label: 'All roles' },
  ...roles.map((role) => ({ value: role, label: roleLabels[role] })),
  { value: 'MANAGER', label: 'Manager' },
];
const statusOptions = [
  { value: '', label: 'All statuses' },
  { value: 'true', label: 'Active' },
  { value: 'false', label: 'Inactive' },
];
const formRoleOptions = roles.map((role) => ({ value: role, label: roleLabels[role] }));
const genderOptions = [
  { value: '', label: 'Select gender' },
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
];
const employmentOptions = [
  { value: '', label: 'Select employment status' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'ON_LEAVE', label: 'On leave' },
  { value: 'TERMINATED', label: 'Terminated' },
  { value: 'RETIRED', label: 'Retired' },
];
const studentTypeOptions = [
  { value: 'DAY', label: 'Day student' },
  { value: 'BOARDING', label: 'Boarding student' },
];

export function ManagerUsersPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [formError, setFormError] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const queryClient = useQueryClient();

  const usersQuery = useQuery({
    queryKey: ['school-users', page, debouncedSearch, roleFilter, statusFilter],
    queryFn: () =>
      getSchoolUsers({
        page,
        size: 10,
        search: debouncedSearch || undefined,
        role: roleFilter || undefined,
        active: statusFilter ? statusFilter === 'true' : undefined,
      }),
    placeholderData: (previousData) => previousData,
  });

  const createMutation = useMutation({
    mutationFn: (request: CreateSchoolUserRequest) => createSchoolUser(request),
    onSuccess: async (createdUser) => {
      setSuccessMessage(`${createdUser.firstName} ${createdUser.lastName} was added successfully.`);
      setFormError('');
      setDialogOpen(false);
      reset();
      setPage(0);
      setSearch('');
      setRoleFilter('');
      setStatusFilter('');
      await queryClient.invalidateQueries({ queryKey: ['school-users'] });
    },
    onError: (error) => {
      setFormError(getApiErrorMessage(error, 'Unable to create this user.'));
    },
  });

  const {
    register,
    watch,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<UserFormValues>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      role: 'TEACHER',
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      password: '',
      admissionNumber: '',
      gender: '',
      dateOfBirth: '',
      employmentStatus: 'ACTIVE',
      qualification: '',
      experienceYears: '',
      studentType: 'DAY',
      address: '',
      emergencyContactName: '',
      emergencyContactPhone: '',
      occupation: '',
    },
  });
  const selectedRole = watch('role');

  const openCreateDialog = () => {
    setFormError('');
    setSuccessMessage('');
    reset();
    setDialogOpen(true);
  };

  const closeDialog = (open: boolean) => {
    if (createMutation.isPending) return;
    setDialogOpen(open);
    if (!open) setFormError('');
  };

  const onSubmit = (values: UserFormValues) => {
    setFormError('');
    const request: CreateSchoolUserRequest = {
      role: values.role,
      firstName: values.firstName,
      lastName: values.lastName,
      email: values.email,
      password: values.password,
      phone: values.phone || undefined,
      ...(values.role === 'STUDENT'
        ? {
            admissionNumber: values.admissionNumber,
            gender: values.gender || undefined,
            dateOfBirth: values.dateOfBirth || undefined,
            studentType: values.studentType,
            address: values.address || undefined,
            emergencyContactName: values.emergencyContactName || undefined,
            emergencyContactPhone: values.emergencyContactPhone || undefined,
          }
        : {}),
      ...(values.role === 'TEACHER'
        ? {
            gender: values.gender,
            dateOfBirth: values.dateOfBirth,
            employmentStatus: values.employmentStatus,
            qualification: values.qualification,
            experienceYears: Number(values.experienceYears),
          }
        : {}),
      ...(values.role === 'PARENT'
        ? {
            gender: values.gender || undefined,
            address: values.address || undefined,
            occupation: values.occupation || undefined,
          }
        : {}),
    };
    createMutation.mutate(request);
  };

  const data = usersQuery.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">School Users</h1>
          <p className="mt-1 text-muted-foreground">Manage accounts and roles for your school.</p>
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="mr-2 h-4 w-4" /> Add User
        </Button>
      </div>

      {successMessage && (
        <Alert className="border-green-200 bg-green-50 text-green-800 dark:border-green-900 dark:bg-green-950 dark:text-green-300">
          <CheckCircle2 className="mr-2 inline h-4 w-4" />
          <AlertDescription>{successMessage}</AlertDescription>
        </Alert>
      )}

      <Card className="space-y-4 p-4 sm:p-6">
        <div className="grid gap-3 sm:grid-cols-[minmax(14rem,1fr)_12rem_12rem]">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              aria-label="Search school users"
              className="pl-9"
              placeholder="Search name or email"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(0);
                setSuccessMessage('');
              }}
            />
          </div>
          <Select
            aria-label="Filter by role"
            options={roleOptions}
            value={roleFilter}
            onChange={(event) => {
              setRoleFilter(event.target.value);
              setPage(0);
            }}
          />
          <Select
            aria-label="Filter by status"
            options={statusOptions}
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setPage(0);
            }}
          />
        </div>

        {usersQuery.isError && (
          <Alert variant="destructive">
            <AlertCircle className="mr-2 inline h-4 w-4" />
            <AlertTitle>Could not load school users</AlertTitle>
            <AlertDescription role="status">
              {getApiErrorMessage(usersQuery.error, 'The user list could not be loaded.')}
              <Button
                className="ml-3"
                variant="outline"
                size="sm"
                onClick={() => void usersQuery.refetch()}
                disabled={usersQuery.isFetching}
              >
                {usersQuery.isFetching ? 'Retrying...' : 'Try again'}
              </Button>
            </AlertDescription>
          </Alert>
        )}

        {usersQuery.isLoading ? (
          <div className="space-y-3" aria-label="Loading users">
            {Array.from({ length: 5 }, (_, index) => (
              <Skeleton key={index} className="h-12 w-full" />
            ))}
          </div>
        ) : !usersQuery.isError ? (
          <>
            <div className="overflow-x-auto rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Phone</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Last sign in</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="py-12 text-center">
                        <Users className="mx-auto mb-3 h-8 w-8 text-muted-foreground" />
                        <p className="font-medium">No users found</p>
                        <p className="mt-1 text-muted-foreground">
                          {search || roleFilter || statusFilter
                            ? 'Try changing your search or filters.'
                            : 'Add a user to get started.'}
                        </p>
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell className="min-w-40 font-medium">
                        {user.firstName} {user.lastName}
                      </TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>{user.phone || '—'}</TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-1">
                          {user.roles.map((role) => (
                            <Badge key={role} variant="secondary">
                              {roleLabels[role as (typeof roles)[number]] || role}
                            </Badge>
                          ))}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={user.isActive ? 'success' : 'outline'}>
                          {user.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {user.lastLogin ? new Date(user.lastLogin).toLocaleDateString() : 'Never'}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
            {data && data.totalPages > 1 && (
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm text-muted-foreground">
                  Showing {data.pageNumber * data.pageSize + 1}–
                  {Math.min((data.pageNumber + 1) * data.pageSize, data.totalElements)} of {data.totalElements}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0 || usersQuery.isFetching}
                    onClick={() => setPage((current) => current - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page + 1 >= data.totalPages || usersQuery.isFetching}
                    onClick={() => setPage((current) => current + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </>
        ) : null}
      </Card>

      <Dialog open={dialogOpen} onOpenChange={closeDialog}>
        <DialogContent
          title="Add school user"
          description="Create an account in your school. Role-specific profile details are required where applicable."
          className="max-h-[90vh] max-w-2xl overflow-y-auto bg-card text-card-foreground"
        >
          <DialogClose onClick={() => closeDialog(false)} />
          <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
            {formError && (
              <Alert variant="destructive">
                <AlertTitle>User was not created</AlertTitle>
                <AlertDescription>{formError}</AlertDescription>
              </Alert>
            )}
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Role" error={errors.role?.message}>
                <Select options={formRoleOptions} {...register('role')} />
              </Field>
              <Field label="Email" error={errors.email?.message}>
                <Input type="email" autoComplete="off" {...register('email')} />
              </Field>
              <Field label="First name" error={errors.firstName?.message}>
                <Input autoComplete="given-name" {...register('firstName')} />
              </Field>
              <Field label="Last name" error={errors.lastName?.message}>
                <Input autoComplete="family-name" {...register('lastName')} />
              </Field>
              <Field label="Phone" error={errors.phone?.message}>
                <Input type="tel" {...register('phone')} />
              </Field>
              <Field label="Temporary password" error={errors.password?.message}>
                <Input type="password" autoComplete="new-password" {...register('password')} />
              </Field>

              {selectedRole === 'STUDENT' && (
                <>
                  <Field label="Admission number" error={errors.admissionNumber?.message}>
                    <Input {...register('admissionNumber')} />
                  </Field>
                  <Field label="Gender">
                    <Select options={genderOptions} {...register('gender')} />
                  </Field>
                  <Field label="Date of birth">
                    <Input type="date" {...register('dateOfBirth')} />
                  </Field>
                  <Field label="Student type">
                    <Select options={studentTypeOptions} {...register('studentType')} />
                  </Field>
                  <Field label="Address">
                    <Input {...register('address')} />
                  </Field>
                  <Field label="Emergency contact name">
                    <Input {...register('emergencyContactName')} />
                  </Field>
                  <Field label="Emergency contact phone">
                    <Input type="tel" {...register('emergencyContactPhone')} />
                  </Field>
                </>
              )}

              {selectedRole === 'TEACHER' && (
                <>
                  <Field label="Gender" error={errors.gender?.message}>
                    <Select options={genderOptions} {...register('gender')} />
                  </Field>
                  <Field label="Date of birth" error={errors.dateOfBirth?.message}>
                    <Input type="date" {...register('dateOfBirth')} />
                  </Field>
                  <Field label="Employment status" error={errors.employmentStatus?.message}>
                    <Select options={employmentOptions} {...register('employmentStatus')} />
                  </Field>
                  <Field label="Qualification" error={errors.qualification?.message}>
                    <Input {...register('qualification')} />
                  </Field>
                  <Field label="Years of experience" error={errors.experienceYears?.message}>
                    <Input type="number" min="0" max="100" step="1" {...register('experienceYears')} />
                  </Field>
                </>
              )}

              {selectedRole === 'PARENT' && (
                <>
                  <Field label="Gender">
                    <Select options={genderOptions} {...register('gender')} />
                  </Field>
                  <Field label="Occupation">
                    <Input {...register('occupation')} />
                  </Field>
                  <Field label="Address">
                    <Input {...register('address')} />
                  </Field>
                </>
              )}
            </div>
            <div className="flex justify-end gap-3 border-t pt-4">
              <Button type="button" variant="outline" onClick={() => closeDialog(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Create user
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium">{label}</label>
      {children}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
