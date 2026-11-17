import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getExaminations,
  createExamination,
  updateExamination,
  updateExaminationStatus,
  getExaminationSubjects,
  createExaminationSubject,
  getExaminationTypes,
  createExaminationType,
  getGradingSchemes,
  type GradingScheme,
} from '@/services/assessments';
import {
  getAcademicYears,
  getSubjects,
  getClasses,
  getStreamsByClass,
  type Subject,
  type Class,
  type Stream,
} from '@/services/academic';
import type { ExaminationResponse, CreateExaminationRequest } from '@/types/academic';
import type { AcademicYear } from '@/services/academic';
import type { ExaminationType } from '@/services/assessments';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import { SearchableSelect } from '@/components/ui/SearchableSelect';
import { Link } from 'react-router-dom';
import { Plus, Eye, Edit, Loader2 } from 'lucide-react';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import type { PaginatedResponse } from '@/types/api';
import { Select } from '@/components/ui/Select';
import { getApiErrorMessage } from '@/services/api';

const examinationSchema = z.object({
  academicYearId: z.number().min(1, 'Academic year is required'),
  name: z.string().min(1, 'Name is required'),
  typeId: z.number().min(1, 'Type is required'),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  status: z.string().optional(),
  description: z.string().optional(),
});

const examinationSubjectSchema = z.object({
  subjectId: z.number().min(1, 'Subject is required').optional(),
  classId: z.number().min(1, 'Class is required').optional(),
  streamId: z.number().optional(),
  maximumMarks: z.number().min(1, 'Maximum marks is required'),
  passMark: z.number().min(0, 'Pass mark is required'),
  gradingSchemeId: z.number().optional(),
});

type ExaminationFormValues = z.infer<typeof examinationSchema>;
type ExaminationSubjectFormValues = z.infer<typeof examinationSubjectSchema>;

const statusOptions = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'SCHEDULED', label: 'Scheduled' },
  { value: 'ONGOING', label: 'Ongoing' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

export function ManagerExaminationsPage() {
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [subjectsOpen, setSubjectsOpen] = useState(false);
  const [subjectDialogOpen, setSubjectDialogOpen] = useState(false);
  const [editExam, setEditExam] = useState<ExaminationResponse | null>(null);
  const [selectedExamination, setSelectedExamination] = useState<ExaminationResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [schemeCompatibilityError, setSchemeCompatibilityError] = useState<string | null>(null);
  const [createTypeOpen, setCreateTypeOpen] = useState(false);
  const [newTypeName, setNewTypeName] = useState('');
  const [newTypeDescription, setNewTypeDescription] = useState('');
  const [newTypeActive, setNewTypeActive] = useState(true);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['examinations', page],
    queryFn: () => getExaminations(page, 10),
  });

  const { data: subjectsData } = useQuery({
    queryKey: ['examination-subjects', selectedExamination?.id],
    queryFn: () => selectedExamination ? getExaminationSubjects(selectedExamination.id, 0, 50) : Promise.resolve({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 50 } as PaginatedResponse<any>),
    enabled: !!selectedExamination,
  });

  // Option lists for the create/edit form. These are scoped to the
  // authenticated manager's school by the backend (tenant-scoped queries).
  const yearsQuery = useQuery({
    queryKey: ['examination-academic-years'],
    queryFn: () => getAcademicYears(0, 500),
    enabled: open,
  });
  const typesQuery = useQuery({
    queryKey: ['examination-types'],
    queryFn: () => getExaminationTypes(0, 500, undefined, true),
    enabled: open,
  });

  const createTypeMutation = useMutation({
    mutationFn: (data: { name: string; description?: string; active?: boolean }) =>
      createExaminationType(data),
    onSuccess: async (res) => {
      await queryClient.invalidateQueries({ queryKey: ['examination-types'] });
      void typesQuery.refetch();
      setValue('typeId', res.id);
      setCreateTypeOpen(false);
      setNewTypeName('');
      setNewTypeDescription('');
      setNewTypeActive(true);
      setFormError(null);
    },
    onError: (err: unknown) => setFormError(getApiErrorMessage(err, 'Failed to create examination type')),
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateExaminationRequest) => createExamination(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['examinations'] });
      setOpen(false);
      setEditExam(null);
      setFormError(null);
    },
    onError: (err: unknown) => setFormError(getApiErrorMessage(err, 'Failed to create examination')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CreateExaminationRequest> }) =>
      updateExamination(id, data as any),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['examinations'] });
      setOpen(false);
      setEditExam(null);
      setFormError(null);
    },
    onError: (err: unknown) => setFormError(getApiErrorMessage(err, 'Failed to update examination')),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      updateExaminationStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['examinations'] });
    },
    onError: (err: unknown) => setFormError(getApiErrorMessage(err, 'Failed to update examination status')),
  });

  const createSubjectMutation = useMutation({
    mutationFn: ({ examinationId, data }: { examinationId: number; data: any }) =>
      createExaminationSubject(examinationId, data),
    retry: 0,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['examination-subjects'] });
      setSubjectDialogOpen(false);
    },
    onError: (err: unknown) => setFormError(getApiErrorMessage(err, 'Failed to create examination subject')),
  });

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ExaminationFormValues>({
    resolver: zodResolver(examinationSchema),
    defaultValues: {
      academicYearId: 0,
      name: '',
      typeId: 0,
      startDate: '',
      endDate: '',
      status: 'DRAFT',
      description: '',
    },
  });

  const {
    register: registerSubject,
    handleSubmit: handleSubmitSubject,
    formState: { errors: subjectErrors },
    watch: watchSubject,
    setValue: setSubjectValue,
  } = useForm<ExaminationSubjectFormValues>({
    resolver: zodResolver(examinationSubjectSchema),
    defaultValues: {
      subjectId: undefined,
      classId: undefined,
      streamId: undefined,
      maximumMarks: 0,
      passMark: 0,
      gradingSchemeId: undefined,
    },
  });

  // Option lists for the Add Examination Subject form. All lists are scoped to
  // the authenticated manager's school by the backend (tenant-scoped queries).
  const subjectsQuery = useQuery({
    queryKey: ['examination-subject-options'],
    queryFn: () => getSubjects(0, 500),
    enabled: subjectDialogOpen,
  });
  const classesQuery = useQuery({
    queryKey: ['examination-class-options'],
    queryFn: () => getClasses(0, 500),
    enabled: subjectDialogOpen,
  });
  const selectedClassId = watchSubject('classId');
  const prevClassIdRef = useRef<number | undefined>(undefined);
  // Clear a previously selected stream whenever the class changes, since a
  // stream only belongs to one class.
  useEffect(() => {
    if (selectedClassId != null && selectedClassId !== prevClassIdRef.current) {
      prevClassIdRef.current = selectedClassId;
      setSubjectValue('streamId', undefined);
    }
  }, [selectedClassId, setSubjectValue]);
  const streamsQuery = useQuery({
    queryKey: ['examination-stream-options', selectedClassId],
    queryFn: async () => {
      if (!selectedClassId) return [] as Stream[];
      return getStreamsByClass(selectedClassId);
    },
    enabled: subjectDialogOpen && !!selectedClassId,
  });
  const gradingSchemesQuery = useQuery({
    queryKey: ['examination-grading-scheme-options'],
    queryFn: () => getGradingSchemes(0, 500),
    enabled: subjectDialogOpen,
  });

  // Validate the selected grading scheme's actual-mark ranges against the entered
  // Maximum Marks. A scheme is only compatible when its ranges cover 0 through the
  // examination maximum without gaps and no range exceeds the maximum.
  const selectedSchemeIdRaw = watchSubject('gradingSchemeId');
  const maximumMarksRaw = watchSubject('maximumMarks');
  useEffect(() => {
    let scheme: GradingScheme | undefined;
    if (selectedSchemeIdRaw) {
      scheme = gradingSchemesQuery.data?.content?.find((s) => s.id === Number(selectedSchemeIdRaw));
    }
    const maximumMarks = maximumMarksRaw ? Number(maximumMarksRaw) : undefined;
    setSchemeCompatibilityError(validateGradingSchemeCompatibility(scheme, maximumMarks));
  }, [selectedSchemeIdRaw, maximumMarksRaw, gradingSchemesQuery.data]);

  const subjectOptions = (subjectsQuery.data?.content ?? []).map((subject: Subject) => ({
    value: String(subject.id),
    label: subject.code ? `${subject.name} (${subject.code})` : subject.name,
  }));
  const classOptions = (classesQuery.data?.content ?? []).map((cls: Class) => ({
    value: String(cls.id),
    label: cls.code ? `${cls.name} (${cls.code})` : cls.name,
  }));
  const streamOptions = (streamsQuery.data ?? []).map((stream: Stream) => ({
    value: String(stream.id),
    label: stream.code ? `${stream.name} (${stream.code})` : stream.name,
  }));
  const gradingSchemeOptions = (gradingSchemesQuery.data?.content ?? []).map((scheme: GradingScheme) => ({
    value: String(scheme.id),
    label: `${scheme.name}${scheme.isDefault ? ' (Default)' : ''}`,
  }));

  function validateGradingSchemeCompatibility(
    scheme: GradingScheme | undefined,
    maximumMarks: number | undefined,
  ): string | null {
    if (!scheme || !maximumMarks || maximumMarks <= 0) return null;
    const ranges = scheme.ranges ?? [];
    if (ranges.length === 0) {
      return `The grading scheme "${scheme.name}" has no grade ranges configured. Add grade ranges before assigning it.`;
    }
    const sorted = [...ranges].sort((a, b) => a.minMark - b.minMark);
    let previousMax: number | null = null;
    for (const r of sorted) {
      if (r.minMark == null || r.maxMark == null) {
        return `Grade "${r.grade}" is missing a minimum or maximum mark.`;
      }
      if (r.minMark < 0 || r.maxMark < 0) {
        return `Grade "${r.grade}" marks must be non-negative.`;
      }
      if (r.minMark > r.maxMark) {
        return `Grade "${r.grade}" minimum mark exceeds its maximum mark.`;
      }
      if (r.maxMark > maximumMarks) {
        return `Grade "${r.grade}" maximum mark (${r.maxMark}) exceeds the examination maximum marks (${maximumMarks}).`;
      }
      if (previousMax === null) {
        if (r.minMark !== 0) {
          return `Grading scale must start at 0 (grade "${r.grade}" starts at ${r.minMark}).`;
        }
      } else {
        if (r.minMark <= previousMax) {
          return `Overlapping grading ranges detected around grade "${r.grade}".`;
        }
        if (r.minMark !== previousMax + 1) {
          return `Gap in grading scale between ${previousMax} and ${r.minMark}. Every score from 0 to ${maximumMarks} must map to exactly one grade.`;
        }
      }
      previousMax = r.maxMark;
    }
    if (previousMax !== maximumMarks) {
      return `Grading scheme ranges must cover 0 through ${maximumMarks} without gaps (highest maximum is ${previousMax}).`;
    }
    return null;
  }

  const handleCreate = (values: ExaminationFormValues) => {
    setFormError(null);
    createMutation.mutate(values);
  };

  const handleUpdate = (values: ExaminationFormValues) => {
    setFormError(null);
    if (editExam) {
      updateMutation.mutate({ id: editExam.id, data: values });
    }
  };

  const handleStatusChange = (exam: ExaminationResponse, status: string) => {
    setFormError(null);
    statusMutation.mutate({ id: exam.id, status });
  };

  const handleSubjectCreate = (values: ExaminationSubjectFormValues) => {
    if (selectedExamination) {
      createSubjectMutation.mutate({ examinationId: selectedExamination.id, data: values });
    }
  };

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (!newOpen) {
      setEditExam(null);
      setFormError(null);
      reset({
        academicYearId: 0,
        name: '',
        typeId: 0,
        startDate: '',
        endDate: '',
        status: 'DRAFT',
        description: '',
      });
    }
  };

  const selectedYearId = watch('academicYearId');
  const selectedTypeId = watch('typeId');

  const yearOptions = (yearsQuery.data?.content ?? []).map((year: AcademicYear) => ({
    value: String(year.id),
    label: year.name,
  }));
  const typeOptions = (typesQuery.data?.content ?? []).map((type: ExaminationType) => ({
    value: String(type.id),
    label: type.name,
  }));

  const optionsError = yearsQuery.error || typesQuery.error;
  const retryOptions = () => {
    void Promise.all([yearsQuery.refetch(), typesQuery.refetch()]);
  };

  const openCreateDialog = () => {
    setEditExam(null);
    setFormError(null);
    reset({
      academicYearId: 0,
      name: '',
      typeId: 0,
      startDate: '',
      endDate: '',
      status: 'DRAFT',
      description: '',
    });
    setOpen(true);
  };

  const openEditDialog = (exam: ExaminationResponse) => {
    setEditExam(exam);
    setFormError(null);
    reset({
      academicYearId: exam.academicYearId,
      name: exam.name,
      typeId: exam.typeId,
      startDate: exam.startDate || '',
      endDate: exam.endDate || '',
      status: exam.status,
      description: exam.description || '',
    });
    setOpen(true);
  };

  const openSubjects = (exam: ExaminationResponse) => {
    setSelectedExamination(exam);
    setSubjectsOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Examinations</h1>
          <p className="mt-1 text-muted-foreground">Manage examinations and subjects</p>
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="mr-2 h-4 w-4" /> Add Examination
        </Button>
      </div>

      <Card className="p-4">
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
                    <TableHead>Type</TableHead>
                    <TableHead>Academic Year</TableHead>
                    <TableHead>Start Date</TableHead>
                    <TableHead>End Date</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-24">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center text-muted-foreground">
                        No examinations found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((exam: any) => (
                    <TableRow key={exam.id}>
                      <TableCell className="font-medium">{exam.name}</TableCell>
                      <TableCell>{exam.typeName}</TableCell>
                      <TableCell>{exam.academicYearName}</TableCell>
                      <TableCell>{exam.startDate ? new Date(exam.startDate).toLocaleDateString() : '-'}</TableCell>
                      <TableCell>{exam.endDate ? new Date(exam.endDate).toLocaleDateString() : '-'}</TableCell>
                      <TableCell>
                        <Badge variant={exam.status === 'COMPLETED' ? 'success' : exam.status === 'ONGOING' ? 'warning' : 'default'}>
                          {exam.status}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <Button variant="ghost" size="icon" onClick={() => openSubjects(exam)} title="View Subjects">
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" size="icon" onClick={() => openEditDialog(exam)} title="Edit">
                            <Edit className="h-4 w-4" />
                          </Button>
                          <select
                            className="text-xs border rounded px-1 py-1"
                            value=""
                            onChange={(e) => e.target.value && handleStatusChange(exam, e.target.value)}
                            title="Change Status"
                          >
                            <option value="">Status</option>
                            {statusOptions.map(opt => (
                              <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                          </select>
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

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent title={editExam ? 'Edit Examination' : 'Add Examination'} description={editExam ? 'Update examination details' : 'Create a new examination'}>
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Request failed</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          {optionsError && (
            <Alert variant="destructive">
              <AlertTitle>Unable to load options</AlertTitle>
              <AlertDescription>
                Could not load academic years or examination types.
                <Button type="button" variant="outline" size="sm" className="ml-3" onClick={retryOptions}>
                  Retry
                </Button>
              </AlertDescription>
            </Alert>
          )}
          <form
            onSubmit={editExam ? handleSubmit(handleUpdate) : handleSubmit(handleCreate)}
            className="space-y-4"
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <SearchableSelect
                  label="Academic Year *"
                  value={selectedYearId ? String(selectedYearId) : ''}
                  options={yearOptions}
                  placeholder="Choose an academic year"
                  loading={yearsQuery.isLoading}
                  onChange={(event) => setValue('academicYearId', Number(event.target.value) || 0, { shouldValidate: true })}
                  emptyMessage={
                    !yearsQuery.isLoading && yearOptions.length === 0
                      ? 'No academic years found. Create an academic year first.'
                      : 'No academic years found'
                  }
                  disabled={yearsQuery.isLoading || !!optionsError}
                />
                {errors.academicYearId && (
                  <p className="text-sm text-destructive">{errors.academicYearId.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <SearchableSelect
                  label="Type *"
                  value={selectedTypeId ? String(selectedTypeId) : ''}
                  options={typeOptions}
                  placeholder="Choose an examination type"
                  loading={typesQuery.isLoading}
                  onChange={(event) => setValue('typeId', Number(event.target.value) || 0, { shouldValidate: true })}
                  emptyMessage={
                    !typesQuery.isLoading && typeOptions.length === 0
                      ? 'No examination types found. Create an examination type first.'
                      : 'No examination types found'
                  }
                  disabled={typesQuery.isLoading || !!optionsError}
                />
                <div className="flex gap-2">
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      setCreateTypeOpen(true);
                      setNewTypeName('');
                      setNewTypeDescription('');
                      setNewTypeActive(true);
                    }}
                    disabled={typesQuery.isLoading || !!optionsError}
                  >
                    Create Examination Type
                  </Button>
                </div>
                {createTypeOpen && (
                  <div className="space-y-2 pt-2 border-t">
                    <label className="text-xs font-medium text-muted-foreground">New examination type</label>
                    <div className="space-y-2">
                      <Input
                        value={newTypeName}
                        onChange={(e) => setNewTypeName(e.target.value)}
                        placeholder="Type name"
                      />
                      <Input
                        value={newTypeDescription}
                        onChange={(e) => setNewTypeDescription(e.target.value)}
                        placeholder="Description (optional)"
                      />
                      <Select
                        value={String(newTypeActive)}
                        onChange={(e) => setNewTypeActive(e.target.value === 'true')}
                        options={[
                          { value: 'true', label: 'Active' },
                          { value: 'false', label: 'Inactive' },
                        ]}
                      />
                    </div>
                    <div className="flex justify-end gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => setCreateTypeOpen(false)}
                      >
                        Cancel
                      </Button>
                      <Button
                        type="button"
                        size="sm"
                        disabled={
                          createTypeMutation.isPending ||
                          !newTypeName.trim()
                        }
                        onClick={() => {
                          createTypeMutation.mutate({
                            name: newTypeName.trim(),
                            description: newTypeDescription.trim() || undefined,
                            active: newTypeActive,
                          });
                        }}
                      >
                        {createTypeMutation.isPending ? 'Creating...' : 'Create Type'}
                      </Button>
                    </div>
                  </div>
                )}
                {errors.typeId && (
                  <p className="text-sm text-destructive">{errors.typeId.message}</p>
                )}
              </div>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Name *</label>
              <Input {...register('name')} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Start Date</label>
                <Input type="date" {...register('startDate')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">End Date</label>
                <Input type="date" {...register('endDate')} />
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Status</label>
                <Select
                  {...register('status')}
                  options={statusOptions}
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Description</label>
                <Input {...register('description')} />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
              <Button
                type="submit"
                disabled={
                  createMutation.isPending ||
                  updateMutation.isPending ||
                  yearsQuery.isLoading ||
                  typesQuery.isLoading ||
                  !selectedYearId ||
                  !selectedTypeId
                }
              >
                {(createMutation.isPending || updateMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {createMutation.isPending || updateMutation.isPending ? 'Saving...' : editExam ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={subjectsOpen} onOpenChange={setSubjectsOpen}>
        <DialogContent title="Examination Subjects" description={selectedExamination ? `Subjects for ${selectedExamination.name}` : ''}>
          <div className="flex justify-end mb-4">
            <Button size="sm" onClick={() => setSubjectDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" /> Add Subject
            </Button>
          </div>
          {subjectsData?.content?.length === 0 && (
            <p className="text-center text-muted-foreground py-4">No subjects found</p>
          )}
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Subject</TableHead>
                  <TableHead>Class</TableHead>
                  <TableHead>Stream</TableHead>
                  <TableHead>Max Marks</TableHead>
                  <TableHead>Pass Mark</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {subjectsData?.content?.map((subject: any) => (
                  <TableRow key={subject.id}>
                    <TableCell className="font-medium">{subject.subjectName}</TableCell>
                    <TableCell>{subject.className}</TableCell>
                    <TableCell>{subject.streamName || '-'}</TableCell>
                    <TableCell>{subject.maximumMarks}</TableCell>
                    <TableCell>{subject.passMark}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={subjectDialogOpen} onOpenChange={setSubjectDialogOpen}>
        <DialogContent title="Add Examination Subject" description="Add a subject to this examination">
          <form onSubmit={handleSubmitSubject(handleSubjectCreate)} className="space-y-4">
            <div className="space-y-2">
              <SearchableSelect
                label="Subject *"
                value={watchSubject('subjectId') ? String(watchSubject('subjectId')) : ''}
                options={subjectOptions}
                placeholder="Search subjects by name or code..."
                loading={subjectsQuery.isLoading}
                onChange={(event) => {
                  const id = event.target.value;
                  setSubjectValue('subjectId', id ? Number(id) : undefined, { shouldValidate: true });
                }}
                emptyMessage={
                  !subjectsQuery.isLoading && subjectOptions.length === 0
                    ? 'No subjects found for this school. Create subjects first.'
                    : 'No subjects found'
                }
                disabled={subjectsQuery.isLoading || !!subjectsQuery.error}
              />
              {subjectErrors.subjectId && <p className="text-sm text-destructive">{subjectErrors.subjectId.message}</p>}
              {subjectsQuery.error && (
                <Alert variant="destructive">
                  <AlertTitle>Unable to load subjects</AlertTitle>
                  <AlertDescription>
                    {getApiErrorMessage(subjectsQuery.error, 'Please try again.')}
                    <Button type="button" variant="outline" size="sm" className="ml-3" onClick={() => void subjectsQuery.refetch()}>
                      Retry
                    </Button>
                  </AlertDescription>
                </Alert>
              )}
              {subjectsQuery.data?.content.length === 0 && !subjectsQuery.isLoading && !subjectsQuery.error && (
                <p className="text-sm text-muted-foreground">
                  This school has no subjects yet.{' '}
                  <Link className="underline" to="/school/subjects">Create subjects first.</Link>
                </p>
              )}
            </div>
            <div className="space-y-2">
              <SearchableSelect
                label="Class *"
                value={watchSubject('classId') ? String(watchSubject('classId')) : ''}
                options={classOptions}
                placeholder="Search classes by name or code..."
                loading={classesQuery.isLoading}
                onChange={(event) => {
                  const id = event.target.value;
                  setSubjectValue('classId', id ? Number(id) : undefined, { shouldValidate: true });
                }}
                emptyMessage={
                  !classesQuery.isLoading && classOptions.length === 0
                    ? 'No classes found for this school. Create classes first.'
                    : 'No classes found'
                }
                disabled={classesQuery.isLoading || !!classesQuery.error}
              />
              {subjectErrors.classId && <p className="text-sm text-destructive">{subjectErrors.classId.message}</p>}
              {classesQuery.error && (
                <Alert variant="destructive">
                  <AlertTitle>Unable to load classes</AlertTitle>
                  <AlertDescription>
                    {getApiErrorMessage(classesQuery.error, 'Please try again.')}
                    <Button type="button" variant="outline" size="sm" className="ml-3" onClick={() => void classesQuery.refetch()}>
                      Retry
                    </Button>
                  </AlertDescription>
                </Alert>
              )}
              {classesQuery.data?.content.length === 0 && !classesQuery.isLoading && !classesQuery.error && (
                <p className="text-sm text-muted-foreground">
                  This school has no classes yet.{' '}
                  <Link className="underline" to="/school/classes">Create classes first.</Link>
                </p>
              )}
            </div>
            <div className="space-y-2">
              <SearchableSelect
                label="Stream"
                value={watchSubject('streamId') ? String(watchSubject('streamId')) : ''}
                options={streamOptions}
                placeholder={selectedClassId ? 'Search streams...' : 'Select a class first'}
                loading={streamsQuery.isLoading}
                onChange={(event) => {
                  const id = event.target.value;
                  setSubjectValue('streamId', id ? Number(id) : undefined, { shouldValidate: true });
                }}
                emptyMessage={
                  !streamsQuery.isLoading && streamOptions.length === 0
                    ? 'No streams found for the selected class. You can continue without a stream.'
                    : 'No streams found'
                }
                disabled={!selectedClassId || streamsQuery.isLoading || !!streamsQuery.error}
              />
              {streamsQuery.error && (
                <Alert variant="destructive">
                  <AlertTitle>Unable to load streams</AlertTitle>
                  <AlertDescription>
                    {getApiErrorMessage(streamsQuery.error, 'Please try again.')}
                    <Button type="button" variant="outline" size="sm" className="ml-3" onClick={() => selectedClassId && void streamsQuery.refetch()}>
                      Retry
                    </Button>
                  </AlertDescription>
                </Alert>
              )}
              {!selectedClassId && (
                <p className="text-sm text-muted-foreground">Select a class above to load its streams.</p>
              )}
              {selectedClassId && streamsQuery.data?.length === 0 && !streamsQuery.isLoading && !streamsQuery.error && (
                <p className="text-sm text-muted-foreground">
                  The selected class has no streams. A stream is optional; you can continue without one.
                </p>
              )}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Maximum Marks *</label>
              <Input type="number" {...registerSubject('maximumMarks', { valueAsNumber: true })} />
              {subjectErrors.maximumMarks && <p className="text-sm text-destructive">{subjectErrors.maximumMarks.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Pass Mark *</label>
              <Input type="number" {...registerSubject('passMark', { valueAsNumber: true })} />
              {subjectErrors.passMark && <p className="text-sm text-destructive">{subjectErrors.passMark.message}</p>}
            </div>
            <div className="space-y-2">
              <SearchableSelect
                label="Grading Scheme"
                value={watchSubject('gradingSchemeId') ? String(watchSubject('gradingSchemeId')) : ''}
                options={gradingSchemeOptions}
                placeholder="Search grading schemes..."
                loading={gradingSchemesQuery.isLoading}
                onChange={(event) => {
                  const id = event.target.value;
                  setSubjectValue('gradingSchemeId', id ? Number(id) : undefined, { shouldValidate: true });
                }}
                emptyMessage="No grading schemes found"
                disabled={gradingSchemesQuery.isLoading || !!gradingSchemesQuery.error}
              />
              {gradingSchemesQuery.error && (
                <Alert variant="destructive">
                  <AlertTitle>Unable to load grading schemes</AlertTitle>
                  <AlertDescription>
                    {getApiErrorMessage(gradingSchemesQuery.error, 'Please try again.')}
                    <Button type="button" variant="outline" size="sm" className="ml-3" onClick={() => void gradingSchemesQuery.refetch()}>
                      Retry
                    </Button>
                  </AlertDescription>
                </Alert>
              )}
              {schemeCompatibilityError && (
                <Alert variant="destructive">
                  <AlertTitle>Incompatible grading scheme</AlertTitle>
                  <AlertDescription>{schemeCompatibilityError}</AlertDescription>
                </Alert>
              )}
              {gradingSchemesQuery.data?.content.length === 0 && !gradingSchemesQuery.isLoading && !gradingSchemesQuery.error && (
                <p className="text-sm text-muted-foreground">
                  No grading schemes are configured for this school.{' '}
                  <Link className="underline" to="/school/grading-schemes">Create a grading scheme first.</Link>
                </p>
              )}
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setSubjectDialogOpen(false)}>Cancel</Button>
              <Button
                type="submit"
                disabled={
                  createSubjectMutation.isPending ||
                  subjectsQuery.isLoading ||
                  classesQuery.isLoading ||
                  !watchSubject('subjectId') ||
                  !watchSubject('classId') ||
                  !!schemeCompatibilityError
                }
              >
                {createSubjectMutation.isPending ? 'Creating...' : 'Add Subject'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

