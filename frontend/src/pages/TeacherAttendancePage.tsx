import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';

export function TeacherAttendancePage() {
  const query = useQuery({
    queryKey: ['teacher-workspace-attendance'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<any>>('/school/teacher-workspace')),
  });

  const assignments = query.data?.assignments ?? [];

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">Attendance</h1>
        <p className="mt-1 text-muted-foreground">Record and view attendance for your classes.</p>
      </header>
      <Card>
        <CardHeader><CardTitle>Attendance Tasks</CardTitle></CardHeader>
        <CardContent>
          {query.isLoading && <p className="text-sm text-muted-foreground">Loading...</p>}
          {query.isError && <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(query.error, 'Unable to load assignments.')}</p>}
          {!query.isLoading && !query.isError && assignments.length === 0 && <p className="text-sm text-muted-foreground">No assigned classes.</p>}
          {assignments.length > 0 && <p className="text-sm text-muted-foreground">Use the Attendance Tasks section on the Dashboard to record attendance for a specific class and date.</p>}
        </CardContent>
      </Card>
    </main>
  );
}
