import { useMemo, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, CreditCard } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Dialog, DialogClose, DialogContent } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/Table';
import { api } from '@/services/api';
import type {
  ApiResponse,
  AssignPackageRequest,
  PackageResponse,
  SchoolResponse,
  SubscriptionResponse,
} from '@/types/api';
import { formatTsh } from '@/lib/utils';

interface SchoolPage {
  content: SchoolResponse[];
  last: boolean;
}

function todayAsDateInput(): string {
  const today = new Date();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${today.getFullYear()}-${month}-${day}`;
}

function getErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = error.response;
    if (typeof response === 'object' && response !== null && 'data' in response) {
      const data = response.data;
      if (typeof data === 'object' && data !== null && 'message' in data && typeof data.message === 'string') {
        return data.message;
      }
    }
  }
  return error instanceof Error ? error.message : fallback;
}

export function SubscriptionsPage() {
  const queryClient = useQueryClient();
  const [manageOpen, setManageOpen] = useState(false);
  const [schoolId, setSchoolId] = useState('');
  const [packageId, setPackageId] = useState('');
  const [startDate, setStartDate] = useState(todayAsDateInput);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const {
    data: subscriptions,
    isLoading,
    isError,
  } = useQuery<SubscriptionResponse[]>({
    queryKey: ['subscriptions'],
    queryFn: async () => {
      const response = await api.get<ApiResponse<SubscriptionResponse[]>>('/platform/subscriptions');
      if (!response.data.data) throw new Error(response.data.message || 'Failed to load subscriptions');
      return response.data.data;
    },
  });

  const schoolsQuery = useQuery<SchoolResponse[]>({
    queryKey: ['subscription-schools'],
    enabled: manageOpen,
    queryFn: async () => {
      const schools: SchoolResponse[] = [];
      let page = 0;
      let last = false;
      while (!last) {
        const response = await api.get<ApiResponse<SchoolPage>>(`/platform/schools?page=${page}&size=100`);
        const result = response.data.data;
        if (!result) throw new Error(response.data.message || 'Failed to load schools');
        schools.push(...result.content);
        last = result.last;
        page += 1;
      }
      return schools;
    },
  });

  const packagesQuery = useQuery<PackageResponse[]>({
    queryKey: ['packages'],
    enabled: manageOpen,
    queryFn: async () => {
      const response = await api.get<ApiResponse<PackageResponse[]>>('/platform/packages');
      if (!response.data.data) throw new Error(response.data.message || 'Failed to load packages');
      return response.data.data.filter(pkg => pkg.active);
    },
  });

  const selectedSchool = schoolsQuery.data?.find(school => String(school.id) === schoolId);
  const selectedPackage = packagesQuery.data?.find(pkg => String(pkg.id) === packageId);
  const schoolSubscriptions = useMemo(
    () => subscriptions?.filter(subscription => subscription.schoolId === Number(schoolId)) ?? [],
    [schoolId, subscriptions],
  );
  const currentSubscription = schoolSubscriptions.find(subscription => subscription.status === 'ACTIVE')
    ?? schoolSubscriptions.find(subscription => subscription.status === 'TRIAL')
    ?? [...schoolSubscriptions].sort((left, right) => right.createdAt.localeCompare(left.createdAt))[0];
  const hasUnexpiredSubscription = Boolean(currentSubscription
    && (currentSubscription.status === 'ACTIVE' || currentSubscription.status === 'SUSPENDED')
    && (!currentSubscription.endDate || Date.parse(currentSubscription.endDate) > Date.now()));

  const assignMutation = useMutation({
    mutationFn: async ({ targetSchoolId, request }: { targetSchoolId: number; request: AssignPackageRequest }) => {
      const response = await api.post<ApiResponse<SubscriptionResponse>>(
        `/platform/subscriptions/assign?schoolId=${targetSchoolId}`,
        request,
      );
      if (!response.data.data) throw new Error(response.data.message || 'Assignment response did not contain subscription data');
      return response.data.data;
    },
    onSuccess: async subscription => {
      setError(null);
      setSuccess(
        `${subscription.packageName || 'Package'} assigned to ${subscription.schoolName || 'school'}. `
        + `Payment status: ${subscription.paymentStatus}.`,
      );
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['subscriptions'] }),
        queryClient.invalidateQueries({ queryKey: ['schools'] }),
      ]);
      setManageOpen(false);
      setConfirming(false);
    },
    onError: mutationError => {
      setError(getErrorMessage(mutationError, 'Failed to assign package'));
      setConfirming(false);
    },
  });

  const openManagement = (targetSchoolId?: number) => {
    setSuccess(null);
    setError(null);
    setSchoolId(targetSchoolId == null ? '' : String(targetSchoolId));
    setPackageId('');
    setStartDate(todayAsDateInput());
    setConfirming(false);
    setManageOpen(true);
  };

  const closeManagement = (open: boolean) => {
    if (!open && !assignMutation.isPending) {
      setManageOpen(false);
      setConfirming(false);
    }
  };

  const reviewAssignment = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    if (!selectedSchool || !selectedPackage) {
      setError('Select a school and an available package.');
      return;
    }
    if (startDate !== todayAsDateInput()) {
      setError('Package assignments must start today; scheduled subscriptions are not supported.');
      return;
    }
    if (hasUnexpiredSubscription) {
      setError('The school has an unexpired paid subscription. It cannot be replaced before its end date.');
      return;
    }
    setConfirming(true);
  };

  const confirmAssignment = () => {
    if (!selectedSchool || !selectedPackage) {
      setError('Select a school and an available package.');
      setConfirming(false);
      return;
    }
    assignMutation.mutate({
      targetSchoolId: selectedSchool.id,
      request: { packageId: selectedPackage.id, startDate },
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Subscriptions</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Subscriptions</h1>
        <Card className="p-6">
          <div className="flex items-center gap-2 text-red-600">
            <AlertCircle className="h-5 w-5" />
            <p>Failed to load subscriptions. Please try again later.</p>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {success && (
        <div role="status" className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
          {success}
        </div>
      )}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Subscriptions</h1>
          <p className="mt-1 text-muted-foreground">Manage school subscriptions</p>
        </div>
        <Button onClick={() => openManagement()}>
          <CreditCard className="mr-2 h-4 w-4" /> Manage Subscription
        </Button>
      </div>

      <Card>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>School</TableHead>
              <TableHead>Package</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Start Date</TableHead>
              <TableHead>End Date</TableHead>
              <TableHead>Payment</TableHead>
              <TableHead>Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {subscriptions?.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">No subscriptions found</TableCell>
              </TableRow>
            )}
            {subscriptions?.map(subscription => (
              <TableRow key={subscription.id}>
                <TableCell className="font-medium">{subscription.schoolName || '-'}</TableCell>
                <TableCell>{subscription.packageName || '-'}</TableCell>
                <TableCell>
                  <Badge variant={subscription.status === 'ACTIVE' ? 'success' : 'warning'}>{subscription.status}</Badge>
                </TableCell>
                <TableCell>{new Date(subscription.startDate).toLocaleDateString()}</TableCell>
                <TableCell>{subscription.endDate ? new Date(subscription.endDate).toLocaleDateString() : '-'}</TableCell>
                <TableCell>
                  <div className="space-y-1">
                    <span className="block">{formatTsh(subscription.price)}</span>
                    <Badge variant="outline">{subscription.paymentStatus}</Badge>
                  </div>
                </TableCell>
                <TableCell>
                  <Button variant="outline" size="sm" onClick={() => openManagement(subscription.schoolId)}>
                    Manage
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <Dialog open={manageOpen} onOpenChange={closeManagement}>
        <DialogContent
          title={confirming ? 'Confirm subscription change' : 'Manage Subscription'}
          description="Assign an active package to a registered school. Payment will remain pending."
          className="max-h-[85vh] overflow-y-auto"
        >
          <DialogClose onClick={() => closeManagement(false)} />
          {error && (
            <div role="alert" className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
              {error}
            </div>
          )}
          {confirming ? (
            <div className="space-y-4">
              <div className="rounded-lg border bg-muted/30 p-4 text-sm">
                <p className="font-medium">{selectedSchool?.name}</p>
                <p className="mt-2">
                  {currentSubscription
                    ? `Current: ${currentSubscription.packageName || '-'} · ${currentSubscription.status} · `
                      + `${currentSubscription.paymentStatus} · Starts ${new Date(currentSubscription.startDate).toLocaleDateString()} · Ends ${currentSubscription.endDate
                        ? new Date(currentSubscription.endDate).toLocaleDateString()
                        : 'not set'}`
                    : 'Current: no subscription'}
                </p>
                <p className="mt-2">
                  New: {selectedPackage?.displayName} · {formatTsh(selectedPackage?.price ?? 0)}
                  {' / '}{selectedPackage?.billingPeriod} · Starts {startDate}
                </p>
                <p className="mt-2 font-medium">Subscription status will be ACTIVE; payment status will be PENDING.</p>
                <p className="mt-1 text-muted-foreground">This does not record or confirm a payment.</p>
              </div>
              <div className="flex justify-end gap-2 border-t pt-4">
                <Button type="button" variant="outline" disabled={assignMutation.isPending} onClick={() => setConfirming(false)}>
                  Back
                </Button>
                <Button type="button" disabled={assignMutation.isPending} onClick={confirmAssignment}>
                  {assignMutation.isPending ? 'Saving...' : 'Confirm and Assign'}
                </Button>
              </div>
            </div>
          ) : (
            <form onSubmit={reviewAssignment} className="space-y-4">
              <div className="space-y-2">
                <label htmlFor="subscription-school" className="text-sm font-medium">School</label>
                {schoolsQuery.isLoading ? (
                  <Skeleton className="h-10 w-full" />
                ) : schoolsQuery.isError ? (
                  <div className="flex items-center justify-between gap-2 text-sm text-red-600">
                    <span>{getErrorMessage(schoolsQuery.error, 'Could not load schools.')}</span>
                    <Button type="button" variant="outline" size="sm" onClick={() => schoolsQuery.refetch()}>Retry</Button>
                  </div>
                ) : (
                  <select
                    id="subscription-school"
                    required
                    value={schoolId}
                    onChange={event => {
                      setSchoolId(event.target.value);
                      setPackageId('');
                      setConfirming(false);
                    }}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
                  >
                    <option value="">Choose a school</option>
                    {schoolsQuery.data?.map(school => <option key={school.id} value={school.id}>{school.name}</option>)}
                  </select>
                )}
              </div>
              {selectedSchool && (
                <div className="rounded-lg border bg-muted/30 p-3 text-sm">
                  <p className="font-medium">{selectedSchool.name}</p>
                  <p className="mt-1 text-muted-foreground">
                    Current: {currentSubscription
                      ? `${currentSubscription.packageName || '-'} · ${currentSubscription.status} · ${currentSubscription.paymentStatus}`
                      : 'No subscription'}
                    {currentSubscription?.startDate
                      ? ` · Starts ${new Date(currentSubscription.startDate).toLocaleDateString()}`
                      : ''}
                    {currentSubscription?.endDate
                      ? ` · Ends ${new Date(currentSubscription.endDate).toLocaleDateString()}`
                      : ''}
                  </p>
                  {hasUnexpiredSubscription && (
                    <p className="mt-2 text-red-700">
                      This paid subscription cannot be replaced before its end date.
                    </p>
                  )}
                </div>
              )}
              <div className="space-y-2">
                <label htmlFor="subscription-package" className="text-sm font-medium">Available package</label>
                {packagesQuery.isLoading ? (
                  <Skeleton className="h-10 w-full" />
                ) : packagesQuery.isError ? (
                  <div className="flex items-center justify-between gap-2 text-sm text-red-600">
                    <span>{getErrorMessage(packagesQuery.error, 'Could not load packages.')}</span>
                    <Button type="button" variant="outline" size="sm" onClick={() => packagesQuery.refetch()}>Retry</Button>
                  </div>
                ) : (
                  <select
                    id="subscription-package"
                    required
                    value={packageId}
                    onChange={event => {
                      setPackageId(event.target.value);
                      setConfirming(false);
                    }}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
                  >
                    <option value="">Choose a package</option>
                    {packagesQuery.data?.map(pkg => (
                      <option key={pkg.id} value={pkg.id}>
                        {pkg.displayName} — {formatTsh(pkg.price)} / {pkg.billingPeriod}
                      </option>
                    ))}
                  </select>
                )}
              </div>
              {selectedPackage && (
                <div className="rounded-lg border p-3 text-sm">
                  <p className="font-medium">{selectedPackage.displayName}</p>
                  {selectedPackage.description && <p className="mt-1 text-muted-foreground">{selectedPackage.description}</p>}
                  <p className="mt-2">{formatTsh(selectedPackage.price)} / {selectedPackage.billingPeriod}</p>
                  {selectedPackage.limits.length > 0 && (
                    <ul className="mt-2 space-y-1 text-muted-foreground">
                      {selectedPackage.limits.map(limit => (
                        <li key={limit.id}>
                          {limit.limitName}: {limit.maxValue}{limit.unit ? ` ${limit.unit}` : ''}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
              <div className="space-y-2">
                <label htmlFor="subscription-start-date" className="text-sm font-medium">Start date</label>
                <input
                  id="subscription-start-date"
                  type="date"
                  required
                  min={todayAsDateInput()}
                  max={todayAsDateInput()}
                  value={startDate}
                  onChange={event => setStartDate(event.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
                />
                <p className="text-xs text-muted-foreground">
                  Scheduled subscription dates are not supported. The package billing period determines the end date.
                </p>
              </div>
              <div className="flex justify-end gap-2 border-t pt-4">
                <Button
                  type="submit"
                  disabled={
                    !selectedSchool
                    || !selectedPackage
                    || hasUnexpiredSubscription
                    || schoolsQuery.isLoading
                    || packagesQuery.isLoading
                    || assignMutation.isPending
                  }
                >
                  Review change
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
