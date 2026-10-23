import { useAuth } from '@/store/AuthContext';
import { Card } from '@/components/ui/Card';
import { Link } from 'react-router-dom';

export function StudentDashboardPage() {
  const { user } = useAuth();
  const name = user ? user.firstName + ' ' + user.lastName : 'Student';

  return (
    <main className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Welcome, {name}</h1>
        <p className="mt-1 text-muted-foreground">Student Dashboard</p>
      </div>
      <Card className="flex flex-wrap items-center justify-between gap-4 p-6">
        <div>
          <h2 className="text-lg font-semibold">Certificates</h2>
          <p className="mt-1 text-sm text-muted-foreground">View and download certificates released by your school.</p>
        </div>
        <Link className="text-sm font-medium text-primary underline-offset-4 hover:underline" to="/school/student/certificates">
          Open my certificates
        </Link>
      </Card>
      <Card className="p-6">
        <h2 className="text-lg font-semibold">Enrolled class and academic information</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Backend endpoints for student academic information are not yet available.
        </p>
      </Card>
      <Card className="p-6">
        <h2 className="text-lg font-semibold">Personal timetable</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Backend endpoints for student timetables are not yet available.
        </p>
      </Card>
      <Card className="p-6">
        <h2 className="text-lg font-semibold">Attendance summary</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Backend endpoints for student attendance summaries are not yet available.
        </p>
      </Card>
      <Card className="p-6">
        <h2 className="text-lg font-semibold">Recent examination results</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Backend endpoints for student results are not yet available.
        </p>
      </Card>
      <Card className="p-6">
        <h2 className="text-lg font-semibold">Upcoming assignments</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Backend endpoints for student assignments are not yet available.
        </p>
      </Card>
      <Card className="p-6">
        <h2 className="text-lg font-semibold">Fee balance</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Backend endpoints for student fee balances are not yet available.
        </p>
      </Card>
    </main>
  );
}
