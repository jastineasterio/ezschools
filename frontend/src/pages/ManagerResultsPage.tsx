import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getResults, getExaminationResults, submitResult, approveResult, rejectResult, publishResult } from '@/services/assessments';
import { api } from '@/services/api';
import type { ResultResponse, CreateResultRequest } from '@/types/academic';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus, RefreshCw, Edit, CheckCircle, XCircle, Send, Eye } from 'lucide-react';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

const resultSchema = z.object({
  studentId: z.number().min(1, 'Student is required'),
  examinationSubjectId: z.number().min(1, 'Examination subject is required'),
  marksObtained: z.coerce.number().optional(),
  remarks: z.string().optional(),
});

type ResultFormValues = z.infer<typeof resultSchema>;

export function ManagerResultsPage() {
  const [page, setPage] = useState(0);
  const [examinationFilter, setExaminationFilter] = useState('');
  const [open, setOpen] = useState(false);
  const [editResult, setEditResult] = useState<ResultResponse | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: examinationFilter ? ['results', 'examination', examinationFilter, page] : ['results', page],
    queryFn: async () => {
      if (examinationFilter) {
        return getExaminationResults(Number(examinationFilter), page, 10);
      }
      return getResults(page, 10);
    },
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateResultRequest) => api.post('/school/results', req).then(r => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['results'] });
      setOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CreateResultRequest> }) =>
      api.put(`/school/results/${id}`, data).then(r => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['results'] });
      setOpen(false);
      setEditResult(null);
    },
  });

  const submitMutation = useMutation({
    mutationFn: submitResult,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['results'] });
    },
  });

  const approveMutation = useMutation({
    mutationFn: approveResult,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['results'] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, comment }: { id: number; comment?: string }) => rejectResult(id, comment),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['results'] });
    },
  });

  const publishMutation = useMutation({
    mutationFn: publishResult,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['results'] });
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ResultFormValues>({
    resolver: zodResolver(resultSchema),
  });

  const handleCreate = (values: ResultFormValues) => {
    createMutation.mutate(values);
  };

  const handleUpdate = (values: ResultFormValues) => {
    if (editResult) {
      updateMutation.mutate({ id: editResult.id, data: values });
    }
  };

  const handleFormSubmit = editResult ? handleSubmit(handleUpdate) : handleSubmit(handleCreate);

  const openCreateDialog = () => {
    setEditResult(null);
    reset({
      studentId: 0,
      examinationSubjectId: 0,
      marksObtained: undefined,
      remarks: '',
    });
    setOpen(true);
  };

  const openEditDialog = (record: ResultResponse) => {
    setEditResult(record);
    reset({
      studentId: record.studentId,
      examinationSubjectId: record.examinationSubjectId,
      marksObtained: record.marksObtained || undefined,
      remarks: record.remarks || '',
    });
    setOpen(true);
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PUBLISHED':
        return <Badge variant="success">{status}</Badge>;
      case 'APPROVED':
        return <Badge variant="success">{status}</Badge>;
      case 'SUBMITTED':
        return <Badge variant="warning">{status}</Badge>;
      case 'REJECTED':
        return <Badge variant="destructive">{status}</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Results</h1>
          <p className="mt-1 text-muted-foreground">Manage and review examination results</p>
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="mr-2 h-4 w-4" /> Add Result
        </Button>
      </div>

      <Card className="p-4">
        <div className="mb-4 flex flex-col gap-4 sm:flex-row">
          <Input
            label="Examination ID"
            type="number"
            placeholder="Filter by examination"
            value={examinationFilter}
            onChange={(e) => { setExaminationFilter(e.target.value); setPage(0); }}
            className="sm:w-48"
          />
          <Button
            variant="outline"
            onClick={() => { setExaminationFilter(''); setPage(0); }}
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
                    <TableHead>Admission No</TableHead>
                    <TableHead>Examination</TableHead>
                    <TableHead>Subject</TableHead>
                    <TableHead>Marks</TableHead>
                    <TableHead>Grade</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-24">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={8} className="text-center text-muted-foreground">
                        No results found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((result: any) => (
                    <TableRow key={result.id}>
                      <TableCell className="font-medium">{result.studentName}</TableCell>
                      <TableCell>{result.admissionNumber}</TableCell>
                      <TableCell>{result.examinationName}</TableCell>
                      <TableCell>{result.subjectName}</TableCell>
                      <TableCell>{result.marksObtained ?? '-'}</TableCell>
                      <TableCell>{result.grade ?? '-'}</TableCell>
                      <TableCell>{getStatusBadge(result.status)}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <Button variant="ghost" size="icon" onClick={() => openEditDialog(result)} title="Edit">
                            <Edit className="h-4 w-4" />
                          </Button>
                          {result.status === 'DRAFT' && (
                            <Button variant="ghost" size="icon" onClick={() => submitMutation.mutate(result.id)} title="Submit">
                              <Send className="h-4 w-4" />
                            </Button>
                          )}
                          {result.status === 'SUBMITTED' && (
                            <>
                              <Button variant="ghost" size="icon" onClick={() => approveMutation.mutate(result.id)} title="Approve">
                                <CheckCircle className="h-4 w-4 text-green-600" />
                              </Button>
                              <Button variant="ghost" size="icon" onClick={() => rejectMutation.mutate({ id: result.id, comment: '' })} title="Reject">
                                <XCircle className="h-4 w-4 text-red-600" />
                              </Button>
                            </>
                          )}
                          {result.status === 'APPROVED' && (
                            <Button variant="ghost" size="icon" onClick={() => publishMutation.mutate(result.id)} title="Publish">
                              <Eye className="h-4 w-4" />
                            </Button>
                          )}
                        </div>
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
        <DialogContent title={editResult ? 'Edit Result' : 'Add Result'} description={editResult ? 'Update result details' : 'Record a new result'}>
          <form onSubmit={handleFormSubmit} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Student ID *</label>
              <Input type="number" {...register('studentId', { valueAsNumber: true })} />
              {errors.studentId && <p className="text-sm text-destructive">{errors.studentId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Examination Subject ID *</label>
              <Input type="number" {...register('examinationSubjectId', { valueAsNumber: true })} />
              {errors.examinationSubjectId && <p className="text-sm text-destructive">{errors.examinationSubjectId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Marks Obtained</label>
              <Input type="number" {...register('marksObtained', { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Remarks</label>
              <Input {...register('remarks')} />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => { setOpen(false); setEditResult(null); }}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                {createMutation.isPending || updateMutation.isPending ? 'Saving...' : editResult ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

