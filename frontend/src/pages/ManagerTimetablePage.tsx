import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse, PageResponse } from '@/types/api';

interface TeacherAssignment {
  id: number;
  teacherId: number;
  teacherName: string;
  subjectName: string;
  className: string;
  streamName: string | null;
  academicYearName: string;
}

interface TimetableEntry {
  id: number;
  teacherAssignmentId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  room: string | null;
  className: string;
  streamName: string | null;
  subjectName: string;
}

const days = [
  'Monday',
  'Tuesday',
  'Wednesday',
  'Thursday',
  'Friday',
  'Saturday',
  'Sunday',
];

export function ManagerTimetablePage() {
  const queryClient = useQueryClient();
  const assignmentsQuery = useQuery({
    queryKey: ['teacher-assignments', 'timetable'],
    queryFn: () =>
      unwrapApiResponse(
        api.get<ApiResponse<PageResponse<TeacherAssignment>>>(
          '/school/teacher-assignments',
          { params: { page: 0, size: 200 } }
        )
      ),
  });
  const timetableQuery = useQuery({
    queryKey: ['school-timetable'],
    queryFn: () =>
      unwrapApiResponse(
        api.get<ApiResponse<TimetableEntry[]>>('/school/timetables')
      ),
  });
  const createEntry = useMutation({
    mutationFn: (entry: {
      teacherAssignmentId: number;
      dayOfWeek: number;
      startTime: string;
      endTime: string;
      room?: string;
    }) =>
      unwrapApiResponse(
        api.post<ApiResponse<TimetableEntry>>('/school/timetables', entry)
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['school-timetable'] }),
  });
  const deleteEntry = useMutation({
    mutationFn: (id: number) =>
      unwrapApiResponse(api.delete<ApiResponse<void>>(`/school/timetables/${id}`)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['school-timetable'] }),
  });

  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    createEntry.mutate({
      teacherAssignmentId: Number(form.get('assignment')),
      dayOfWeek: Number(form.get('dayOfWeek')),
      startTime: String(form.get('startTime')),
      endTime: String(form.get('endTime')),
      room: String(form.get('room') || '').trim() || undefined,
    }, { onSuccess: () => formElement.reset() });
  };

  const error = assignmentsQuery.error ?? timetableQuery.error ?? createEntry.error ?? deleteEntry.error;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Timetable</h1>
        <p className="mt-1 text-muted-foreground">Schedule lessons from existing teacher class and subject assignments.</p>
      </div>
      {error && (
        <Alert variant="destructive">
          <AlertTitle>Timetable action failed</AlertTitle>
          <AlertDescription>{getApiErrorMessage(error, 'The timetable could not be loaded or changed.')}</AlertDescription>
        </Alert>
      )}
      <Card className="space-y-4 p-6">
        <h2 className="text-lg font-semibold">Add a lesson</h2>
        {assignmentsQuery.isLoading ? (
          <p className="text-sm text-muted-foreground">Loading teacher assignments…</p>
        ) : assignmentsQuery.isError ? (
          <p className="text-sm text-muted-foreground">Teacher assignments could not be loaded.</p>
        ) : assignmentsQuery.data?.content.length === 0 ? (
          <p className="text-sm text-muted-foreground">Create teacher assignments before scheduling lessons.</p>
        ) : assignmentsQuery.data ? (
          <form className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" onSubmit={submit}>
            <label className="space-y-1 text-sm">
              <span>Teacher assignment</span>
              <select className="w-full rounded-md border bg-background px-3 py-2" name="assignment" required defaultValue="">
                <option value="" disabled>Select assignment</option>
                {assignmentsQuery.data?.content.map((assignment) => (
                  <option key={assignment.id} value={assignment.id}>
                    {assignment.teacherName} · {assignment.className}{assignment.streamName ? ` / ${assignment.streamName}` : ''} · {assignment.subjectName} · {assignment.academicYearName}
                  </option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-sm">
              <span>Day</span>
              <select className="w-full rounded-md border bg-background px-3 py-2" name="dayOfWeek" defaultValue="1">
                {days.map((day, index) => <option key={day} value={index + 1}>{day}</option>)}
              </select>
            </label>
            <label className="space-y-1 text-sm">
              <span>Room (optional)</span>
              <input className="w-full rounded-md border bg-background px-3 py-2" name="room" maxLength={120} />
            </label>
            <label className="space-y-1 text-sm">
              <span>Start time</span>
              <input className="w-full rounded-md border bg-background px-3 py-2" name="startTime" type="time" required />
            </label>
            <label className="space-y-1 text-sm">
              <span>End time</span>
              <input className="w-full rounded-md border bg-background px-3 py-2" name="endTime" type="time" required />
            </label>
            <div className="flex items-end">
              <Button disabled={createEntry.isPending}>
                {createEntry.isPending ? 'Adding lesson…' : 'Add lesson'}
              </Button>
            </div>
          </form>
        ) : <p className="text-sm text-muted-foreground">Loading teacher assignments…</p>}
        {createEntry.isSuccess && <p role="status" className="text-sm text-green-700">Lesson scheduled.</p>}
      </Card>

      <Card className="space-y-4 p-6">
        <h2 className="text-lg font-semibold">Scheduled lessons</h2>
        {timetableQuery.isLoading && <p className="text-sm text-muted-foreground">Loading timetable…</p>}
        {timetableQuery.data?.length === 0 && <p className="text-sm text-muted-foreground">No lessons have been scheduled yet.</p>}
        {timetableQuery.data && timetableQuery.data.length > 0 && (
          <ul className="divide-y">
            {timetableQuery.data.map((entry) => (
              <li key={entry.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <p className="font-medium">
                    {days[entry.dayOfWeek - 1]} · {entry.startTime}–{entry.endTime} · {entry.subjectName} · {entry.className}
                    {entry.streamName ? ` / ${entry.streamName}` : ''}
                  </p>
                  {entry.room && <p className="text-sm text-muted-foreground">Room: {entry.room}</p>}
                </div>
                <Button
                  variant="outline"
                  disabled={deleteEntry.isPending}
                  onClick={() => deleteEntry.mutate(entry.id)}
                >
                  Remove
                </Button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
