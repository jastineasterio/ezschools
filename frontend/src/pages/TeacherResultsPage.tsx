import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Dialog, DialogContent, DialogClose } from '@/components/ui/Dialog';
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/components/ui/Table';
import { Badge } from '@/components/ui/Badge';
import { Separator } from '@/components/ui/Separator';
import { Skeleton } from '@/components/ui/Skeleton';
import { Save, Send } from 'lucide-react';
import {
  unwrapApiResponse,
  api,
} from '@/services/api';
import type { ApiResponse } from '@/types/api';

type ClassOption = { id: number; name: string; academicYearId: number; academicYearName: string };
type StreamOption = { id: number; name: string };
type SubjectOption = { id: number; name: string; code?: string | null };
type ExaminationOption = {
  id: number;
  examinationSubjectId: number;
  examinationName: string;
  typeName?: string | null;
  academicYearId: number;
  academicYearName?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  status: string;
  maximumMarks: number | null;
  passMark: number | null;
  subjectId: number;
  classId: number;
  streamId: number | null;
  gradingSchemeId: number | null;
};
type RosterStudent = {
  studentId: number;
  studentName: string;
  admissionNumber: string;
  resultId?: number | null;
  marksObtained?: number | null;
  resultStatus?: string | null;
  editable?: boolean;
  grade?: string | null;
};

function statusVariant(status: string) {
  switch (status) {
    case 'PUBLISHED':
      return 'success';
    case 'APPROVED':
      return 'secondary';
    case 'SUBMITTED':
      return 'warning';
    case 'REJECTED':
      return 'destructive';
    case 'DRAFT':
      return 'outline';
    default:
      return 'outline';
  }
}

export function TeacherResultsPage() {
  const [step, setStep] = useState<'classes' | 'streams' | 'exams' | 'marks'>('classes');
  const [selectedClass, setSelectedClass] = useState<ClassOption | null>(null);
  const [selectedStream, setSelectedStream] = useState<StreamOption | null>(null);
  const [selectedSubject, setSelectedSubject] = useState<SubjectOption | null>(null);
  const [selectedExam, setSelectedExam] = useState<ExaminationOption | null>(null);
  const [draftMarks, setDraftMarks] = useState<Map<number, number>>(new Map());
  const [confirmOpen, setConfirmOpen] = useState(false);

  const queryClient = useQueryClient();

  const { data: classes = [], isLoading: classesLoading } = useQuery({
    queryKey: ['teacher-results-classes'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<ClassOption[]>>('/school/teacher-workspace/results/classes')),
  });

  const streamsQuery = useQuery({
    queryKey: ['teacher-results-streams', selectedClass?.id],
    queryFn: async () => {
      if (!selectedClass) return [];
      const { data } = await api.get<ApiResponse<StreamOption[]>>(
        `/school/teacher-workspace/results/classes/${selectedClass.id}/streams`,
      );
      return Array.isArray(data.data) ? data.data : [];
    },
    enabled: !!selectedClass,
  });

  const { data: streams = [], isLoading: streamsLoading } = streamsQuery;

  const academicYearId = selectedClass?.academicYearId;

  const subjectsQuery = useQuery({
    queryKey: ['teacher-results-subjects', selectedClass?.id, selectedStream?.id, academicYearId],
    queryFn: async () => {
      if (!selectedClass || academicYearId == null) return [];
      const { data } = await api.get<ApiResponse<SubjectOption[]>>(
        `/school/teacher-workspace/results/subjects?classId=${selectedClass.id}&streamId=${selectedStream?.id ?? ''}&academicYearId=${academicYearId}`,
      );
      return Array.isArray(data.data) ? data.data : [];
    },
    enabled: !!selectedClass && academicYearId != null,
  });

  const { data: subjects = [], isLoading: subjectsLoading } = subjectsQuery;

  const examsQuery = useQuery({
    queryKey: ['teacher-results-exams', selectedClass?.id, selectedStream?.id, selectedSubject?.id, academicYearId],
    queryFn: async () => {
      if (!selectedClass || !selectedSubject || academicYearId == null) return [];
      const { data } = await api.get<ApiResponse<ExaminationOption[]>>(
        `/school/teacher-workspace/results/examinations?classId=${selectedClass.id}&streamId=${selectedStream?.id ?? ''}&subjectId=${selectedSubject.id}&academicYearId=${academicYearId}`,
      );
      return Array.isArray(data.data) ? data.data : [];
    },
    enabled: !!selectedClass && !!selectedSubject && academicYearId != null,
  });

  const { data: exams = [], isLoading: examsLoading } = examsQuery;

  const rosterQuery = useQuery({
    queryKey: ['teacher-results-roster', selectedExam?.examinationSubjectId],
    queryFn: async () => {
      if (!selectedExam) return [];
      const { data } = await api.get<ApiResponse<RosterStudent[]>>(
        `/school/teacher-workspace/examination-subjects/${selectedExam.examinationSubjectId}/roster`,
      );
      return Array.isArray(data.data) ? data.data : [];
    },
    enabled: !!selectedExam,
  });

  const { data: roster = [], isLoading: rosterLoading } = rosterQuery;

  const saveDraftMutation = useMutation({
    mutationFn: async (submissions: { studentId: number; marksObtained: number; remarks: string }[]) => {
      const { data } = await api.post<ApiResponse<any[]>>(
        `/school/teacher-workspace/examination-subjects/${selectedExam!.examinationSubjectId}/results`,
        submissions,
      );
      return data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teacher-results-roster', selectedExam?.examinationSubjectId] });
    },
  });

  const submitAllMutation = useMutation({
    mutationFn: async () => {
      const { data } = await api.post<ApiResponse<any[]>>(
        `/school/teacher-workspace/examination-subjects/${selectedExam!.examinationSubjectId}/submit`,
      );
      return data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teacher-results-roster', selectedExam?.examinationSubjectId] });
    },
  });

  const students = useMemo(() => {
    if (!roster) return [];
    return roster.map((s) => {
      const draft = draftMarks.get(s.studentId);
      const marks = s.marksObtained ?? draft;
      return {
        ...s,
        marksObtained: marks,
        editable: s.editable ?? true,
      };
    });
  }, [roster, draftMarks]);

  const summary = useMemo(() => {
    const total = students.length;
    const entered = students.filter((s) => s.marksObtained != null).length;
    const valid = students.filter((s) => typeof s.marksObtained === 'number').map((s) => s.marksObtained as number);
    const avg = valid.length ? valid.reduce((a, b) => a + b, 0) / valid.length : 0;
    const max = valid.length ? Math.max(...valid) : null;
    const min = valid.length ? Math.min(...valid) : null;
    return { total, entered, remaining: total - entered, avg, max, min };
  }, [students]);

  const maxMarks = selectedExam?.maximumMarks ?? 0;

  const handleMarksChange = (studentId: number, value: string) => {
    const num = value === '' ? undefined : Number(value);
    if (num != null && (isNaN(num) || num < 0 || num > maxMarks)) return;
    setDraftMarks((prev) => {
      const next = new Map(prev);
      if (num === undefined) next.delete(studentId);
      else next.set(studentId, num);
      return next;
    });
  };

  const handleSaveDraft = () => {
    const submissions = students
      .filter((s) => s.marksObtained != null)
      .map((s) => ({
        studentId: s.studentId,
        marksObtained: s.marksObtained as number,
        remarks: '',
      }));
    if (submissions.length === 0) return;
    saveDraftMutation.mutate(submissions);
  };

  const handleSubmitAll = () => setConfirmOpen(true);

  const confirmSubmit = () => {
    submitAllMutation.mutate();
    setConfirmOpen(false);
  };

  if (classesLoading) {
    return (
      <main className="mx-auto max-w-6xl space-y-6">
        <h1 className="text-3xl font-bold">Results</h1>
        <Card>
          <CardContent>
            <div className="space-y-3">
              {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-8 w-full" />)}
            </div>
          </CardContent>
        </Card>
      </main>
    );
  }

  if (!classes || classes.length === 0) {
    return (
      <main className="mx-auto max-w-6xl space-y-6">
        <h1 className="text-3xl font-bold">Results</h1>
        <Card>
          <CardHeader>
            <CardTitle>Results</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              No teaching assignments found. You must be assigned to a class, stream and subject to enter marks.
            </p>
          </CardContent>
        </Card>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-3xl font-bold">Results</h1>
        <p className="text-sm text-muted-foreground">
          Enter marks for your assigned classes, streams and subjects.
        </p>
      </header>

      {step === 'classes' && (
        <Card>
          <CardHeader>
            <CardTitle>Select Class</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-4">
              Choose one of your assigned classes to view its streams.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {classes.map((c) => (
                <Button
                  key={c.id}
                  onClick={() => {
                    setSelectedClass(c);
                    setSelectedStream(null);
                    setSelectedSubject(null);
                    setSelectedExam(null);
                    setDraftMarks(new Map());
                    setStep('streams');
                  }}
                  className="w-full justify-start text-left"
                  variant={selectedClass?.id === c.id ? 'default' : 'outline'}
                >
                  {c.name} {c.academicYearName ? `(${c.academicYearName})` : ''}
                </Button>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {step === 'streams' && selectedClass && (
        <>
          <Button variant="outline" size="sm" onClick={() => setStep('classes')}>
            ← Back to classes
          </Button>
          <Card>
            <CardHeader>
              <CardTitle>Select Stream — {selectedClass.name}</CardTitle>
            </CardHeader>
            <CardContent>
              {streamsLoading ? (
                <div className="space-y-3">
                  {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-8 w-full" />)}
                </div>
              ) : streams?.length === 0 ? (
                <p className="text-sm text-muted-foreground">No streams available for this class.</p>
              ) : (
                <>
                  <p className="text-sm text-muted-foreground mb-4">
                    Choose a stream you are assigned to teach.
                  </p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                    {streams.map((s) => (
                      <Button
                        key={s.id}
                        onClick={() => {
                          setSelectedStream(s);
                          setSelectedSubject(null);
                          setSelectedExam(null);
                          setDraftMarks(new Map());
                          setStep('exams');
                        }}
                        className="w-full justify-start text-left"
                        variant={selectedStream?.id === s.id ? 'default' : 'outline'}
                      >
                        {s.name}
                      </Button>
                    ))}
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </>
      )}

      {step === 'exams' && selectedClass && selectedStream && (
        <>
          <Button variant="outline" size="sm" onClick={() => setStep('streams')}>
            ← Back to streams
          </Button>
          <Card>
            <CardHeader>
              <CardTitle>
                {selectedClass.name} › {selectedStream.name}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <h2 className="text-sm font-semibold mb-1">Assigned Subjects</h2>
                  {subjectsLoading ? (
                    <div className="space-y-2">
                      {[...Array(2)].map((_, i) => <Skeleton key={i} className="h-8 w-1/2" />)}
                    </div>
                  ) : subjects?.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No subjects assigned for this class/stream.</p>
                  ) : (
                    <div className="flex gap-2 flex-wrap">
                      {subjects.map((s) => (
                        <Button
                          key={s.id}
                          onClick={() => {
                            setSelectedSubject(s);
                            setSelectedExam(null);
                            setDraftMarks(new Map());
                            setStep('marks');
                          }}
                          className="flex-1 justify-start text-left"
                          variant={selectedSubject?.id === s.id ? 'default' : 'outline'}
                        >
                          {s.name}
                        </Button>
                      ))}
                    </div>
                  )}
                </div>
                <Separator />
                <div>
                  <h2 className="text-sm font-semibold mb-1">Available Examinations</h2>
                  {examsLoading ? (
                    <div className="space-y-2">
                      {[...Array(2)].map((_, i) => <Skeleton key={i} className="h-8 w-1/2" />)}
                    </div>
                  ) : exams?.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                      No open examinations available for this subject.
                    </p>
                  ) : (
                    <div className="space-y-2">
                      {exams.map((e) => (
                        <Button
                          key={e.id}
                          onClick={() => {
                            setSelectedExam(e);
                            setDraftMarks(new Map());
                            setStep('marks');
                          }}
                          className="w-full justify-start text-left"
                          variant={selectedExam?.id === e.id ? 'default' : 'outline'}
                        >
                          <div className="flex justify-between w-full">
                            <span>{e.examinationName}</span>
                            <Badge variant={e.status === 'DRAFT' ? 'destructive' : 'secondary'}>
                              {e.status}
                            </Badge>
                          </div>
                          <div className="text-xs text-muted-foreground">
                            Max marks: {e.maximumMarks}
                          </div>
                        </Button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        </>
      )}

      {step === 'marks' && selectedClass && selectedStream && selectedSubject && selectedExam && (
        <>
          <Button variant="outline" size="sm" onClick={() => setStep('exams')}>
            ← Back to examinations
          </Button>
          <MarksEntryTable
            className={selectedClass.name}
            streamName={selectedStream.name}
            subjectName={selectedSubject.name}
            examinationName={selectedExam.examinationName}
            maxMarks={maxMarks}
            students={students}
            summary={summary}
            rosterLoading={rosterLoading}
            onMarksChange={handleMarksChange}
            onSaveDraft={handleSaveDraft}
            onSubmitAll={handleSubmitAll}
            isSaving={saveDraftMutation.isPending || submitAllMutation.isPending}
          />
          <ConfirmDialog
            open={confirmOpen}
            onOpenChange={setConfirmOpen}
            title="Confirm submission"
            description="Once submitted, results cannot be edited unless reopened by a Manager. Continue?"
            onConfirm={confirmSubmit}
          />
        </>
      )}
    </main>
  );
}

function MarksEntryTable({
  className,
  streamName,
  subjectName,
  examinationName,
  maxMarks,
  students,
  summary,
  rosterLoading,
  onMarksChange,
  onSaveDraft,
  onSubmitAll,
  isSaving,
}: {
  className: string;
  streamName: string;
  subjectName: string;
  examinationName: string;
  maxMarks: number;
  students: any[];
  summary: any;
  rosterLoading: boolean;
  onMarksChange: (studentId: number, value: string) => void;
  onSaveDraft: () => void;
  onSubmitAll: () => void;
  isSaving: boolean;
}) {
  const allEntered = students.every((s) => s.marksObtained != null && s.marksObtained !== '');
  const hasInvalid = students.some((s) => {
    const v = s.marksObtained;
    return v != null && v !== '' && (isNaN(Number(v)) || Number(v) < 0 || Number(v) > maxMarks);
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold">{examinationName}</h2>
          <p className="text-sm text-muted-foreground">
            {className} › {streamName} › {subjectName}
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={onSaveDraft}
            disabled={isSaving || summary.entered === 0}
          >
            <Save className="mr-2 h-4 w-4" /> Save Draft
          </Button>
          <Button
            onClick={onSubmitAll}
            disabled={!allEntered || hasInvalid}
          >
            <Send className="mr-2 h-4 w-4" /> Submit All Results
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <StatCard title="Total students" value={summary.total} />
        <StatCard title="Marks entered" value={summary.entered} />
        <StatCard title="Remaining" value={summary.remaining} />
        <StatCard title="Average" value={summary.avg > 0 ? summary.avg.toFixed(1) : '-'} />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Student Marks Entry</CardTitle>
        </CardHeader>
        <CardContent>
          {rosterLoading && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
            </div>
          )}
          {!rosterLoading && students.length === 0 && (
            <p className="text-sm text-muted-foreground">No eligible students in this stream.</p>
          )}
          {!rosterLoading && students.length > 0 && (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Admission No</TableHead>
                    <TableHead>Student</TableHead>
                    <TableHead>Marks</TableHead>
                    <TableHead>Grade</TableHead>
                    <TableHead>Remark</TableHead>
                    <TableHead className="w-24">Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {students.map((s) => {
                    const invalid =
                      s.marksObtained != null &&
                      s.marksObtained !== '' &&
                      (isNaN(Number(s.marksObtained)) ||
                        Number(s.marksObtained) < 0 ||
                        Number(s.marksObtained) > maxMarks);
                    return (
                      <TableRow key={s.studentId}>
                        <TableCell className="font-mono text-sm">{s.admissionNumber}</TableCell>
                        <TableCell className="font-medium">{s.studentName}</TableCell>
                        <TableCell>
                          <Input
                            type="text"
                            value={s.marksObtained == null || s.marksObtained === '' ? '' : String(s.marksObtained)}
                            onChange={(e) => onMarksChange(s.studentId, e.target.value)}
                            disabled={!s.editable}
                            placeholder="-"
                          />
                          {invalid && (
                            <p className="text-xs text-destructive mt-1">
                              Enter marks between 0 and {maxMarks}
                            </p>
                          )}
                        </TableCell>
                        <TableCell>
                          {s.grade || <span className="text-xs text-muted-foreground">-</span>}
                        </TableCell>
                        <TableCell>
                          {s.marksObtained == null || s.marksObtained === '' ? (
                            <span className="text-xs text-muted-foreground">-</span>
                          ) : (
                            <span className="text-xs">
                              {s.marksObtained >= maxMarks * 0.7 ? 'Excellent' : s.marksObtained >= maxMarks * 0.5 ? 'Good' : 'Needs improvement'}
                            </span>
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant={statusVariant(s.resultStatus || 'DRAFT')}>
                            {s.resultStatus || 'DRAFT'}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {summary.total > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Grade Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[summary.total].map(() => (
                <div key="placeholder" className="text-center text-sm text-muted-foreground">
                  No grades calculated yet
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function StatCard({ title, value }: { title: string; value: string | number }) {
  return (
    <Card className="p-3">
      <CardContent>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs text-muted-foreground">{title}</p>
            <p className="text-lg font-semibold">{value}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  onConfirm: () => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogClose onClick={() => onOpenChange(false)} />
        <h2 className="text-lg font-semibold">{title}</h2>
        <p className="text-sm text-muted-foreground">{description}</p>
        <div className="flex justify-end gap-2 mt-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={onConfirm}>Confirm</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
