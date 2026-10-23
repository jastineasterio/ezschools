import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { getEnrollments, createEnrollment, updateEnrollment, getStudents } from '@/services/people';
import type { Student, CreateEnrollmentRequest, UpdateEnrollmentRequest } from '@/services/people';
import { getAcademicYears, getClasses, getStreamsByClass } from '@/services/academic';
import type { AcademicYear, Class, Stream } from '@/services/academic';
import type { StudentEnrollmentResponse } from '@/types/academic';
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
import { Plus, MoreVertical } from 'lucide-react';
import { Loader2 } from 'lucide-react';
import { SearchableSelect } from '@/components/ui/SearchableSelect';

const enrollmentSchema = z.object({
  studentId: z.number().min(1, 'Student ID is required'),
  academicYearId: z.number().min(1, 'Academic year is required'),
  classId: z.number().min(1, 'Class is required'),
  streamId: z.number().optional(),
  status: z.string().optional(),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().optional(),
});

type EnrollmentFormValues = z.infer<typeof enrollmentSchema>;

const statusOptions = [
  { value: '', label: 'Select status' },
  { value: 'ENROLLED', label: 'Enrolled' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'WITHDRAWN', label: 'Withdrawn' },
  { value: 'TRANSFERRED', label: 'Transferred' },
  { value: 'SUSPENDED', label: 'Suspended' },
];

export function ManagerEnrollmentsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editingEnrollment, setEditingEnrollment] = useState<StudentEnrollmentResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, error: enrollmentsError, refetch: refetchEnrollments } = useQuery({
    queryKey: ['enrollments', page, search],
    queryFn: async () => {
      const result = await getEnrollments(page, 10);
      return result as PaginatedResponse<StudentEnrollmentResponse>;
    },
  });
  const studentsQuery = useQuery({
    queryKey: ['enrollment-students'],
    queryFn: () => getStudents(0, 500),
    enabled: open,
  });
  const yearsQuery = useQuery({
    queryKey: ['enrollment-academic-years'],
    queryFn: () => getAcademicYears(0, 500),
    enabled: open,
  });
  const classesQuery = useQuery({
    queryKey: ['enrollment-classes'],
    queryFn: () => getClasses(0, 500),
    enabled: open,
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateEnrollmentRequest) => createEnrollment(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrollments'] });
      setOpen(false);
      setEditingEnrollment(null);
      setFormError(null);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data: request }: { id: number; data: UpdateEnrollmentRequest }) =>
      updateEnrollment(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrollments'] });
      setOpen(false);
      setEditingEnrollment(null);
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
  } = useForm<EnrollmentFormValues>({
    resolver: zodResolver(enrollmentSchema),
    defaultValues: {
      studentId: 0,
      academicYearId: 0,
      classId: 0,
      streamId: undefined,
      status: '',
      startDate: '',
      endDate: '',
    },
  });
  const selectedClassId = watch('classId');
  const streamsQuery = useQuery({
    queryKey: ['enrollment-streams', selectedClassId],
    queryFn: () => getStreamsByClass(selectedClassId),
    enabled: open && Number.isInteger(selectedClassId) && selectedClassId > 0,
  });

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (!newOpen) {
      setEditingEnrollment(null);
      setFormError(null);
      reset({
        studentId: 0,
        academicYearId: 0,
        classId: 0,
        streamId: undefined,
        status: '',
        startDate: '',
        endDate: '',
      });
    }
  };

  const handleEdit = (enrollment: StudentEnrollmentResponse) => {
    setEditingEnrollment(enrollment);
    reset({
      studentId: enrollment.studentId,
      academicYearId: enrollment.academicYearId,
      classId: enrollment.classId,
      streamId: enrollment.streamId || undefined,
      status: enrollment.status || '',
      startDate: enrollment.startDate || '',
      endDate: enrollment.endDate || '',
    });
    setOpen(true);
  };

  const onSubmit = async (values: EnrollmentFormValues) => {
    setFormError(null);
    try {
      if (editingEnrollment) {
        await updateMutation.mutateAsync({
          id: editingEnrollment.id,
          data: { ...values, status: values.status || 'ENROLLED', endDate: values.endDate || undefined },
        });
      } else {
        await createMutation.mutateAsync({ ...values, status: values.status || 'ENROLLED' });
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Operation failed');
    }
  };
  const retryOptions = () => {
    void Promise.all([
      studentsQuery.refetch(),
      yearsQuery.refetch(),
      classesQuery.refetch(),
      streamsQuery.refetch(),
    ]);
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'ENROLLED':
        return 'success';
      case 'COMPLETED':
        return 'default';
      case 'WITHDRAWN':
        return 'destructive';
      case 'TRANSFERRED':
        return 'outline';
      case 'SUSPENDED':
        return 'destructive';
      default:
        return 'default';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Enrollments</h1>
          <p className="mt-1 text-muted-foreground">Manage student enrollments</p>
        </div>
        <Button onClick={() => { setEditingEnrollment(null); setOpen(true); }}>
          <Plus className="mr-2 h-4 w-4" /> Add Enrollment
        </Button>
      </div>

      <Card className="p-4">
        {enrollmentsError && (
          <Alert variant="destructive" className="mb-4">
            <AlertTitle>Unable to load enrollments</AlertTitle>
            <AlertDescription>
              {enrollmentsError instanceof Error ? enrollmentsError.message : 'Please try again.'}
              <Button variant="outline" size="sm" className="ml-3" onClick={() => refetchEnrollments()}>
                Retry
              </Button>
            </AlertDescription>
          </Alert>
        )}
        <div className="mb-4">
          <Input
            placeholder="Search enrollments..."
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
                    <TableHead>Student</TableHead>
                    <TableHead>Class</TableHead>
                    <TableHead>Stream</TableHead>
                    <TableHead>Academic Year</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        No enrollments found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((enrollment) => (
                    <TableRow key={enrollment.id}>
                      <TableCell className="font-medium">{enrollment.studentName}</TableCell>
                      <TableCell>{enrollment.className}</TableCell>
                      <TableCell>{enrollment.streamName || '-'}</TableCell>
                      <TableCell>{enrollment.academicYearName}</TableCell>
                      <TableCell>
                        <Badge variant={getStatusVariant(enrollment.status)}>
                          {enrollment.status}
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
                          <DropdownMenuItem onClick={() => handleEdit(enrollment)}>
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
          title={editingEnrollment ? 'Edit Enrollment' : 'Add Enrollment'}
          description={editingEnrollment ? 'Update enrollment information' : 'Create a new enrollment'}
        >
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          {(studentsQuery.error || yearsQuery.error || classesQuery.error || streamsQuery.error) && (
            <Alert variant="destructive">
              <AlertTitle>Unable to load assignment options</AlertTitle>
              <AlertDescription>
                One or more option lists could not be loaded.
                <Button type="button" variant="outline" size="sm" className="ml-3" onClick={retryOptions}>
                  Retry
                </Button>
              </AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <SearchableSelect
                  label="Student *"
                  value={watch('studentId') ? String(watch('studentId')) : ''}
                  options={(studentsQuery.data?.content ?? []).map((student: Student) => ({
                    value: String(student.id),
                    label: `${student.firstName} ${student.lastName} (${student.admissionNumber})`,
                  }))}
                  placeholder="Choose a student"
                  loading={studentsQuery.isLoading}
                  onChange={(event) => setValue('studentId', Number(event.target.value) || 0, { shouldValidate: true })}
                  emptyMessage="No students found"
                />
                {errors.studentId && <p className="text-sm text-destructive">{errors.studentId.message}</p>}
              </div>
              <div className="space-y-2">
                <SearchableSelect
                  label="Academic Year *"
                  value={watch('academicYearId') ? String(watch('academicYearId')) : ''}
                  options={(yearsQuery.data?.content ?? []).map((year: AcademicYear) => ({
                    value: String(year.id),
                    label: year.name,
                  }))}
                  placeholder="Choose an academic year"
                  loading={yearsQuery.isLoading}
                  onChange={(event) => setValue('academicYearId', Number(event.target.value) || 0, { shouldValidate: true })}
                  emptyMessage="No academic years found"
                />
                {errors.academicYearId && <p className="text-sm text-destructive">{errors.academicYearId.message}</p>}
              </div>
              <div className="space-y-2">
                <SearchableSelect
                  label="Class *"
                  value={selectedClassId ? String(selectedClassId) : ''}
                  options={(classesQuery.data?.content ?? []).map((clazz: Class) => ({
                    value: String(clazz.id),
                    label: clazz.code ? `${clazz.name} (${clazz.code})` : clazz.name,
                  }))}
                  placeholder="Choose a class"
                  loading={classesQuery.isLoading}
                  onChange={(event) => {
                    setValue('classId', Number(event.target.value) || 0, { shouldValidate: true });
                    setValue('streamId', undefined, { shouldValidate: true });
                  }}
                  emptyMessage="No classes found"
                />
                {errors.classId && <p className="text-sm text-destructive">{errors.classId.message}</p>}
              </div>
              <div className="space-y-2">
                <SearchableSelect
                  label="Stream"
                  value={watch('streamId') ? String(watch('streamId')) : ''}
                  options={(streamsQuery.data ?? []).map((stream: Stream) => ({
                    value: String(stream.id),
                    label: stream.code ? `${stream.name} (${stream.code})` : stream.name,
                  }))}
                  placeholder={selectedClassId ? 'Choose a stream (optional)' : 'Choose a class first'}
                  loading={streamsQuery.isLoading}
                  disabled={!selectedClassId}
                  onChange={(event) => setValue('streamId', Number(event.target.value) || undefined, { shouldValidate: true })}
                  emptyMessage={selectedClassId ? 'No streams are configured for this class' : 'Select a class first'}
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
                <label className="text-sm font-medium">Start Date *</label>
                <Input type="date" {...register('startDate')} />
                {errors.startDate && <p className="text-sm text-destructive">{errors.startDate.message}</p>}
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">End Date</label>
                <Input type="date" {...register('endDate')} />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending
                  || !(watch('studentId') > 0) || !(watch('academicYearId') > 0) || !(watch('classId') > 0)}
              >
                {(createMutation.isPending || updateMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {editingEnrollment ? 'Update Enrollment' : 'Create Enrollment'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
