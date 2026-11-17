import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';

interface TeacherAssignment {
  id: number;
  className: string;
  streamName: string | null;
  subjectName: string;
  academicYearName: string;
  classTeacher?: boolean;
  studentCount?: number;
}

export function TeacherClassesPage() {
  const query = useQuery({
    queryKey: ['teacher-assignments'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<TeacherAssignment[]>>('/school/teacher-workspace/assignments')),
  });

  const items = query.data ?? [];

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">My Classes</h1>
        <p className="mt-1 text-muted-foreground">Classes assigned to you for the current academic year.</p>
      </header>
      <Card>
        <CardHeader>
          <CardTitle>Assigned Classes</CardTitle>
        </CardHeader>
        <CardContent>
          {query.isLoading && <p className="text-sm text-muted-foreground">Loading classes...</p>}
          {query.isError && <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(query.error, 'Unable to load classes.')}</p>}
          {!query.isLoading && !query.isError && items.length === 0 && <p className="text-sm text-muted-foreground">No classes assigned.</p>}
          {items.length > 0 && (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {items.map((a) => (
                <article key={a.id} className="rounded-lg border p-4">
                  <h2 className="font-semibold">{a.className}{a.streamName ? ` · ${a.streamName}` : ''}</h2>
                  <p className="text-sm text-muted-foreground">{a.subjectName}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{a.academicYearName}{a.studentCount !== undefined ? ` · ${a.studentCount} students` : ''}{a.classTeacher ? ' · Class teacher' : ''}</p>
                </article>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
