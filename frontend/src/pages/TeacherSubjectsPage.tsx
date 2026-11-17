import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';

interface TeacherAssignment {
  id: number;
  subjectName: string;
  className: string;
  streamName: string | null;
  academicYearName: string;
}

export function TeacherSubjectsPage() {
  const query = useQuery({
    queryKey: ['teacher-subjects'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<TeacherAssignment[]>>('/school/teacher-workspace/assignments')),
  });

  const items = (query.data ?? []).reduce((acc: TeacherAssignment[], cur) => {
    if (!acc.find((x) => x.subjectName === cur.subjectName && x.className === cur.className && x.streamName === cur.streamName)) {
      acc.push(cur);
    }
    return acc;
  }, []);

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">My Subjects</h1>
        <p className="mt-1 text-muted-foreground">Subjects you are assigned to teach.</p>
      </header>
      <Card>
        <CardHeader><CardTitle>Assigned Subjects</CardTitle></CardHeader>
        <CardContent>
          {query.isLoading && <p className="text-sm text-muted-foreground">Loading subjects...</p>}
          {query.isError && <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(query.error, 'Unable to load subjects.')}</p>}
          {!query.isLoading && !query.isError && items.length === 0 && <p className="text-sm text-muted-foreground">No subjects assigned.</p>}
          {items.length > 0 && (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {items.map((a, idx) => (
                <article key={idx} className="rounded-lg border p-4">
                  <h2 className="font-semibold">{a.subjectName}</h2>
                  <p className="text-sm text-muted-foreground">{a.className}{a.streamName ? ` · ${a.streamName}` : ''} · {a.academicYearName}</p>
                </article>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
