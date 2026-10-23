import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { SearchableSelect } from '@/components/ui/SearchableSelect';
import {
  getAcademicYears,
  getClasses,
  getStreamsByClass,
  getStreamSubjectAssignments,
} from '@/services/academic';
import type { AcademicYear, Class, Stream } from '@/services/academic';
import {
  createTeacherAssignmentsBatch,
  deleteTeacherAssignment,
  getTeacherAssignments,
  getTeachers,
  updateTeacherAssignment,
} from '@/services/people';
import type { Teacher, TeacherAssignment, UpdateTeacherAssignmentRequest } from '@/services/people';
import { Loader2, Pencil, Trash2 } from 'lucide-react';

export function ManagerTeacherAssignmentsPage() {
  const [academicYearId, setAcademicYearId] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [classId, setClassId] = useState('');
  const [streamId, setStreamId] = useState('');
  const [subjectIds, setSubjectIds] = useState<number[]>([]);
  const [editing, setEditing] = useState<TeacherAssignment | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const yearsQuery = useQuery({
    queryKey: ['teacher-assignment-years'],
    queryFn: () => getAcademicYears(0, 500),
  });
  const classesQuery = useQuery({
    queryKey: ['teacher-assignment-classes'],
    queryFn: () => getClasses(0, 500),
  });
  const teachersQuery = useQuery({
    queryKey: ['teacher-assignment-teachers'],
    queryFn: () => getTeachers(0, 500),
  });
  const streamsQuery = useQuery({
    queryKey: ['teacher-assignment-streams', classId],
    queryFn: () => getStreamsByClass(Number(classId)),
    enabled: Boolean(classId),
  });
  const subjectsQuery = useQuery({
    queryKey: ['teacher-assignment-stream-subjects', academicYearId, classId, streamId],
    queryFn: () => getStreamSubjectAssignments(Number(academicYearId), Number(classId), Number(streamId)),
    enabled: Boolean(academicYearId && classId && streamId),
  });
  const assignmentsQuery = useQuery({
    queryKey: ['teacher-assignments'],
    queryFn: () => getTeacherAssignments(0, 500),
  });

  const invalidateAssignments = () => {
    void queryClient.invalidateQueries({ queryKey: ['teacher-assignments'] });
  };
  const createMutation = useMutation({
    mutationFn: () => createTeacherAssignmentsBatch({
      teacherId: Number(teacherId),
      academicYearId: Number(academicYearId),
      classId: Number(classId),
      streamId: Number(streamId),
      subjectIds,
    }),
    onSuccess: () => {
      invalidateAssignments();
      setEditing(null);
      setSubjectIds([]);
      setFormError(null);
    },
    onError: (error: unknown) => setFormError(error instanceof Error ? error.message : 'Could not create assignments'),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateTeacherAssignmentRequest }) =>
      updateTeacherAssignment(id, data),
    onSuccess: () => {
      invalidateAssignments();
      setEditing(null);
      setSubjectIds([]);
      setFormError(null);
    },
    onError: (error: unknown) => setFormError(error instanceof Error ? error.message : 'Could not update assignment'),
  });
  const deleteMutation = useMutation({
    mutationFn: deleteTeacherAssignment,
    onSuccess: () => {
      invalidateAssignments();
      setFormError(null);
    },
    onError: (error: unknown) => setFormError(error instanceof Error ? error.message : 'Could not remove assignment'),
  });

  const activeTeachers = (teachersQuery.data?.content ?? []).filter(
    (teacher: Teacher) => teacher.employmentStatus === 'ACTIVE'
  );
  const configuredSubjects = subjectsQuery.data ?? [];
  const handleSave = () => {
    setFormError(null);
    if (!academicYearId || !teacherId || !classId || !streamId || subjectIds.length === 0) {
      setFormError('Select an academic year, teacher, class, stream, and at least one configured subject.');
      return;
    }
    if (editing) {
      updateMutation.mutate({
        id: editing.id,
        data: {
          teacherId: Number(teacherId),
          academicYearId: Number(academicYearId),
          classId: Number(classId),
          streamId: Number(streamId),
          subjectId: subjectIds[0],
          isClassTeacher: editing.isClassTeacher,
        },
      });
      return;
    }
    createMutation.mutate();
  };
  const editAssignment = (assignment: TeacherAssignment) => {
    setEditing(assignment);
    setAcademicYearId(String(assignment.academicYearId));
    setTeacherId(String(assignment.teacherId));
    setClassId(String(assignment.classId));
    setStreamId(assignment.streamId ? String(assignment.streamId) : '');
    setSubjectIds([assignment.subjectId]);
    setFormError(null);
  };
  const cancelEdit = () => {
    setEditing(null);
    setSubjectIds([]);
    setFormError(null);
  };
  const optionsError = yearsQuery.error || classesQuery.error || teachersQuery.error
    || streamsQuery.error || subjectsQuery.error || assignmentsQuery.error;
  const loading = yearsQuery.isLoading || classesQuery.isLoading || teachersQuery.isLoading;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Teacher Assignments</h1>
        <p className="mt-1 text-muted-foreground">Assign teachers to configured subjects in each class stream.</p>
      </div>
      {optionsError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load assignment data</AlertTitle>
          <AlertDescription>
            Check your connection and retry.
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="ml-3"
              onClick={() => void Promise.all([
                yearsQuery.refetch(), classesQuery.refetch(), teachersQuery.refetch(),
                streamsQuery.refetch(), subjectsQuery.refetch(), assignmentsQuery.refetch(),
              ])}
            >
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      )}
      {formError && (
        <Alert variant="destructive">
          <AlertTitle>Assignment request failed</AlertTitle>
          <AlertDescription>{formError}</AlertDescription>
        </Alert>
      )}
      <Card className="space-y-5 p-5">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <SearchableSelect
            label="Academic Year *"
            value={academicYearId}
            options={(yearsQuery.data?.content ?? []).map((year: AcademicYear) => ({
              value: String(year.id), label: year.name,
            }))}
            placeholder="Select an academic year"
            loading={yearsQuery.isLoading}
            onChange={(event) => { setAcademicYearId(event.target.value); setSubjectIds([]); }}
            emptyMessage="No academic years found"
          />
          <SearchableSelect
            label="Teacher *"
            value={teacherId}
            options={activeTeachers.map((teacher: Teacher) => ({
              value: String(teacher.userId),
              label: `${teacher.firstName ?? ''} ${teacher.lastName ?? ''}${teacher.employeeNumber ? ` (${teacher.employeeNumber})` : ''}`.trim(),
            }))}
            placeholder="Select a teacher"
            loading={teachersQuery.isLoading}
            onChange={(event) => setTeacherId(event.target.value)}
            emptyMessage="No active teacher profiles found"
          />
          <SearchableSelect
            label="Class *"
            value={classId}
            options={(classesQuery.data?.content ?? []).map((item: Class) => ({
              value: String(item.id), label: item.code ? `${item.name} (${item.code})` : item.name,
            }))}
            placeholder="Select a class"
            loading={classesQuery.isLoading}
            onChange={(event) => { setClassId(event.target.value); setStreamId(''); setSubjectIds([]); }}
            emptyMessage="No classes found"
          />
          <SearchableSelect
            label="Stream *"
            value={streamId}
            options={(streamsQuery.data ?? []).map((stream: Stream) => ({
              value: String(stream.id), label: stream.code ? `${stream.name} (${stream.code})` : stream.name,
            }))}
            placeholder={classId ? 'Select a stream' : 'Select a class first'}
            loading={streamsQuery.isLoading}
            disabled={!classId}
            onChange={(event) => { setStreamId(event.target.value); setSubjectIds([]); }}
            emptyMessage="No streams are configured for this class"
          />
        </div>
        {academicYearId && classId && streamId && (
          <section className="space-y-3">
            <h2 className="font-semibold">{editing ? 'Edit teaching assignment' : 'Subjects to assign'}</h2>
            {subjectsQuery.isLoading ? (
              <p className="text-sm text-muted-foreground">Loading configured subjects...</p>
            ) : configuredSubjects.length === 0 ? (
              <p className="rounded-md border p-4 text-sm text-muted-foreground">
                Configure subjects for this stream before assigning teachers.
              </p>
            ) : (
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {configuredSubjects.map((subject) => (
                  <label key={subject.subjectId} className="flex items-center gap-3 rounded-md border p-3">
                    <input
                      type={editing ? 'radio' : 'checkbox'}
                      name="teacher-subjects"
                      checked={subjectIds.includes(subject.subjectId)}
                      onChange={(event) => setSubjectIds((current) => editing
                        ? [subject.subjectId]
                        : event.target.checked
                          ? [...new Set([...current, subject.subjectId])]
                          : current.filter((id) => id !== subject.subjectId))}
                    />
                    <span>{subject.subjectName}{subject.subjectCode ? ` (${subject.subjectCode})` : ''}</span>
                  </label>
                ))}
              </div>
            )}
          </section>
        )}
        <div className="flex justify-end gap-2">
          {editing && (
            <Button type="button" variant="outline" onClick={cancelEdit}>
              Cancel edit
            </Button>
          )}
          <Button
            type="button"
            disabled={loading || createMutation.isPending || updateMutation.isPending
              || configuredSubjects.length === 0}
            onClick={handleSave}
          >
            {(createMutation.isPending || updateMutation.isPending) && (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            )}
            {editing ? 'Update Assignment' : 'Assign Teacher'}
          </Button>
        </div>
      </Card>

      <Card className="space-y-3 p-5">
        <h2 className="text-lg font-semibold">Current teaching assignments</h2>
        {assignmentsQuery.isLoading ? (
          <p className="text-sm text-muted-foreground">Loading assignments...</p>
        ) : assignmentsQuery.data?.content.length ? (
          <ul className="divide-y">
            {assignmentsQuery.data.content.map((assignment: TeacherAssignment) => (
              <li key={assignment.id} className="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-medium">{assignment.teacherName || `Teacher ${assignment.teacherId}`}</p>
                  <p className="text-sm text-muted-foreground">
                    {assignment.academicYearName} · {assignment.className}
                    {assignment.streamName ? `, Stream ${assignment.streamName}` : ''}
                    {' · '}{assignment.subjectName}
                  </p>
                </div>
                <div className="flex gap-2">
                  <Button type="button" variant="outline" size="sm" onClick={() => editAssignment(assignment)}>
                    <Pencil className="mr-2 h-4 w-4" /> Edit
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={deleteMutation.isPending}
                    onClick={() => deleteMutation.mutate(assignment.id)}
                  >
                    <Trash2 className="mr-2 h-4 w-4" /> Remove
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-muted-foreground">No teaching assignments have been configured.</p>
        )}
      </Card>
    </div>
  );
}
