import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { SearchableSelect } from '@/components/ui/SearchableSelect';
import { getAcademicYears, getClasses, getSubjects, getStreamsByClass, getStreamSubjectAssignments, saveStreamSubjectAssignments } from '@/services/academic';
import type { AcademicYear, Class, Stream, Subject } from '@/services/academic';
import { Loader2 } from 'lucide-react';

export function ManagerStreamSubjectsPage() {
  const [academicYearId, setAcademicYearId] = useState('');
  const [classId, setClassId] = useState('');
  const [streamId, setStreamId] = useState('');
  const [subjectIds, setSubjectIds] = useState<number[]>([]);
  const [subjectSearch, setSubjectSearch] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const yearsQuery = useQuery({
    queryKey: ['stream-subject-years'],
    queryFn: () => getAcademicYears(0, 500),
  });
  const classesQuery = useQuery({
    queryKey: ['stream-subject-classes'],
    queryFn: () => getClasses(0, 500),
  });
  const subjectsQuery = useQuery({
    queryKey: ['stream-subject-subjects'],
    queryFn: () => getSubjects(0, 500),
  });
  const streamsQuery = useQuery({
    queryKey: ['stream-subject-streams', classId],
    queryFn: () => getStreamsByClass(Number(classId)),
    enabled: Boolean(classId),
  });
  const assignmentsQuery = useQuery({
    queryKey: ['stream-subject-assignments', academicYearId, classId, streamId],
    queryFn: () => getStreamSubjectAssignments(Number(academicYearId), Number(classId), Number(streamId)),
    enabled: Boolean(academicYearId && classId && streamId),
  });

  useEffect(() => {
    setSubjectIds((assignmentsQuery.data ?? []).map((assignment) => assignment.subjectId));
  }, [assignmentsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () => saveStreamSubjectAssignments({
      academicYearId: Number(academicYearId),
      classId: Number(classId),
      streamId: Number(streamId),
      subjectIds,
    }),
    onSuccess: (saved) => {
      setSubjectIds(saved.map((assignment) => assignment.subjectId));
      setFormError(null);
      queryClient.invalidateQueries({ queryKey: ['stream-subject-assignments', academicYearId, classId, streamId] });
    },
    onError: (error: unknown) => setFormError(error instanceof Error ? error.message : 'Could not save stream subjects'),
  });

  const selectedAssignments = (assignmentsQuery.data ?? []).filter((assignment) => subjectIds.includes(assignment.subjectId));
  const availableSubjects = (subjectsQuery.data?.content ?? []).filter((subject: Subject) =>
    subject.status === 'ACTIVE'
  );
  const filteredSubjects = availableSubjects.filter((subject) =>
    `${subject.name} ${subject.code ?? ''}`.toLocaleLowerCase().includes(subjectSearch.trim().toLocaleLowerCase())
  );
  const optionsError = yearsQuery.error || classesQuery.error || subjectsQuery.error
    || streamsQuery.error || assignmentsQuery.error;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Stream Subject Configuration</h1>
        <p className="mt-1 text-muted-foreground">Set the shared subjects for a class stream and academic year.</p>
      </div>
      {optionsError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load configuration data</AlertTitle>
          <AlertDescription>
            Check your connection and retry.
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="ml-3"
              onClick={() => void Promise.all([
                yearsQuery.refetch(), classesQuery.refetch(), subjectsQuery.refetch(),
                streamsQuery.refetch(), assignmentsQuery.refetch(),
              ])}
            >
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      )}
      {formError && (
        <Alert variant="destructive">
          <AlertTitle>Save failed</AlertTitle>
          <AlertDescription>{formError}</AlertDescription>
        </Alert>
      )}

      <Card className="space-y-5 p-5">
        <div className="grid gap-4 md:grid-cols-3">
          <SearchableSelect
            label="Academic Year *"
            value={academicYearId}
            options={(yearsQuery.data?.content ?? []).map((year: AcademicYear) => ({
              value: String(year.id), label: year.name,
            }))}
            placeholder="Select an academic year"
            loading={yearsQuery.isLoading}
            onChange={(event) => { setAcademicYearId(event.target.value); setSubjectIds([]); setFormError(null); }}
            emptyMessage="No academic years found"
          />
          <SearchableSelect
            label="Class *"
            value={classId}
            options={(classesQuery.data?.content ?? []).map((item: Class) => ({
              value: String(item.id), label: item.code ? `${item.name} (${item.code})` : item.name,
            }))}
            placeholder="Select a class"
            loading={classesQuery.isLoading}
            onChange={(event) => { setClassId(event.target.value); setStreamId(''); setSubjectIds([]); setFormError(null); }}
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
            onChange={(event) => { setStreamId(event.target.value); setSubjectIds([]); setFormError(null); }}
            emptyMessage="No streams are configured for this class"
          />
        </div>
        {academicYearId && classId && streamId && (
          <section className="space-y-3">
            <h2 className="text-lg font-semibold">Subjects for this stream</h2>
            {assignmentsQuery.isLoading ? (
              <p className="text-sm text-muted-foreground">Loading current assignments...</p>
            ) : (
              <p className="text-sm text-muted-foreground">
                {selectedAssignments.length
                  ? `Currently assigned: ${selectedAssignments.map((item) => item.subjectName).join(', ')}`
                  : 'No subjects are currently assigned to this stream.'}
              </p>
            )}
            <Input
              type="search"
              placeholder="Search available subjects..."
              value={subjectSearch}
              onChange={(event) => setSubjectSearch(event.target.value)}
            />
            {availableSubjects.length === 0 ? (
              <p className="rounded-md border p-4 text-sm text-muted-foreground">No active subjects are available.</p>
            ) : filteredSubjects.length === 0 ? (
              <p className="rounded-md border p-4 text-sm text-muted-foreground">No subjects match your search.</p>
            ) : (
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {filteredSubjects.map((subject) => (
                  <label key={subject.id} className="flex items-center gap-3 rounded-md border p-3">
                    <input
                      type="checkbox"
                      checked={subjectIds.includes(subject.id)}
                      onChange={(event) => setSubjectIds((current) => event.target.checked
                        ? [...new Set([...current, subject.id])]
                        : current.filter((id) => id !== subject.id))}
                    />
                    <span>{subject.name}{subject.code ? ` (${subject.code})` : ''}</span>
                  </label>
                ))}
              </div>
            )}
            <div className="flex justify-end">
              <Button
                type="button"
                disabled={assignmentsQuery.isLoading || saveMutation.isPending || subjectsQuery.isLoading}
                onClick={() => saveMutation.mutate()}
              >
                {saveMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Save Subjects
              </Button>
            </div>
          </section>
        )}
      </Card>
    </div>
  );
}
