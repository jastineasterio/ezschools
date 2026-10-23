import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';
import { useAuth } from '@/store/AuthContext';

interface TeacherAssignment {
  id: number;
  className: string;
  streamName: string | null;
  subjectName: string;
  academicYearName: string;
  classTeacher?: boolean;
  studentCount?: number;
}

interface TimetableLesson {
  id: number;
  startTime: string;
  endTime: string;
  room: string | null;
  className: string;
  streamName: string | null;
  subjectName: string;
}

interface UpcomingExamination {
  examinationSubjectId: number;
  examinationId: number;
  examinationName: string;
  startDate: string | null;
  endDate: string | null;
  status: string;
  subjectName: string;
  className: string;
  streamName: string | null;
  maximumMarks: number;
}

interface TeacherDashboard {
  today: string;
  timeZone: string;
  assignments: TeacherAssignment[];
  timetable: TimetableLesson[];
  upcomingExaminations: UpcomingExamination[];
}

interface AttendanceStudent {
  studentId: number;
  studentName: string;
  admissionNumber: string | null;
  streamName: string | null;
  status: string | null;
  remarks: string | null;
}

interface ExamStudent {
  studentId: number;
  studentName: string;
  admissionNumber: string | null;
  resultId: number | null;
  marksObtained: number | null;
  resultStatus: string | null;
  editable: boolean;
}

const attendanceStatuses = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'];

function formatDate(value: string | null) {
  if (!value) return 'Date not set';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeZone: 'UTC' })
    .format(new Date(`${value}T00:00:00Z`));
}

function AttendancePanel({
  assignment,
  date,
}: {
  assignment: TeacherAssignment;
  date: string;
}) {
  const queryClient = useQueryClient();
  const [statuses, setStatuses] = useState<Record<number, string>>({});
  const rosterQuery = useQuery({
    queryKey: ['teacher-attendance-roster', assignment.id, date],
    enabled: Boolean(date),
    queryFn: () =>
      unwrapApiResponse(
        api.get<ApiResponse<AttendanceStudent[]>>(
          `/school/teacher-workspace/assignments/${assignment.id}/attendance`,
          { params: { date } }
        )
      ),
  });
  const recordAttendance = useMutation({
    mutationFn: () =>
      unwrapApiResponse(
        api.post<ApiResponse<unknown[]>>(
          `/school/teacher-workspace/assignments/${assignment.id}/attendance`,
          (rosterQuery.data ?? [])
            .filter((student) => !student.status)
            .map((student) => ({
              studentId: student.studentId,
              status: statuses[student.studentId],
            })),
          { params: { date } }
        )
      ),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ['teacher-attendance-roster', assignment.id, date],
      }),
  });

  const unrecordedStudents = rosterQuery.data?.filter((student) => !student.status) ?? [];
  const allMarked = unrecordedStudents.every((student) => Boolean(statuses[student.studentId]));

  return (
    <Card className="space-y-4 p-5">
      <div>
        <h3 className="font-semibold">{assignment.className}{assignment.streamName ? ` · ${assignment.streamName}` : ''}</h3>
        <p className="text-sm text-muted-foreground">{assignment.subjectName} · {date}</p>
      </div>
      {rosterQuery.isLoading && <p className="text-sm text-muted-foreground">Loading class roster…</p>}
      {rosterQuery.isError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load attendance roster</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(rosterQuery.error, 'The class roster could not be loaded.')}
          </AlertDescription>
        </Alert>
      )}
      {rosterQuery.data && (
        <>
          {rosterQuery.data.length === 0 ? (
            <p className="text-sm text-muted-foreground">No enrolled students are assigned to this class for the academic year.</p>
          ) : (
            <ul className="divide-y">
              {rosterQuery.data.map((student) => (
                <li key={student.studentId} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div>
                    <p className="font-medium">{student.studentName}</p>
                    <p className="text-xs text-muted-foreground">
                      {[student.admissionNumber, student.streamName].filter(Boolean).join(' · ')}
                    </p>
                  </div>
                  {student.status ? (
                    <span className="text-sm font-medium">{student.status} · recorded</span>
                  ) : (
                    <select
                      aria-label={`Attendance for ${student.studentName}`}
                      className="rounded-md border bg-background px-3 py-2 text-sm"
                      value={statuses[student.studentId] ?? ''}
                      onChange={(event) => setStatuses((current) => ({
                        ...current,
                        [student.studentId]: event.target.value,
                      }))}
                    >
                      <option value="">Select status</option>
                      {attendanceStatuses.map((status) => (
                        <option key={status} value={status}>{status}</option>
                      ))}
                    </select>
                  )}
                </li>
              ))}
            </ul>
          )}
          {recordAttendance.isError && (
            <Alert variant="destructive">
              <AlertTitle>Attendance was not recorded</AlertTitle>
              <AlertDescription>
                {getApiErrorMessage(recordAttendance.error, 'Please review the roster and try again.')}
              </AlertDescription>
            </Alert>
          )}
          {rosterQuery.data.length > 0 && (
            <Button
              disabled={unrecordedStudents.length === 0 || !allMarked || recordAttendance.isPending}
              onClick={() => recordAttendance.mutate()}
            >
              {recordAttendance.isPending ? 'Saving attendance…' : 'Save attendance'}
            </Button>
          )}
        </>
      )}
    </Card>
  );
}

function ExaminationMarksPanel({ examination }: { examination: UpcomingExamination }) {
  const queryClient = useQueryClient();
  const [marks, setMarks] = useState<Record<number, string>>({});
  const rosterKey = ['teacher-examination-roster', examination.examinationSubjectId];
  const rosterQuery = useQuery({
    queryKey: rosterKey,
    queryFn: () =>
      unwrapApiResponse(
        api.get<ApiResponse<ExamStudent[]>>(
          `/school/teacher-workspace/examination-subjects/${examination.examinationSubjectId}/roster`
        )
      ),
  });
  const saveMarks = useMutation({
    mutationFn: (submissions: { studentId: number; marksObtained: number }[]) =>
      unwrapApiResponse(
        api.post<ApiResponse<unknown[]>>(
          `/school/teacher-workspace/examination-subjects/${examination.examinationSubjectId}/results`,
          submissions
        )
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: rosterKey }),
  });
  const submitResult = useMutation({
    mutationFn: (resultId: number) =>
      unwrapApiResponse(
        api.post<ApiResponse<unknown>>(`/school/teacher-workspace/results/${resultId}/submit`)
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: rosterKey }),
  });
  const editableStudents = rosterQuery.data?.filter((student) => !student.resultId || student.editable) ?? [];
  const submissions = editableStudents.flatMap((student) => {
    const value = marks[student.studentId] ?? (student.marksObtained == null ? '' : String(student.marksObtained));
    if (!value.trim()) return [];
    const parsed = Number(value);
    return Number.isInteger(parsed) && parsed >= 0 && parsed <= examination.maximumMarks
      ? [{ studentId: student.studentId, marksObtained: parsed }]
      : [];
  });

  return (
    <Card className="space-y-4 p-5">
      <div>
        <h3 className="font-semibold">{examination.examinationName} · {examination.subjectName}</h3>
        <p className="text-sm text-muted-foreground">
          {examination.className}{examination.streamName ? ` · ${examination.streamName}` : ''}
          {' · '}Maximum {examination.maximumMarks} marks
        </p>
      </div>
      {rosterQuery.isLoading && <p className="text-sm text-muted-foreground">Loading examination roster…</p>}
      {rosterQuery.isError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load examination roster</AlertTitle>
          <AlertDescription>{getApiErrorMessage(rosterQuery.error, 'The roster could not be loaded.')}</AlertDescription>
        </Alert>
      )}
      {rosterQuery.data && (
        <>
          {rosterQuery.data.length === 0 ? (
            <p className="text-sm text-muted-foreground">No enrolled students are eligible for this examination subject.</p>
          ) : (
            <ul className="divide-y">
              {rosterQuery.data.map((student) => (
                <li key={student.studentId} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div>
                    <p className="font-medium">{student.studentName}</p>
                    <p className="text-xs text-muted-foreground">
                      {[student.admissionNumber, student.resultStatus].filter(Boolean).join(' · ')}
                    </p>
                  </div>
                  {student.resultId == null || student.editable ? (
                    <input
                      aria-label={`Marks for ${student.studentName}`}
                      className="w-32 rounded-md border bg-background px-3 py-2 text-sm"
                      type="number"
                      min={0}
                      max={examination.maximumMarks}
                      step={1}
                      value={marks[student.studentId] ?? (student.marksObtained == null ? '' : String(student.marksObtained))}
                      onChange={(event) => setMarks((current) => ({
                        ...current,
                        [student.studentId]: event.target.value,
                      }))}
                    />
                  ) : (
                    <div className="flex items-center gap-3">
                      <span className="text-sm">{student.marksObtained ?? 'No marks'} · {student.resultStatus}</span>
                    </div>
                  )}
                  {student.resultId && student.editable && (
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={submitResult.isPending}
                      onClick={() => {
                        if (student.resultId !== null) submitResult.mutate(student.resultId);
                      }}
                    >
                      Submit for approval
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
          {saveMarks.isError && (
            <Alert variant="destructive">
              <AlertTitle>Marks were not saved</AlertTitle>
              <AlertDescription>{getApiErrorMessage(saveMarks.error, 'Review marks and try again.')}</AlertDescription>
            </Alert>
          )}
          {submitResult.isError && (
            <Alert variant="destructive">
              <AlertTitle>Result was not submitted</AlertTitle>
              <AlertDescription>{getApiErrorMessage(submitResult.error, 'The result could not be submitted.')}</AlertDescription>
            </Alert>
          )}
          {saveMarks.isSuccess && <p role="status" className="text-sm text-green-700">Marks saved as drafts.</p>}
          {submissions.length > 0 && (
            <Button disabled={saveMarks.isPending} onClick={() => saveMarks.mutate(submissions)}>
              {saveMarks.isPending ? 'Saving marks…' : 'Save draft marks'}
            </Button>
          )}
        </>
      )}
    </Card>
  );
}

export function TeacherDashboardPage() {
  const { user } = useAuth();
  const name = user ? `${user.firstName} ${user.lastName}` : 'Teacher';
  const dashboardQuery = useQuery({
    queryKey: ['teacher-workspace'],
    queryFn: () =>
      unwrapApiResponse(
        api.get<ApiResponse<TeacherDashboard>>('/school/teacher-workspace')
      ),
  });
  const [selectedAssignmentId, setSelectedAssignmentId] = useState('');
  const [attendanceDate, setAttendanceDate] = useState('');
  const [selectedExaminationSubjectId, setSelectedExaminationSubjectId] = useState<number | null>(null);
  const dashboard = dashboardQuery.data;
  const selectedAssignment = dashboard?.assignments.find(
    (item) => item.id === Number(selectedAssignmentId)
  );

  return (
    <main className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Welcome, {name}</h1>
        <p className="mt-1 text-muted-foreground">Teacher Dashboard</p>
      </div>

      {dashboardQuery.isLoading && <p role="status" className="text-muted-foreground">Loading your school workspace…</p>}
      {dashboardQuery.isError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load teacher workspace</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(dashboardQuery.error, 'Your school workspace could not be loaded.')}
            <Button className="ml-3" size="sm" variant="outline" onClick={() => void dashboardQuery.refetch()}>
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {dashboard && (
        <>
          <Card className="space-y-3 p-6">
            <h2 className="text-lg font-semibold">Assigned classes and subjects</h2>
            {dashboard.assignments.length === 0 ? (
              <p className="text-sm text-muted-foreground">You do not have any active class or subject assignments.</p>
            ) : (
              <ul className="grid gap-3 sm:grid-cols-2">
                {dashboard.assignments.map((assignment) => (
                  <li key={assignment.id} className="rounded-md border p-4">
                    <p className="font-semibold">
                      {assignment.className}{assignment.streamName ? ` · ${assignment.streamName}` : ''}
                    </p>
                    <p className="text-sm text-muted-foreground">{assignment.subjectName}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {assignment.academicYearName}
                      {assignment.studentCount !== undefined ? ` · ${assignment.studentCount} enrolled students` : ''}
                      {assignment.classTeacher ? ' · Class teacher' : ''}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card className="space-y-3 p-6">
            <h2 className="text-lg font-semibold">Today's timetable</h2>
            <p className="text-xs text-muted-foreground">
              {formatDate(dashboard.today)} · {dashboard.timeZone}
            </p>
            {dashboard.timetable.length === 0 ? (
              <p className="text-sm text-muted-foreground">No lessons are scheduled for today.</p>
            ) : (
              <ul className="divide-y">
                {dashboard.timetable.map((lesson) => (
                  <li key={lesson.id} className="flex flex-wrap justify-between gap-2 py-3">
                    <div>
                      <p className="font-medium">{lesson.subjectName} · {lesson.className}{lesson.streamName ? ` · ${lesson.streamName}` : ''}</p>
                      {lesson.room && <p className="text-sm text-muted-foreground">{lesson.room}</p>}
                    </div>
                    <p className="text-sm tabular-nums">{lesson.startTime}–{lesson.endTime}</p>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card className="space-y-4 p-6">
            <div>
              <h2 className="text-lg font-semibold">Attendance tasks</h2>
              <p className="text-sm text-muted-foreground">Record attendance for students in your assigned classes.</p>
            </div>
            {dashboard.assignments.length === 0 ? (
              <p className="text-sm text-muted-foreground">No assigned classes are available for attendance.</p>
            ) : (
              <div className="flex flex-wrap gap-3">
                <select
                  aria-label="Assigned class and subject"
                  className="min-w-64 rounded-md border bg-background px-3 py-2 text-sm"
                  value={selectedAssignmentId}
                  onChange={(event) => {
                    setSelectedAssignmentId(event.target.value);
                    setAttendanceDate(dashboard.today);
                  }}
                >
                  <option value="">Choose an assigned class</option>
                  {dashboard.assignments.map((assignment) => (
                    <option key={assignment.id} value={assignment.id}>
                      {assignment.className}{assignment.streamName ? ` · ${assignment.streamName}` : ''} — {assignment.subjectName}
                    </option>
                  ))}
                </select>
                {selectedAssignmentId && (
                  <input
                    aria-label="Attendance date"
                    className="rounded-md border bg-background px-3 py-2 text-sm"
                    type="date"
                    value={attendanceDate || dashboard.today}
                    onChange={(event) => setAttendanceDate(event.target.value)}
                  />
                )}
              </div>
            )}
            {selectedAssignment && (
              <AttendancePanel
                key={`${selectedAssignment.id}-${attendanceDate || dashboard.today}`}
                assignment={selectedAssignment}
                date={attendanceDate || dashboard.today}
              />
            )}
          </Card>

          <Card className="space-y-3 p-6">
            <h2 className="text-lg font-semibold">Upcoming examinations</h2>
            {dashboard.upcomingExaminations.length === 0 ? (
              <p className="text-sm text-muted-foreground">There are no upcoming examinations for your assigned classes and subjects.</p>
            ) : (
              <ul className="divide-y">
                {dashboard.upcomingExaminations.map((examination, index) => (
                  <li key={`${examination.examinationId}-${examination.className}-${examination.subjectName}-${index}`} className="py-3">
                    <p className="font-medium">{examination.examinationName} · {examination.subjectName}</p>
                    <p className="text-sm text-muted-foreground">
                      {examination.className}{examination.streamName ? ` · ${examination.streamName}` : ''}
                      {' · '}{formatDate(examination.startDate)}{examination.endDate && examination.endDate !== examination.startDate ? ` – ${formatDate(examination.endDate)}` : ''}
                    </p>
                    <Button
                      className="mt-2"
                      size="sm"
                      variant="outline"
                      onClick={() => setSelectedExaminationSubjectId(
                        selectedExaminationSubjectId === examination.examinationSubjectId
                          ? null
                          : examination.examinationSubjectId
                      )}
                    >
                      {selectedExaminationSubjectId === examination.examinationSubjectId ? 'Close marks' : 'Enter or edit marks'}
                    </Button>
                    {selectedExaminationSubjectId === examination.examinationSubjectId && (
                      <div className="mt-4">
                        <ExaminationMarksPanel examination={examination} />
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </>
      )}
    </main>
  );
}
