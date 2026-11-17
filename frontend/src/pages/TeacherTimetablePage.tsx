import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';

interface TimetableLesson {
  id: number;
  startTime: string;
  endTime: string;
  room: string | null;
  className: string;
  streamName: string | null;
  subjectName: string;
  dayOfWeek?: number;
}

interface TeacherDashboard {
  timetable: TimetableLesson[];
}

export function TeacherTimetablePage() {
  const query = useQuery({
    queryKey: ['teacher-workspace-timetable'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<TeacherDashboard>>('/school/teacher-workspace')),
  });

  const timetable = query.data?.timetable ?? [];

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">Timetable</h1>
        <p className="mt-1 text-muted-foreground">Your teaching schedule.</p>
      </header>
      <Card>
        <CardHeader><CardTitle>Schedule</CardTitle></CardHeader>
        <CardContent>
          {query.isLoading && <p className="text-sm text-muted-foreground">Loading timetable...</p>}
          {query.isError && <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(query.error, 'Unable to load timetable.')}</p>}
          {!query.isLoading && !query.isError && timetable.length === 0 && <p className="text-sm text-muted-foreground">No lessons scheduled.</p>}
          {timetable.length > 0 && (
            <div className="divide-y">
              {timetable.map((lesson) => (
                <div key={lesson.id} className="flex flex-wrap justify-between gap-2 py-3">
                  <div>
                    <p className="font-medium">{lesson.subjectName} · {lesson.className}{lesson.streamName ? ` · ${lesson.streamName}` : ''}</p>
                    {lesson.room && <p className="text-sm text-muted-foreground">{lesson.room}</p>}
                  </div>
                  <p className="text-sm tabular-nums">{lesson.startTime}–{lesson.endTime}</p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
