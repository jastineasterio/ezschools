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

export function TeacherAssignmentsPage() {
  const query = useQuery({
    queryKey: ['teacher-assignments-full'],
    queryFn: () =>
      unwrapApiResponse(api.get<ApiResponse<TeacherAssignment[]>>('/school/teacher-workspace/assignments')),
  });

  const items = query.data ?? [];

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">Teaching Assignments</h1>
        <p className="mt-1 text-muted-foreground">All your current teaching assignments.</p>
      </header>
      <Card>
        <CardHeader><CardTitle>Assignments</CardTitle></CardHeader>
        <CardContent>
          {query.isLoading && <p className="text-sm text-muted-foreground">Loading assignments...</p>}
          {query.isError && <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(query.error, 'Unable to load assignments.')}</p>}
          {!query.isLoading && !query.isError && items.length === 0 && <p className="text-sm text-muted-foreground">No assignments.</p>}
          {items.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b">
                    <th className="text-left p-2">Class</th>
                    <th className="text-left p-2">Stream</th>
                    <th className="text-left p-2">Subject</th>
                    <th className="text-left p-2">Academic Year</th>
                    <th className="text-left p-2">Role</th>
                    <th className="text-right p-2">Students</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((a) => (
                    <tr key={a.id} className="border-b">
                      <td className="p-2">{a.className}</td>
                      <td className="p-2">{a.streamName || '-'}</td>
                      <td className="p-2">{a.subjectName}</td>
                      <td className="p-2">{a.academicYearName}</td>
                      <td className="p-2">{a.classTeacher ? 'Class Teacher' : 'Subject Teacher'}</td>
                      <td className="p-2 text-right">{a.studentCount ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
