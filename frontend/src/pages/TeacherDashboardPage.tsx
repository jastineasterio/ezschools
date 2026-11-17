import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';
import { useAuth } from '@/store/AuthContext';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import { Users, BookOpen, GraduationCap, ClipboardCheck, FileText, Calendar } from 'lucide-react';
import { StatCard } from '@/components/dashboard/StatCard';
import { SectionCard } from '@/components/dashboard/EmptyState';
import { formatDate, formatNumber } from '@/lib/format';

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
  assignedClassesCount?: number;
  assignedSubjectsCount?: number;
  totalUniqueStudents?: number;
  upcomingExaminationsCount?: number;
  attendanceTasksCount?: number;
  unreadNotificationsCount?: number;
  announcementsCount?: number;
  performanceChart?: Array<{ name: string; averageMarks: number; passRate: number }>;
  attendanceChart?: Array<{ date: string; present: number; absent: number; late: number; excused: number }>;
  classPerformance?: Array<{ className: string; streamName: string | null; averageMarks: number; studentCount: number }>;
  subjectPerformance?: Array<{ subjectName: string; averageMarks: number; studentCount: number }>;
  studentDistribution?: Array<{ name: string; count: number }>;
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

const COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

const attendanceStatuses = ['Present', 'Absent', 'Late', 'Excused'];

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
        <h3 className="font-semibold">
          {assignment.className}
          {assignment.streamName ? ` · ${assignment.streamName}` : ''}
        </h3>
        <p className="text-sm text-muted-foreground">
          {assignment.subjectName} · {date}
        </p>
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
                      className="input w-auto min-w-[100px]"
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
                      className="input w-32"
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
          {saveMarks.isSuccess && <p role="status" className="text-sm text-emerald-600 dark:text-emerald-400">Marks saved as drafts.</p>}
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

  const performanceChart = dashboard?.performanceChart ?? [];
  const attendanceChart = dashboard?.attendanceChart ?? [];
  const classPerformance = dashboard?.classPerformance ?? [];
  const subjectPerformance = dashboard?.subjectPerformance ?? [];
  const studentDistribution = dashboard?.studentDistribution ?? [];

  return (
    <main className="page space-y-6">
      <div className="page-header">
        <div>
          <h1 className="page-title">Welcome, {name}</h1>
          <p className="page-subtitle">Teacher workspace and class overview</p>
        </div>
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
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              label="Assigned Classes"
              value={formatNumber(dashboard.assignedClassesCount ?? dashboard.assignments.length)}
              icon={Users}
              hint="Classes you are assigned to teach"
            />
            <StatCard
              label="Assigned Subjects"
              value={formatNumber(dashboard.assignedSubjectsCount ?? new Set(dashboard.assignments.map(a => a.subjectName)).size)}
              icon={BookOpen}
              hint="Subjects you teach"
            />
            <StatCard
              label="Total Students"
              value={formatNumber(dashboard.totalUniqueStudents ?? dashboard.assignments.reduce((s,a) => s + (a.studentCount || 0), 0))}
              icon={GraduationCap}
              hint="Unique students across assignments"
            />
            <StatCard
              label="Upcoming Exams"
              value={formatNumber(dashboard.upcomingExaminationsCount ?? dashboard.upcomingExaminations.length)}
              icon={FileText}
              hint="Examinations to prepare for"
            />
            <StatCard
              label="Attendance Tasks"
              value={formatNumber(dashboard.attendanceTasksCount ?? dashboard.assignments.length)}
              icon={ClipboardCheck}
              hint="Classes needing attendance"
            />
            <StatCard
              label="Unread Notifications"
              value={formatNumber(dashboard.unreadNotificationsCount ?? 0)}
              icon={Calendar}
              hint="Notifications requiring attention"
            />
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <SectionCard
              title="Student Academic Performance"
              description="Average marks and pass rate by subject"
            >
              {performanceChart.length === 0 ? (
                <p className="text-sm text-muted-foreground">No performance data available.</p>
              ) : (
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={performanceChart}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} angle={-15} textAnchor="end" height={60} />
                      <YAxis tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="averageMarks" fill="#3b82f6" name="Average Marks" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="passRate" fill="#22c55e" name="Pass Rate (%)" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </SectionCard>

            <SectionCard
              title="Attendance Statistics & Trends"
              description="Last 7 days"
            >
              {attendanceChart.length === 0 ? (
                <p className="text-sm text-muted-foreground">No attendance data available.</p>
              ) : (
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={attendanceChart}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Legend />
                      <Line type="monotone" dataKey="present" stroke="#22c55e" name="Present" />
                      <Line type="monotone" dataKey="absent" stroke="#ef4444" name="Absent" />
                      <Line type="monotone" dataKey="late" stroke="#f59e0b" name="Late" />
                      <Line type="monotone" dataKey="excused" stroke="#8b5cf6" name="Excused" />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}
            </SectionCard>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <SectionCard
              title="Class Performance Comparisons"
              description="Average performance by class/stream"
            >
              {classPerformance.length === 0 ? (
                <p className="text-sm text-muted-foreground">No class performance data available.</p>
              ) : (
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={classPerformance.map(c => ({ name: c.className + (c.streamName ? ' ' + c.streamName : ''), averageMarks: c.averageMarks, studentCount: c.studentCount }))}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="averageMarks" fill="#06b6d4" name="Average Marks" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </SectionCard>

            <SectionCard
              title="Subject Performance"
              description="Average performance by subject"
            >
              {subjectPerformance.length === 0 ? (
                <p className="text-sm text-muted-foreground">No subject performance data available.</p>
              ) : (
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={subjectPerformance}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="subjectName" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="averageMarks" fill="#8b5cf6" name="Average Marks" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </SectionCard>
          </div>

          <SectionCard
            title="Student Distribution Across Assigned Classes"
            description="Enrollment breakdown"
          >
            {studentDistribution.length === 0 ? (
              <p className="text-sm text-muted-foreground">No distribution data available.</p>
            ) : (
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={studentDistribution}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                      outerRadius={110}
                      dataKey="count"
                      nameKey="name"
                    >
                      {studentDistribution.map((_entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            )}
          </SectionCard>

          <SectionCard
            title="My Assigned Classes & Subjects"
            description="Overview of your teaching assignments"
          >
            {dashboard.assignments.length === 0 ? (
              <p className="text-sm text-muted-foreground">You do not have any active class or subject assignments.</p>
            ) : (
              <ul className="grid gap-3 sm:grid-cols-2">
                {dashboard.assignments.map((assignment) => (
                  <li key={assignment.id} className="rounded-lg border p-4">
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
          </SectionCard>

          <SectionCard
            title="Today's Timetable"
            description={formatDate(dashboard.today) + ' · ' + dashboard.timeZone}
          >
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
          </SectionCard>

          <SectionCard
            title="Attendance Tasks"
            description="Record attendance for students in your assigned classes."
          >
            {dashboard.assignments.length === 0 ? (
              <p className="text-sm text-muted-foreground">No assigned classes are available for attendance.</p>
            ) : (
              <div className="flex flex-wrap gap-3">
                <select
                  aria-label="Assigned class and subject"
                  className="input min-w-64"
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
                    className="input"
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
          </SectionCard>

          <SectionCard
            title="Upcoming Examinations"
            description="Examinations for your assigned classes and subjects"
          >
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
          </SectionCard>
        </>
      )}
    </main>
  );
}
