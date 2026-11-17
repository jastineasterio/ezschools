import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFeeStructures, getFeeAssignments, assignFee, bulkPreviewFeeAssignment, bulkAssignFee } from '@/services/fees';
import { getStudents } from '@/services/people';
import { getAcademicYears, getClasses, getStreamsByClass, type Stream } from '@/services/academic';
import type { CreateFeeAssignmentRequest, BulkFeeAssignmentRequest, BulkFeeAssignmentPreviewDto, BulkFeeAssignmentResultDto } from '@/types/fees';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus, Upload } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const schema = z.object({
  studentId: z.coerce.number().positive('Student is required'),
  feeStructureId: z.coerce.number().positive('Fee structure is required'),
  dueDate: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export function ManagerFeeAssignmentsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [bulkOpen, setBulkOpen] = useState(false);
  const [bulkStructureId, setBulkStructureId] = useState('');
  const [bulkAcademicYearId, setBulkAcademicYearId] = useState<string | null>(null);
  const [bulkClassId, setBulkClassId] = useState<string | null>(null);
  const [bulkStreamId, setBulkStreamId] = useState<string | null>(null);
  const [bulkDueDate, setBulkDueDate] = useState('');
  const [preview, setPreview] = useState<BulkFeeAssignmentPreviewDto | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [result, setResult] = useState<BulkFeeAssignmentResultDto | null>(null);
  const [streams, setStreams] = useState<Stream[]>([]);

  const { data: assignments, isLoading: assignmentsLoading } = useQuery({
    queryKey: ['fee-assignments'],
    queryFn: () => getFeeAssignments(),
  });

  const { data: feeStructures } = useQuery({
    queryKey: ['fee-structures'],
    queryFn: () => getFeeStructures(),
  });

  const { data: studentsData } = useQuery({
    queryKey: ['students'],
    queryFn: () => getStudents(0, 200),
  });

  const { data: academicYearsData } = useQuery({
    queryKey: ['academic-years'],
    queryFn: () => getAcademicYears(),
  });

  const { data: classesData } = useQuery({
    queryKey: ['classes'],
    queryFn: () => getClasses(),
  });

  const academicYears = academicYearsData?.content || [];
  const schoolClasses = classesData?.content || [];

  useEffect(() => {
    let cancelled = false;
    const classId = bulkClassId ? Number(bulkClassId) : null;
    if (!classId) {
      setStreams([]);
      setBulkStreamId(null);
      setPreview(null);
      return;
    }
    setPreviewLoading(true);
    getStreamsByClass(classId).then((loaded) => {
      if (!cancelled) {
        setStreams(loaded);
        setBulkStreamId(null);
        setPreview(null);
        setPreviewLoading(false);
      }
    });
    return () => { cancelled = true; };
  }, [bulkClassId]);

  useEffect(() => {
    let cancelled = false;
    if (!bulkStructureId || !bulkAcademicYearId || !bulkClassId) {
      setPreview(null);
      return;
    }
    setPreviewLoading(true);
    const payload: BulkFeeAssignmentRequest = {
      scope: bulkStreamId ? 'STREAM' : 'CLASS',
      classId: Number(bulkClassId),
      streamId: bulkStreamId ? Number(bulkStreamId) : null,
      academicYearId: Number(bulkAcademicYearId),
      feeStructureIds: [Number(bulkStructureId)],
      dueDate: bulkDueDate || null,
    };
    bulkPreviewFeeAssignment(payload).then((p) => {
      if (!cancelled) {
        setPreview(p);
        setPreviewLoading(false);
      }
    }).catch(() => {
      if (!cancelled) setPreviewLoading(false);
    });
    return () => { cancelled = true; };
  }, [bulkStructureId, bulkAcademicYearId, bulkClassId, bulkStreamId, bulkDueDate]);

  const assignMutation = useMutation({
    mutationFn: (payload: CreateFeeAssignmentRequest) => assignFee(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['fee-assignments'] });
      setOpen(false);
    },
  });

  const bulkMutation = useMutation({
    mutationFn: (payload: BulkFeeAssignmentRequest) => bulkAssignFee(payload),
    onSuccess: (res) => {
      setResult(res);
      queryClient.invalidateQueries({ queryKey: ['fee-assignments'] });
    },
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  const onSubmit = (values: FormValues) => {
    assignMutation.mutate({
      studentId: values.studentId,
      feeStructureId: values.feeStructureId,
      dueDate: values.dueDate || undefined,
    });
  };

  const handleBulkAssign = () => {
    if (!bulkStructureId || !bulkAcademicYearId || !bulkClassId) return;
    if (!preview || preview.eligibleStudentCount === 0) return;
    if (!window.confirm(`Assign fee to ${preview.eligibleStudentCount} eligible student(s)?`)) return;
    const payload: BulkFeeAssignmentRequest = {
      scope: bulkStreamId ? 'STREAM' : 'CLASS',
      classId: Number(bulkClassId),
      streamId: bulkStreamId ? Number(bulkStreamId) : null,
      academicYearId: Number(bulkAcademicYearId),
      feeStructureIds: [Number(bulkStructureId)],
      dueDate: bulkDueDate || null,
    };
    bulkMutation.mutate(payload);
  };

  const items = assignments || [];
  const studentOptions = studentsData?.content || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Fee Assignments</h1>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => setBulkOpen(true)} className="gap-2">
            <Upload className="h-4 w-4" /> Bulk Assign
          </Button>
          <Button onClick={() => setOpen(true)} className="gap-2">
            <Plus className="h-4 w-4" /> Assign Fee
          </Button>
        </div>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-3 px-4">Student</th>
                <th className="text-left py-3 px-4">Admission No</th>
                <th className="text-left py-3 px-4">Fee</th>
                <th className="text-left py-3 px-4">Due</th>
                <th className="text-left py-3 px-4">Paid</th>
                <th className="text-left py-3 px-4">Balance</th>
                <th className="text-left py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody>
              {assignmentsLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i} className="border-b">
                    {[...Array(7)].map((_, j) => (
                      <td key={j} className="py-3 px-4"><Skeleton className="h-4 w-24" /></td>
                    ))}
                  </tr>
                ))
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-muted-foreground">
                    No fee assignments found.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4">{item.studentName}</td>
                    <td className="py-3 px-4">{item.admissionNumber}</td>
                    <td className="py-3 px-4">{item.feeStructureName}</td>
                    <td className="py-3 px-4">{item.amountDue.toLocaleString()}</td>
                    <td className="py-3 px-4">{item.amountPaid.toLocaleString()}</td>
                    <td className="py-3 px-4 font-medium">{item.balance.toLocaleString()}</td>
                    <td className="py-3 px-4">
                      <Badge variant={item.status === 'PAID' ? 'success' : item.status === 'PENDING' ? 'warning' : 'secondary'}>
                        {item.status}
                      </Badge>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent title="Assign Fee">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Student</label>
              <Select
                options={studentOptions.map((s) => ({ value: String(s.id), label: `${s.firstName} ${s.lastName} (${s.admissionNumber})` }))}
                {...register('studentId')}
              />
              {errors.studentId && <p className="text-sm text-destructive mt-1">{errors.studentId.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Fee Structure</label>
              <Select
                options={(feeStructures?.content || []).map((f) => ({ value: String(f.id), label: `${f.name} - ${f.amount.toLocaleString()}` }))}
                {...register('feeStructureId')}
              />
              {errors.feeStructureId && <p className="text-sm text-destructive mt-1">{errors.feeStructureId.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Due Date</label>
              <Input type="date" {...register('dueDate')} />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={assignMutation.isPending}>Assign</Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={bulkOpen} onOpenChange={(isOpen) => {
        setBulkOpen(isOpen);
        if (!isOpen) {
          setBulkStructureId('');
          setBulkAcademicYearId(null);
          setBulkClassId(null);
          setBulkStreamId(null);
          setBulkDueDate('');
          setPreview(null);
          setResult(null);
        }
      }}>
        <DialogContent title="Bulk Assign Fee">
          <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); handleBulkAssign(); }}>
            <div>
              <label className="block text-sm font-medium mb-1">Fee Structure</label>
              <Select
                value={bulkStructureId}
                onChange={(e) => setBulkStructureId(e.target.value)}
                options={(feeStructures?.content || []).map((f) => ({ value: String(f.id), label: `${f.name} - ${f.amount.toLocaleString()}` }))}
              />
              {!bulkStructureId && <p className="text-sm text-destructive mt-1">Fee structure is required</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Academic Year</label>
              <Select
                value={bulkAcademicYearId ?? ''}
                onChange={(e) => setBulkAcademicYearId(e.target.value || null)}
                options={academicYears.map((y) => ({ value: String(y.id), label: y.name }))}
              />
              {!bulkAcademicYearId && <p className="text-sm text-destructive mt-1">Academic year is required</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Class</label>
              <Select
                value={bulkClassId ?? ''}
                onChange={(e) => setBulkClassId(e.target.value || null)}
                options={schoolClasses.map((c) => ({ value: String(c.id), label: c.name }))}
              />
              {!bulkClassId && <p className="text-sm text-destructive mt-1">Class is required</p>}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Stream</label>
              <Select
                value={bulkStreamId ?? ''}
                onChange={(e) => setBulkStreamId(e.target.value || null)}
                options={[
                  { value: '', label: 'All Streams' },
                  ...streams.map((s) => ({ value: String(s.id), label: s.name })),
                ]}
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Due Date (optional)</label>
              <Input type="date" value={bulkDueDate} onChange={(e) => setBulkDueDate(e.target.value)} />
            </div>

            {result ? (
              <div className="border rounded p-3 bg-success/10 text-success">
                <div className="font-semibold mb-1">Assignment complete</div>
                <div>Newly assigned: {result.assigned}</div>
                <div>Already assigned (skipped): {result.skipped}</div>
                {result.failed > 0 && <div className="text-destructive">Failed: {result.failed}</div>}
                {result.details && result.details.length > 0 && (
                  <div className="mt-2 text-sm">
                    <div className="font-semibold mb-1">Details:</div>
                    <ul className="list-disc list-inside space-y-1">
                      {result.details.slice(0, 10).map((d, i) => (
                        <li key={i}>{d.studentName} ({d.admissionNumber}): {d.outcome} — {d.reason}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ) : (
              <>
                {previewLoading ? (
                  <div className="flex items-center gap-2 text-muted-foreground"><Skeleton className="h-4 w-24" /> Loading preview...</div>
                ) : preview ? (
                  <div className="border rounded p-3 bg-muted text-sm">
                    <div><strong>Fee structure:</strong> {preview.feeStructures[0]?.name}</div>
                    <div><strong>Amount:</strong> {preview.feeStructures[0]?.amount.toLocaleString()}</div>
                    <div><strong>Academic year:</strong> {academicYears.find((y) => String(y.id) === bulkAcademicYearId)?.name ?? bulkAcademicYearId}</div>
                    <div><strong>Class:</strong> {schoolClasses.find((c) => String(c.id) === bulkClassId)?.name ?? bulkClassId}</div>
                    <div><strong>Stream:</strong> {bulkStreamId ? (streams.find((s) => String(s.id) === bulkStreamId)?.name ?? 'All Streams') : 'All Streams'}</div>
                    <div><strong>Eligible students:</strong> {preview.eligibleStudentCount}</div>
                    <div><strong>Already assigned (skipped):</strong> {Math.max(0, preview.eligibleStudentCount - preview.totalAssignments)}</div>
                    <div><strong>To be assigned:</strong> {preview.totalAssignments}</div>
                    {preview.eligibleStudentCount === 0 && (
                      <p className="text-sm text-destructive mt-1">No eligible students found for this selection.</p>
                    )}
                    {preview.students.length > 0 && (
                      <div className="mt-2">
                        <div className="font-semibold mb-1">Students (showing up to 50):</div>
                        <ul className="text-xs list-disc list-inside space-y-1">
                          {preview.students.map((s) => (
                            <li key={s.studentId}>{s.name} — {s.admissionNumber}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">Select a fee structure, academic year and class to preview eligible students.</p>
                )}
                <div className="flex justify-end gap-2 pt-2">
                  <Button type="button" variant="secondary" onClick={() => setBulkOpen(false)}>Cancel</Button>
                  <Button
                    type="submit"
                    disabled={bulkMutation.isPending || !bulkStructureId || !bulkAcademicYearId || !bulkClassId || !preview || preview.eligibleStudentCount === 0}
                  >
                    {bulkMutation.isPending ? 'Assigning...' : 'Assign'}
                  </Button>
                </div>
              </>
            )}
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

