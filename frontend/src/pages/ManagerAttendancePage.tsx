import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { createAttendance, bulkCreateAttendance, getAttendance, updateAttendance } from '@/services/assessments';
import { getApiErrorMessage } from '@/services/api';
import type { AttendanceResponse, CreateAttendanceRequest } from '@/types/academic';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Plus, RefreshCw, Edit } from 'lucide-react';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

const attendanceSchema = z.object({
  studentId: z.number().min(1, 'Student is required'),
  classId: z.number().min(1, 'Class is required'),
  academicYearId: z.number().min(1, 'Academic year is required'),
  attendanceDate: z.string().min(1, 'Date is required'),
  status: z.string().min(1, 'Status is required'),
  streamId: z.number().optional(),
  subjectId: z.number().optional(),
  teacherId: z.number().optional(),
  remarks: z.string().optional(),
});

const bulkAttendanceSchema = z.object({
  classId: z.number().min(1, 'Class is required'),
  academicYearId: z.number().min(1, 'Academic year is required'),
  studentIds: z.string().trim().min(1, 'Enter at least one student ID')
    .refine((value) => {
      const ids = value.split(',').map((id) => id.trim());
      return ids.every((id) => /^[1-9]\d*$/.test(id)) && new Set(ids).size === ids.length;
    }, 'Enter unique, positive student IDs separated by commas'),
  attendanceDate: z.string().min(1, 'Date is required'),
  status: z.string().min(1, 'Status is required'),
  streamId: z.number().optional(),
  subjectId: z.number().optional(),
  teacherId: z.number().optional(),
  remarks: z.string().optional(),
});

type AttendanceFormValues = z.infer<typeof attendanceSchema>;
type BulkAttendanceFormValues = z.infer<typeof bulkAttendanceSchema>;

const statusOptions = [
  { value: 'PRESENT', label: 'Present' },
  { value: 'ABSENT', label: 'Absent' },
  { value: 'LATE', label: 'Late' },
  { value: 'EXCUSED', label: 'Excused' },
];

export function ManagerAttendancePage() {
  const [page, setPage] = useState(0);
  const [classFilter, setClassFilter] = useState('');
  const [dateFilter, setDateFilter] = useState('');
  const [open, setOpen] = useState(false);
  const [bulkOpen, setBulkOpen] = useState(false);
  const [editRecord, setEditRecord] = useState<AttendanceResponse | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['attendance', page, classFilter, dateFilter],
    queryFn: () => getAttendance(
      page,
      10,
      classFilter ? Number(classFilter) : undefined,
      dateFilter || undefined
    ),
  });

  const createMutation = useMutation({
    mutationFn: createAttendance,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attendance'] });
      setOpen(false);
    },
  });

  const bulkMutation = useMutation({
    mutationFn: bulkCreateAttendance,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attendance'] });
      setBulkOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateAttendanceRequest }) =>
      updateAttendance(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attendance'] });
      setOpen(false);
      setEditRecord(null);
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AttendanceFormValues>({
    resolver: zodResolver(attendanceSchema),
  });

  const {
    register: registerBulk,
    handleSubmit: handleSubmitBulk,
    formState: { errors: bulkErrors },
  } = useForm<BulkAttendanceFormValues>({
    resolver: zodResolver(bulkAttendanceSchema),
  });

  const handleCreate = (values: AttendanceFormValues) => {
    createMutation.mutate(values);
  };

  const handleBulkCreate = (values: BulkAttendanceFormValues) => {
    const payload: CreateAttendanceRequest[] = values.studentIds.split(',').map((id) => ({
        studentId: Number(id.trim()),
        classId: values.classId,
        academicYearId: values.academicYearId,
        attendanceDate: values.attendanceDate,
        status: values.status,
        streamId: values.streamId,
        subjectId: values.subjectId,
        teacherId: values.teacherId,
        remarks: values.remarks,
      }));
    bulkMutation.mutate(payload);
  };

  const handleEdit = (record: AttendanceResponse) => {
    setEditRecord(record);
    reset({
      studentId: record.studentId,
      classId: record.classId,
      academicYearId: record.academicYearId,
      attendanceDate: record.attendanceDate,
      status: record.status,
      streamId: record.streamId || undefined,
      subjectId: record.subjectId || undefined,
      teacherId: record.teacherId || undefined,
      remarks: record.remarks || '',
    });
    setOpen(true);
  };

  const handleUpdate = (values: AttendanceFormValues) => {
    if (editRecord) {
      updateMutation.mutate({ id: editRecord.id, data: values });
    }
  };

  const handleFormSubmit = editRecord ? handleSubmit(handleUpdate) : handleSubmit(handleCreate);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Attendance</h1>
          <p className="mt-1 text-muted-foreground">Manage student attendance records</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setBulkOpen(true)}>
            <Plus className="mr-2 h-4 w-4" /> Bulk Entry
          </Button>
          <Button onClick={() => { setEditRecord(null); reset({}); setOpen(true); }}>
            <Plus className="mr-2 h-4 w-4" /> Add Attendance
          </Button>
        </div>
      </div>

      {isError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load attendance</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(error, 'Attendance records could not be loaded.')}
            <Button className="ml-3" size="sm" variant="outline" onClick={() => void refetch()}>
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      )}
      {(createMutation.isError || bulkMutation.isError || updateMutation.isError) && (
        <Alert variant="destructive">
          <AlertTitle>Attendance was not saved</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(
              createMutation.error || bulkMutation.error || updateMutation.error,
              'Check the attendance details and try again.'
            )}
          </AlertDescription>
        </Alert>
      )}

      <Card className="p-4">
        <div className="mb-4 flex flex-col gap-4 sm:flex-row">
          <Input
            label="Class ID"
            type="number"
            placeholder="Filter by class"
            value={classFilter}
            onChange={(e) => setClassFilter(e.target.value)}
            className="sm:w-48"
          />
          <Input
            label="Date"
            type="date"
            value={dateFilter}
            onChange={(e) => setDateFilter(e.target.value)}
            className="sm:w-48"
          />
          <Button
            variant="outline"
            onClick={() => { setClassFilter(''); setDateFilter(''); setPage(0); }}
          >
            <RefreshCw className="mr-2 h-4 w-4" /> Reset
          </Button>
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
                    <TableHead>Date</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Remarks</TableHead>
                    <TableHead className="w-24">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {!isError && data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        No attendance records found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((record: AttendanceResponse) => (
                    <TableRow key={record.id}>
                      <TableCell className="font-medium">{record.studentName}</TableCell>
                      <TableCell>{record.className}</TableCell>
                      <TableCell>{new Date(record.attendanceDate).toLocaleDateString()}</TableCell>
                      <TableCell>
                        <Badge variant={record.status === 'PRESENT' ? 'success' : record.status === 'ABSENT' ? 'destructive' : 'warning'}>
                          {record.status}
                        </Badge>
                      </TableCell>
                      <TableCell>{record.remarks || '-'}</TableCell>
                      <TableCell>
                        <Button variant="ghost" size="icon" onClick={() => handleEdit(record)}>
                          <Edit className="h-4 w-4" />
                        </Button>
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
                  onClick={() => setPage(p => p - 1)}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page + 1 >= (data?.totalPages || 0)}
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
        <DialogContent title={editRecord ? 'Edit Attendance' : 'Add Attendance'} description={editRecord ? 'Update attendance record' : 'Record new attendance'}>
          <form onSubmit={handleFormSubmit} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Student ID *</label>
              <Input type="number" {...register('studentId', { valueAsNumber: true })} />
              {errors.studentId && <p className="text-sm text-destructive">{errors.studentId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Class ID *</label>
              <Input type="number" {...register('classId', { valueAsNumber: true })} />
              {errors.classId && <p className="text-sm text-destructive">{errors.classId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Academic Year ID *</label>
              <Input type="number" {...register('academicYearId', { valueAsNumber: true })} />
              {errors.academicYearId && <p className="text-sm text-destructive">{errors.academicYearId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Date *</label>
              <Input type="date" {...register('attendanceDate')} />
              {errors.attendanceDate && <p className="text-sm text-destructive">{errors.attendanceDate.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Status *</label>
              <Select
                {...register('status')}
                options={statusOptions}
              />
              {errors.status && <p className="text-sm text-destructive">{errors.status.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Stream ID</label>
              <Input type="number" {...register('streamId', { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Subject ID</label>
              <Input type="number" {...register('subjectId', { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Teacher ID</label>
              <Input type="number" {...register('teacherId', { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Remarks</label>
              <Input {...register('remarks')} />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => { setOpen(false); setEditRecord(null); }}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {createMutation.isPending || updateMutation.isPending ? 'Saving...' : editRecord ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={bulkOpen} onOpenChange={setBulkOpen}>
        <DialogContent title="Bulk Attendance Entry" description="Create attendance records for multiple students">
          <form onSubmit={handleSubmitBulk(handleBulkCreate)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Class ID *</label>
              <Input type="number" {...registerBulk('classId', { valueAsNumber: true })} />
              {bulkErrors.classId && <p className="text-sm text-destructive">{bulkErrors.classId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Academic Year ID *</label>
              <Input type="number" {...registerBulk('academicYearId', { valueAsNumber: true })} />
              {bulkErrors.academicYearId && <p className="text-sm text-destructive">{bulkErrors.academicYearId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Student IDs (comma-separated) *</label>
              <Input placeholder="e.g. 18, 24, 31" {...registerBulk('studentIds')} />
              {bulkErrors.studentIds && <p className="text-sm text-destructive">{bulkErrors.studentIds.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Date *</label>
              <Input type="date" {...registerBulk('attendanceDate')} />
              {bulkErrors.attendanceDate && <p className="text-sm text-destructive">{bulkErrors.attendanceDate.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Status *</label>
              <Select
                {...registerBulk('status')}
                options={statusOptions}
              />
              {bulkErrors.status && <p className="text-sm text-destructive">{bulkErrors.status.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Stream ID</label>
              <Input type="number" {...registerBulk('streamId', { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Subject ID</label>
              <Input type="number" {...registerBulk('subjectId', { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Teacher ID</label>
              <Input type="number" {...registerBulk('teacherId', { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Remarks</label>
              <Input {...registerBulk('remarks')} />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setBulkOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={bulkMutation.isPending}>
                {bulkMutation.isPending ? 'Creating...' : 'Bulk Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
