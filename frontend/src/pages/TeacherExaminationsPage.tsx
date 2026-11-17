import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';

interface UpcomingExamination {
  examinationSubjectId: number;
  examinationName: string;
  subjectName: string;
  className: string;
  streamName: string | null;
  startDate: string | null;
  endDate: string | null;
  status: string;
}

interface TeacherDashboard {
  upcomingExaminations: UpcomingExamination[];
}

function formatDate(value: string | null) {
  if (!value) return 'Date not set';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeZone: 'UTC' }).format(new Date(`${value}T00:00:00Z`));
}

export function TeacherExaminationsPage() {
  const query = useQuery({
    queryKey: ['teacher-workspace-exams'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<TeacherDashboard>>('/school/teacher-workspace')),
  });

  const exams = query.data?.upcomingExaminations ?? [];

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">Examinations</h1>
        <p className="mt-1 text-muted-foreground">Upcoming examinations for your assigned classes.</p>
      </header>
      <Card>
        <CardHeader><CardTitle>Upcoming Examinations</CardTitle></CardHeader>
        <CardContent>
          {query.isLoading && <p className="text-sm text-muted-foreground">Loading...</p>}
          {query.isError && <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(query.error, 'Unable to load examinations.')}</p>}
          {!query.isLoading && !query.isError && exams.length === 0 && <p className="text-sm text-muted-foreground">No upcoming examinations.</p>}
          {exams.length > 0 && (
            <div className="divide-y">
              {exams.map((ex, idx) => (
                <div key={idx} className="py-3">
                  <p className="font-medium">{ex.examinationName} · {ex.subjectName}</p>
                  <p className="text-sm text-muted-foreground">{ex.className}{ex.streamName ? ` · ${ex.streamName}` : ''} · {formatDate(ex.startDate)}{ex.endDate && ex.endDate !== ex.startDate ? ` – ${formatDate(ex.endDate)}` : ''} · {ex.status}</p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
