import { useQuery } from '@tanstack/react-query';
import {
  Bus,
  CalendarClock,
  Route,
  ShieldAlert,
  Users,
  Wrench,
} from 'lucide-react';
import { useAuth } from '@/store/AuthContext';
import { getTransportOfficerDashboard } from '@/services/dashboards';
import { getApiErrorMessage } from '@/services/api';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { StatCard } from '@/components/dashboard/StatCard';
import { EmptyState, SectionCard } from '@/components/dashboard/EmptyState';
import { formatDate, formatNumber } from '@/lib/format';

export function TransportOfficerDashboardPage() {
  const { user } = useAuth();
  const displayName =
    user && (user.firstName || user.lastName)
      ? `${user.firstName} ${user.lastName}`.trim()
      : 'Transport Officer';

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard', 'transport-officer'],
    queryFn: getTransportOfficerDashboard,
  });

  if (isLoading) {
    return (
      <div className="space-y-6" aria-busy="true" aria-live="polite">
        <Skeleton className="h-10 w-64" />
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </div>
        <Skeleton className="h-72 w-full" />
      </div>
    );
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Unable to load the transport dashboard</AlertTitle>
        <AlertDescription>
          {getApiErrorMessage(error, 'The transport summary could not be loaded.')}{' '}
          <button className="underline" onClick={() => void refetch()}>
            Try again
          </button>
        </AlertDescription>
      </Alert>
    );
  }

  if (!data) {
    return (
      <EmptyState
        title="No dashboard data"
        description="The transport dashboard returned no information for this school."
      />
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground sm:text-3xl">
          Welcome, {displayName}
        </h1>
        <p className="mt-1 text-muted-foreground">
          {data.schoolName ? `${data.schoolName} · ` : ''}Transport officer dashboard
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Vehicles"
          value={formatNumber(data.totalVehicles)}
          icon={Bus}
          hint={`${formatNumber(data.activeVehicles)} active`}
        />
        <StatCard
          label="Routes"
          value={formatNumber(data.totalRoutes)}
          icon={Route}
          hint={`${formatNumber(data.activeRoutes)} active`}
        />
        <StatCard
          label="Students on transport"
          value={formatNumber(data.studentsOnTransport)}
          icon={Users}
          hint={`${formatNumber(data.activeAssignments)} active assignments`}
        />
        <StatCard
          label="Service due"
          value={formatNumber(data.vehiclesDueForService)}
          tone={data.vehiclesDueForService > 0 ? 'warning' : 'success'}
          icon={Wrench}
          hint={`By ${formatDate(data.serviceHorizon)}`}
        />
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label="Insurance expiring"
          value={formatNumber(data.vehiclesWithExpiringInsurance)}
          tone={data.vehiclesWithExpiringInsurance > 0 ? 'warning' : 'success'}
          icon={ShieldAlert}
          hint={`By ${formatDate(data.serviceHorizon)}`}
        />
        <StatCard
          label="Active assignments"
          value={formatNumber(data.activeAssignments)}
          icon={CalendarClock}
        />
      </div>

      <SectionCard
        title="Fleet"
        description="Vehicles registered for this school"
      >
        {data.vehicles.length === 0 ? (
          <EmptyState
            title="No vehicles"
            description="Vehicles added to the fleet will be listed here."
          />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Registration</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead className="text-right">Capacity</TableHead>
                  <TableHead>Driver</TableHead>
                  <TableHead>Next service</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.vehicles.map((vehicle) => (
                  <TableRow key={vehicle.id}>
                    <TableCell className="font-medium">
                      {vehicle.registrationNumber}
                    </TableCell>
                    <TableCell>{vehicle.vehicleType}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatNumber(vehicle.capacity)}
                    </TableCell>
                    <TableCell>
                      {vehicle.driverName ?? '—'}
                      {vehicle.driverPhone ? (
                        <span className="block text-xs text-muted-foreground">
                          {vehicle.driverPhone}
                        </span>
                      ) : null}
                    </TableCell>
                    <TableCell className="whitespace-nowrap">
                      {formatDate(vehicle.nextServiceDate)}
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{vehicle.status ?? '—'}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </SectionCard>

      <div className="grid gap-4 lg:grid-cols-2">
        <SectionCard title="Routes" description="Transport routes in operation">
          {data.routes.length === 0 ? (
            <EmptyState
              title="No routes"
              description="Routes created for this school will appear here."
            />
          ) : (
            <ul className="space-y-2">
              {data.routes.map((route) => (
                <li
                  key={route.id}
                  className="flex items-center justify-between gap-3 rounded-lg border border-border p-3"
                >
                  <div>
                    <p className="text-sm font-medium text-foreground">{route.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {route.startPoint} → {route.endPoint}
                    </p>
                  </div>
                  <Badge variant="secondary">{route.status ?? '—'}</Badge>
                </li>
              ))}
            </ul>
          )}
        </SectionCard>

        <SectionCard
          title="Active assignments"
          description="Students assigned to a route and vehicle"
        >
          {data.assignments.length === 0 ? (
            <EmptyState
              title="No active assignments"
              description="Student transport assignments will be listed here."
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Student</TableHead>
                    <TableHead>Route</TableHead>
                    <TableHead>Stop</TableHead>
                    <TableHead>Vehicle</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.assignments.map((assignment) => (
                    <TableRow key={assignment.id}>
                      <TableCell>{assignment.studentName ?? '—'}</TableCell>
                      <TableCell>{assignment.routeName ?? '—'}</TableCell>
                      <TableCell>{assignment.stopName ?? '—'}</TableCell>
                      <TableCell>
                        {assignment.vehicleRegistration ?? '—'}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </SectionCard>
      </div>
    </div>
  );
}
