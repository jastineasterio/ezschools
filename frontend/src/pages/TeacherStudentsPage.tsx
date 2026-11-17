import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';

interface TeacherAssignment {
  id: number;
  className: string;
  streamName: string | null;
  subjectName: string;
  studentCount?: number;
}

export function TeacherStudentsPage() {
  const query = useQuery({
    queryKey: ['teacher-assignments-students'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<TeacherAssignment[]>>('/school/teacher-workspace/assignments')),
  });

  const items = query.data ?? [];

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">My Students</h1>
        <p className="mt-1 text-muted-foreground">Students in your assigned classes.</p>
      </header>
      {query.isLoading && <p className="text-sm text-muted-foreground">Loading...</p>}
      {query.isError && <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(query.error, 'Unable to load assignments.')}</p>}
      {items.map((a) => (
        <Card key={a.id}>
          <CardHeader><CardTitle>{a.className}{a.streamName ? ` · ${a.streamName}` : ''} - {a.subjectName}</CardTitle></CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">{a.studentCount ?? 0} enrolled students</p>
          </CardContent>
        </Card>
      ))}
    </main>
  );
}
