import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Skeleton } from '@/components/ui/Skeleton';
import { Dialog, DialogContent, DialogClose } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { Plus } from 'lucide-react';

export function PlatformUsersPage() {
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phone: '',
    schoolId: '',
  });
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['platform-users'],
    queryFn: () => api.get('/platform/users').then(r => r.data.data),
  });

  const { data: schoolsData, isLoading: schoolsLoading, error: schoolsError, refetch: refetchSchools } = useQuery({
    queryKey: ['platform-schools'],
    queryFn: () =>
      api
        .get('/platform/schools?page=0&size=100')
        .then(r => r.data.data.content)
        .then((schools: any[]) => schools.map((s: any) => ({ id: s.id, name: s.name }))),
  });

  const createMutation = useMutation({
    mutationFn: (req: {
      firstName: string;
      lastName: string;
      email: string;
      password: string;
      phone: string;
      schoolId: string;
    }) =>
      api
        .post('/platform/users/managers', {
          firstName: req.firstName,
          lastName: req.lastName,
          email: req.email,
          password: req.password,
          phone: req.phone || undefined,
          schoolId: Number(req.schoolId),
        })
        .then(r => r.data.data),
    onSuccess: () => {
      setError(null);
      setSuccess('Manager created successfully');
      queryClient.invalidateQueries({ queryKey: ['platform-users'] });
      setOpen(false);
      setFormData({ firstName: '', lastName: '', email: '', password: '', phone: '', schoolId: '' });
    },
    onError: (err: any) => {
      const message = err?.response?.data?.message || err?.message || 'Failed to create manager';
      setError(message);
    },
  });

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (newOpen) {
      setError(null);
      setSuccess(null);
      setFormData({ firstName: '', lastName: '', email: '', password: '', phone: '', schoolId: '' });
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!formData.firstName.trim()) {
      setError('First name is required');
      return;
    }
    if (!formData.lastName.trim()) {
      setError('Last name is required');
      return;
    }
    if (!formData.email.trim()) {
      setError('Email address is required');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      setError('Email must be valid');
      return;
    }
    if (!formData.password.trim()) {
      setError('Initial password is required');
      return;
    }
    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }
    if (!formData.schoolId) {
      setError('Please assign a school');
      return;
    }

    createMutation.mutate(formData);
  };

  const schoolOptions = (schoolsData ?? []).map((s) => ({ value: String(s.id), label: s.name || 'Unnamed School' }));

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Platform Users</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {success && (
        <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
          {success}
        </div>
      )}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Platform Users</h1>
          <p className="mt-1 text-muted-foreground">All users across the platform</p>
        </div>
        <Button onClick={() => setOpen(true)}>
          <Plus className="mr-2 h-4 w-4" /> Create Manager
        </Button>
      </div>

      <Card>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>School</TableHead>
              <TableHead>Roles</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.content?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-muted-foreground">No users found</TableCell>
              </TableRow>
            )}
            {data?.content?.map((user: any) => (
              <TableRow key={user.id}>
                <TableCell className="font-medium">{user.firstName} {user.lastName}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.schoolName || '-'}</TableCell>
                <TableCell>{user.roles?.join(', ') || '-'}</TableCell>
                <TableCell><Badge variant={user.isActive ? 'success' : 'destructive'}>{user.isActive ? 'Active' : 'Inactive'}</Badge></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Card>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent title="Create Manager" description="Assign a school manager to an existing school" className="flex flex-col h-full max-h-[85vh]">
          <DialogClose onClick={() => setOpen(false)} />
          <form onSubmit={handleSubmit} className="flex flex-col h-full">
            {error && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                {error}
              </div>
            )}
            {schoolsError && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
                Failed to load schools.{' '}
                <Button type="button" variant="outline" size="sm" onClick={() => refetchSchools()}>
                  Retry
                </Button>
              </div>
            )}
            <div className="flex-1 overflow-y-auto pr-1 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-sm font-medium">First Name <span className="text-red-500">*</span></label>
                  <Input
                    required
                    value={formData.firstName}
                    onChange={e => setFormData({ ...formData, firstName: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Last Name <span className="text-red-500">*</span></label>
                  <Input
                    required
                    value={formData.lastName}
                    onChange={e => setFormData({ ...formData, lastName: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Email Address <span className="text-red-500">*</span></label>
                  <Input
                    type="email"
                    required
                    value={formData.email}
                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Initial Password <span className="text-red-500">*</span></label>
                  <Input
                    type="password"
                    required
                    value={formData.password}
                    onChange={e => setFormData({ ...formData, password: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Phone Number</label>
                  <Input
                    value={formData.phone}
                    onChange={e => setFormData({ ...formData, phone: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Assign School <span className="text-red-500">*</span></label>
                  <Select
                    required
                    options={schoolOptions}
                    value={formData.schoolId}
                    onChange={(e) => setFormData({ ...formData, schoolId: e.target.value })}
                  />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-4 border-t mt-4">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending || schoolsLoading}>
                {createMutation.isPending ? 'Creating...' : 'Create Manager'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

