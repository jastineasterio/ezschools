import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/Skeleton';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import type { ApiResponse, SchoolResponse } from '@/types/api';
import { Mail, Phone, MapPin } from 'lucide-react';

export function ManagerSchoolPage() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['school-profile'],
    queryFn: () =>
      unwrapApiResponse(
        api.get<ApiResponse<SchoolResponse>>('/school/profile')
      ),
  });
  const updateTimeZone = useMutation({
    mutationFn: (timeZone: string) =>
      unwrapApiResponse(
        api.patch<ApiResponse<SchoolResponse>>('/school/profile/time-zone', { timeZone })
      ),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['school-profile'] }),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">School Profile</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">School Profile</h1>

      {isError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load school profile</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(error, 'The school information could not be loaded.')}
            <Button className="ml-3" size="sm" variant="outline" onClick={() => void refetch()}>
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {!isError && data && (
      <Card className="p-6">
        <h2 className="text-lg font-semibold mb-4">School Information</h2>
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <p className="text-sm text-muted-foreground">Name</p>
            <p className="font-medium">{data.name || 'Not provided'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Registration Number</p>
            <p className="font-medium">{data.registrationNumber || 'Not provided'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Email</p>
            <p className="font-medium flex items-center gap-2"><Mail className="h-4 w-4" /> {data.email || 'Not provided'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Phone</p>
            <p className="font-medium flex items-center gap-2"><Phone className="h-4 w-4" /> {data.phone || 'Not provided'}</p>
          </div>
          <div className="md:col-span-2">
            <p className="text-sm text-muted-foreground">Address</p>
            <p className="font-medium flex items-center gap-2"><MapPin className="h-4 w-4" /> {data.address || 'Not provided'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Region</p>
            <p className="font-medium">{data.region || 'Not provided'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">District</p>
            <p className="font-medium">{data.district || 'Not provided'}</p>
          </div>
        </div>
        <form
          className="mt-6 flex flex-wrap items-end gap-3 border-t pt-5"
          onSubmit={(event) => {
            event.preventDefault();
            const form = new FormData(event.currentTarget);
            updateTimeZone.mutate(String(form.get('timeZone') ?? ''));
          }}
        >
          <label className="space-y-1 text-sm">
            <span className="block font-medium">School time zone</span>
            <input
              className="rounded-md border bg-background px-3 py-2"
              name="timeZone"
              defaultValue={data.timeZone || 'UTC'}
              placeholder="Africa/Accra"
              maxLength={64}
              required
            />
          </label>
          <Button type="submit" disabled={updateTimeZone.isPending}>
            {updateTimeZone.isPending ? 'Saving…' : 'Save time zone'}
          </Button>
          <p className="w-full text-xs text-muted-foreground">Use an IANA time zone, for example Africa/Accra or America/New_York.</p>
          {updateTimeZone.isSuccess && <p role="status" className="w-full text-sm text-green-700">Time zone updated.</p>}
          {updateTimeZone.isError && (
            <p role="alert" className="w-full text-sm text-destructive">
              {getApiErrorMessage(updateTimeZone.error, 'The time zone could not be saved.')}
            </p>
          )}
        </form>
      </Card>
      )}
    </div>
  );
}
