import { useState, type FormEvent } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Dialog, DialogContent, DialogClose } from '@/components/ui/Dialog';
import { DropdownMenu, DropdownMenuItem } from '@/components/ui/DropdownMenu';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus, MoreVertical, Eye, Edit, Trash2, CreditCard } from 'lucide-react';
import { AssignPackageRequest, CreateSchoolRequest, PackageResponse, SchoolResponse, SubscriptionResponse } from '@/types/api';
import { formatTsh } from '@/lib/utils';

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

export function SchoolsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [assignSchool, setAssignSchool] = useState<SchoolResponse | null>(null);
  const [selectedPackageId, setSelectedPackageId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [assignmentError, setAssignmentError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['schools', page, search],
    queryFn: () => api.get(`/platform/schools?page=${page}&size=10&search=${encodeURIComponent(search)}`).then(r => r.data.data),
  });

  const {
    data: packages = [],
    isLoading: packagesLoading,
    isError: packagesError,
    refetch: refetchPackages,
  } = useQuery({
    queryKey: ['packages'],
    queryFn: async () => {
      const response = await api.get<{ data: PackageResponse[] }>('/platform/packages');
      if (!response.data.data) throw new Error('Package response did not contain package data');
      return response.data.data.filter(pkg => pkg.active);
    },
    enabled: assignSchool !== null,
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateSchoolRequest) => api.post('/platform/schools', req).then(r => r.data.data),
    onSuccess: () => {
      setError(null);
      setSuccess('School created successfully');
      queryClient.invalidateQueries({ queryKey: ['schools'] });
      setOpen(false);
    },
    onError: (mutationError: unknown) => {
      setError(getErrorMessage(mutationError, 'Failed to create school'));
    },
  });

  const assignPackageMutation = useMutation({
    mutationFn: async ({ schoolId, request }: { schoolId: number; request: AssignPackageRequest }) => {
      const response = await api.post<{ data: SubscriptionResponse }>(
        `/platform/subscriptions/assign?schoolId=${schoolId}`,
        request
      );
      if (!response.data.data) throw new Error('Assignment response did not contain subscription data');
      return response.data.data;
    },
    onSuccess: async (subscription) => {
      setAssignmentError(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['schools'] }),
        queryClient.invalidateQueries({ queryKey: ['subscriptions'] }),
      ]);
      setSuccess(`${subscription.packageName || 'Package'} assigned to ${subscription.schoolName || assignSchool?.name || 'school'} successfully`);
      setAssignSchool(null);
      setSelectedPackageId('');
    },
    onError: (mutationError: unknown) => {
      setAssignmentError(getErrorMessage(mutationError, 'Failed to assign package'));
    },
  });

  const [formData, setFormData] = useState<CreateSchoolRequest>({
    name: '',
    registrationNumber: '',
    email: '',
    phone: '',
    address: '',
    region: '',
    district: '',
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!formData.name.trim()) {
      setError('School name is required');
      return;
    }
    if (!formData.registrationNumber.trim()) {
      setError('Registration number is required');
      return;
    }

    createMutation.mutate(formData);
  };

  const handlePackageAssignment = (e: FormEvent) => {
    e.preventDefault();
    setAssignmentError(null);
    setSuccess(null);
    if (!assignSchool) return;
    if (!selectedPackageId) {
      setAssignmentError('Select a package to assign');
      return;
    }
    assignPackageMutation.mutate({
      schoolId: assignSchool.id,
      request: { packageId: Number(selectedPackageId) },
    });
  };

  const openPackageAssignment = (school: SchoolResponse) => {
    setSuccess(null);
    setAssignmentError(null);
    setSelectedPackageId('');
    setAssignSchool(school);
  };

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (newOpen) {
      setError(null);
      setSuccess(null);
      setFormData({
        name: '',
        registrationNumber: '',
        email: '',
        phone: '',
        address: '',
        region: '',
        district: '',
      });
    }
  };

  const selectedPackage = packages.find(pkg => String(pkg.id) === selectedPackageId);

  return (
    <div className="space-y-6">
      {success && (
        <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
          {success}
        </div>
      )}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Schools</h1>
          <p className="mt-1 text-muted-foreground">Manage all schools on the platform</p>
        </div>
        <Button onClick={() => setOpen(true)}>
          <Plus className="mr-2 h-4 w-4" /> Add School
        </Button>
      </div>

      <Card className="p-4">
        <div className="mb-4">
          <Input
            placeholder="Search schools..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="max-w-md"
          />
        </div>

        {isLoading ? (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (
          <>
            <div className="rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Region</TableHead>
                    <TableHead>District</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Package</TableHead>
                    <TableHead>Subscription</TableHead>
                    <TableHead>Users</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={8} className="text-center text-muted-foreground">
                        No schools found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((school: SchoolResponse) => (
                    <TableRow key={school.id}>
                      <TableCell className="font-medium">{school.name}</TableCell>
                      <TableCell>{school.region || '-'}</TableCell>
                      <TableCell>{school.district || '-'}</TableCell>
                      <TableCell>
                        <Badge variant={school.isSuspended ? 'destructive' : 'success'}>
                          {school.isSuspended ? 'Suspended' : 'Active'}
                        </Badge>
                      </TableCell>
                      <TableCell>{school.packageName || '-'}</TableCell>
                      <TableCell>
                        {school.subscriptionStatus ? (
                          <Badge variant={school.subscriptionStatus === 'ACTIVE' ? 'success' : 'warning'}>
                            {school.subscriptionStatus}
                          </Badge>
                        ) : '-'}
                      </TableCell>
                      <TableCell>{school.userCount || 0}</TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={<button className="rounded-full p-1 hover:bg-muted"><MoreVertical className="h-4 w-4" /></button>}
                        >
                          <DropdownMenuItem onClick={() => window.location.href = `/platform/schools/${school.id}`}>
                            <Eye className="mr-2 h-4 w-4" /> View
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => openPackageAssignment(school)}>
                            <CreditCard className="mr-2 h-4 w-4" /> Assign Package
                          </DropdownMenuItem>
                          <DropdownMenuItem>
                            <Edit className="mr-2 h-4 w-4" /> Edit
                          </DropdownMenuItem>
                          <DropdownMenuItem>
                            <Trash2 className="mr-2 h-4 w-4 text-red-600" /> Suspend
                          </DropdownMenuItem>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                Page {page + 1} of {data?.totalPages || 1}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage(p => p - 1)}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data?.last}
                  onClick={() => setPage(p => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent title="Add School" description="Create a new school" className="flex flex-col h-full max-h-[85vh]">
          <DialogClose onClick={() => setOpen(false)} />
          <form onSubmit={handleSubmit} className="flex flex-col h-full">
            {error && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                {error}
              </div>
            )}
            <div className="flex-1 overflow-y-auto pr-1 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-sm font-medium">School Name <span className="text-red-500">*</span></label>
                  <Input
                    required
                    value={formData.name}
                    onChange={e => setFormData({ ...formData, name: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Registration Number <span className="text-red-500">*</span></label>
                  <Input
                    required
                    value={formData.registrationNumber}
                    onChange={e => setFormData({ ...formData, registrationNumber: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Email</label>
                  <Input
                    type="email"
                    value={formData.email || ''}
                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Phone</label>
                  <Input
                    value={formData.phone || ''}
                    onChange={e => setFormData({ ...formData, phone: e.target.value })}
                  />
                </div>
                <div className="space-y-1 sm:col-span-2">
                  <label className="text-sm font-medium">Address</label>
                  <Input
                    value={formData.address || ''}
                    onChange={e => setFormData({ ...formData, address: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Region</label>
                  <Input
                    value={formData.region || ''}
                    onChange={e => setFormData({ ...formData, region: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">District</label>
                  <Input
                    value={formData.district || ''}
                    onChange={e => setFormData({ ...formData, district: e.target.value })}
                  />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-4 border-t mt-4">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Creating...' : 'Create School'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog
        open={assignSchool !== null}
        onOpenChange={(newOpen) => {
          if (!newOpen && !assignPackageMutation.isPending) {
            setAssignSchool(null);
            setAssignmentError(null);
          }
        }}
      >
        <DialogContent
          title="Assign Package"
          description={assignSchool ? `Select an existing package for ${assignSchool.name}.` : undefined}
          className="flex max-h-[85vh] flex-col"
        >
          <DialogClose onClick={() => {
            if (!assignPackageMutation.isPending) setAssignSchool(null);
          }} />
          {assignSchool && (
            <form onSubmit={handlePackageAssignment} className="space-y-4">
              {assignmentError && (
                <div role="alert" className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                  {assignmentError}
                </div>
              )}
              <div className="rounded-lg border bg-muted/30 p-3 text-sm">
                <p className="font-medium">{assignSchool.name}</p>
                <p className="text-muted-foreground">
                  Current package: {assignSchool.packageName || 'None'} · Status: {assignSchool.subscriptionStatus || 'No subscription'}
                  {assignSchool.subscriptionEndDate
                    ? ` · Ends ${new Date(assignSchool.subscriptionEndDate).toLocaleDateString()}`
                    : ''}
                </p>
              </div>
              <div className="space-y-2">
                <label htmlFor="assignment-package" className="text-sm font-medium">
                  Package <span className="text-red-500">*</span>
                </label>
                {packagesLoading ? (
                  <Skeleton className="h-10 w-full" />
                ) : packagesError ? (
                  <div className="flex items-center justify-between gap-2 text-sm text-red-600">
                    <span>Could not load packages from the platform.</span>
                    <Button type="button" variant="outline" size="sm" onClick={() => refetchPackages()}>
                      Retry
                    </Button>
                  </div>
                ) : packages.length === 0 ? (
                  <p className="rounded-md border p-3 text-sm text-muted-foreground">
                    No active packages are available to assign.
                  </p>
                ) : (
                  <select
                    id="assignment-package"
                    required
                    value={selectedPackageId}
                    onChange={e => setSelectedPackageId(e.target.value)}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
                  >
                    <option value="">Choose a package</option>
                    {packages.map(pkg => (
                      <option key={pkg.id} value={pkg.id}>{pkg.displayName} — {formatTsh(pkg.price)} / {pkg.billingPeriod}</option>
                    ))}
                  </select>
                )}
              </div>
              {selectedPackage && (
                <div className="rounded-lg border p-3">
                  <p className="font-medium">{selectedPackage.displayName}</p>
                  {selectedPackage.description && <p className="mt-1 text-sm text-muted-foreground">{selectedPackage.description}</p>}
                  <p className="mt-2 text-sm">
                    {formatTsh(selectedPackage.price)} / {selectedPackage.billingPeriod} · Starts today; payment remains pending until verified.
                  </p>
                  {selectedPackage.limits?.length > 0 && (
                    <ul className="mt-2 space-y-1 text-sm text-muted-foreground">
                      {selectedPackage.limits.map(limit => (
                        <li key={limit.id}>
                          {limit.limitName}: {limit.maxValue}{limit.unit ? ` ${limit.unit}` : ''}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
              <div className="flex justify-end gap-2 border-t pt-4">
                <Button type="button" variant="outline" disabled={assignPackageMutation.isPending} onClick={() => setAssignSchool(null)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={
                  assignPackageMutation.isPending ||
                  packagesLoading ||
                  packagesError ||
                  packages.length === 0 ||
                  !selectedPackageId
                }>
                  {assignPackageMutation.isPending ? 'Assigning...' : 'Confirm and Assign Package'}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
