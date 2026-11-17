import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse } from '@/types/api';

interface TeacherDashboard {
  announcements?: any[];
}

export function TeacherAnnouncementsPage() {
  const query = useQuery({
    queryKey: ['teacher-announcements'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<TeacherDashboard>>('/school/teacher-workspace')),
  });

  const announcements = query.data?.announcements ?? [];

  return (
    <main className="mx-auto max-w-4xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">Announcements</h1>
        <p className="mt-1 text-muted-foreground">School announcements.</p>
      </header>
      <Card>
        <CardHeader><CardTitle>Announcements</CardTitle></CardHeader>
        <CardContent>
          {query.isLoading && <p className="text-sm text-muted-foreground">Loading...</p>}
          {query.isError && <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(query.error, 'Unable to load announcements.')}</p>}
          {!query.isLoading && !query.isError && announcements.length === 0 && <p className="text-sm text-muted-foreground">No announcements available.</p>}
          {announcements.length > 0 && (
            <div className="divide-y">
              {announcements.map((a: any, idx: number) => (
                <div key={idx} className="py-3">
                  <p className="font-medium">{a.title}</p>
                  <p className="text-sm text-muted-foreground">{a.message}</p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
