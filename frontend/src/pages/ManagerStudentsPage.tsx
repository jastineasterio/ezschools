import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  getStudents,
  createStudent,
  updateStudent,
  updateStudentStatus,
} from '@/services/people';
import type { StudentProfileResponse, CreateStudentProfileRequest } from '@/types/academic';
import type { PaginatedResponse } from '@/types/api';
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
import { Plus, MoreVertical, UserX, UserCheck } from 'lucide-react';
import { Loader2 } from 'lucide-react';

const studentSchema = z.object({
  userId: z.number().min(1, 'User ID is required'),
  admissionNumber: z.string().min(1, 'Admission number is required'),
  firstName: z.string().min(1, 'First name is required'),
  middleName: z.string().optional(),
  lastName: z.string().min(1, 'Last name is required'),
  gender: z.string().optional(),
  dateOfBirth: z.string().optional(),
  registrationDate: z.string().optional(),
  phone: z.string().optional(),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  address: z.string().optional(),
  photoUrl: z.string().optional(),
  studentType: z.string().optional(),
  status: z.string().optional(),
  emergencyContactName: z.string().optional(),
  emergencyContactPhone: z.string().optional(),
});

type StudentFormValues = z.infer<typeof studentSchema>;

const statusOptions = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'GRADUATED', label: 'Graduated' },
  { value: 'SUSPENDED', label: 'Suspended' },
  { value: 'WITHDRAWN', label: 'Withdrawn' },
];

export function ManagerStudentsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editingStudent, setEditingStudent] = useState<StudentProfileResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['students', page, search],
    queryFn: async () => {
      const result = await getStudents(page, 10);
      return result as PaginatedResponse<StudentProfileResponse>;
    },
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateStudentProfileRequest) => createStudent(req as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      setOpen(false);
      setEditingStudent(null);
      setFormError(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CreateStudentProfileRequest> }) =>
      updateStudent(id, data as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      setOpen(false);
      setEditingStudent(null);
      setFormError(null);
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      updateStudentStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<StudentFormValues>({
    resolver: zodResolver(studentSchema),
    defaultValues: {
      userId: 0,
      admissionNumber: '',
      firstName: '',
      middleName: '',
      lastName: '',
      gender: '',
      dateOfBirth: '',
      registrationDate: '',
      phone: '',
      email: '',
      address: '',
      photoUrl: '',
      studentType: '',
      status: 'ACTIVE',
      emergencyContactName: '',
      emergencyContactPhone: '',
    },
  });

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (!newOpen) {
      setEditingStudent(null);
      setFormError(null);
      reset({
        userId: 0,
        admissionNumber: '',
        firstName: '',
        middleName: '',
        lastName: '',
        gender: '',
        dateOfBirth: '',
        registrationDate: '',
        phone: '',
        email: '',
        address: '',
        photoUrl: '',
        studentType: '',
        status: 'ACTIVE',
        emergencyContactName: '',
        emergencyContactPhone: '',
      });
    }
  };

  const handleEdit = (student: StudentProfileResponse) => {
    setEditingStudent(student);
    reset({
      userId: student.userId,
      admissionNumber: student.admissionNumber,
      firstName: student.firstName,
      middleName: student.middleName || '',
      lastName: student.lastName,
      gender: student.gender || '',
      dateOfBirth: student.dateOfBirth || '',
      registrationDate: student.registrationDate || '',
      phone: student.phone || '',
      email: student.email || '',
      address: student.address || '',
      photoUrl: student.photoUrl || '',
      studentType: student.studentType || '',
      status: student.status || 'ACTIVE',
      emergencyContactName: student.emergencyContactName || '',
      emergencyContactPhone: student.emergencyContactPhone || '',
    });
    setOpen(true);
  };

  const onSubmit = async (values: StudentFormValues) => {
    setFormError(null);
    try {
      if (editingStudent) {
        await updateMutation.mutateAsync({
          id: editingStudent.id,
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
        return 'success';
      case 'SUSPENDED':
        return 'destructive';
      case 'INACTIVE':
        return 'outline';
      case 'GRADUATED':
        return 'default';
      case 'WITHDRAWN':
        return 'outline';
      default:
        return 'default';
    }
  };

  const handleStatusChange = (studentId: number, newStatus: string) => {
    statusMutation.mutate({ id: studentId, status: newStatus });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Students</h1>
          <p className="mt-1 text-muted-foreground">Manage students in your school</p>
        </div>
        <Button onClick={() => { setEditingStudent(null); setOpen(true); }}>
          <Plus className="mr-2 h-4 w-4" /> Add Student
        </Button>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input
            placeholder="Search students..."
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
                    <TableHead>Admission No.</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Gender</TableHead>
                    <TableHead>Phone</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        No students found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((student: any) => (
                    <TableRow key={student.id}>
                      <TableCell className="font-medium">{student.admissionNumber}</TableCell>
                      <TableCell>{student.fullName || `${student.firstName} ${student.lastName}`}</TableCell>
                      <TableCell>{student.gender || '-'}</TableCell>
                      <TableCell>{student.phone || '-'}</TableCell>
                      <TableCell>
                        <Badge variant={getStatusVariant(student.status)}>
                          {student.status}
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
                          <DropdownMenuItem onClick={() => handleEdit(student)}>
                            Edit
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => handleStatusChange(student.id, student.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')}>
                            {student.status === 'ACTIVE' ? (
                              <><UserX className="mr-2 h-4 w-4" /> Deactivate</>
                            ) : (
                              <><UserCheck className="mr-2 h-4 w-4" /> Activate</>
                            )}
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
          title={editingStudent ? 'Edit Student' : 'Add Student'}
          description={editingStudent ? 'Update student information' : 'Create a new student'}
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
                <label className="text-sm font-medium">Middle Name</label>
                <Input {...register('middleName')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Admission Number *</label>
                <Input {...register('admissionNumber')} />
                {errors.admissionNumber && <p className="text-sm text-destructive">{errors.admissionNumber.message}</p>}
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Gender</label>
                <Select
                  {...register('gender')}
                  options={[
                    { value: '', label: 'Select gender' },
                    { value: 'MALE', label: 'Male' },
                    { value: 'FEMALE', label: 'Female' },
                    { value: 'OTHER', label: 'Other' },
                  ]}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Date of Birth</label>
                <Input type="date" {...register('dateOfBirth')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Registration Date</label>
                <Input type="date" {...register('registrationDate')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Phone</label>
                <Input {...register('phone')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Email</label>
                <Input type="email" {...register('email')} />
                {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
              </div>
              <div className="space-y-2 md:col-span-2">
                <label className="text-sm font-medium">Address</label>
                <Input {...register('address')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Student Type</label>
                <Select
                  {...register('studentType')}
                  options={[
                    { value: '', label: 'Select type' },
                    { value: 'DAY', label: 'Day' },
                    { value: 'BOARDING', label: 'Boarding' },
                  ]}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Status</label>
                <Select
                  {...register('status')}
                  options={statusOptions}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Emergency Contact Name</label>
                <Input {...register('emergencyContactName')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Emergency Contact Phone</label>
                <Input {...register('emergencyContactPhone')} />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {(createMutation.isPending || updateMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {editingStudent ? 'Update Student' : 'Create Student'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

