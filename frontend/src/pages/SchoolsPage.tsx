import { useState } from 'react';
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
import { Plus, MoreVertical, Eye, Edit, Trash2 } from 'lucide-react';
import { CreateSchoolRequest } from '@/types/api';

export function SchoolsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['schools', page, search],
    queryFn: () => api.get(`/platform/schools?page=${page}&size=10&search=${encodeURIComponent(search)}`).then(r => r.data.data),
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateSchoolRequest) => api.post('/platform/schools', req).then(r => r.data.data),
    onSuccess: () => {
      setError(null);
      setSuccess('School created successfully');
      queryClient.invalidateQueries({ queryKey: ['schools'] });
      setOpen(false);
    },
    onError: (err: any) => {
      const message = err?.response?.data?.message || err?.message || 'Failed to create school';
      setError(message);
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

  const handleSubmit = (e: React.FormEvent) => {
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
                    <TableHead>Users</TableHead>
                    <TableHead className="w-12" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data?.content?.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center text-muted-foreground">
                        No schools found
                      </TableCell>
                    </TableRow>
                  )}
                  {data?.content?.map((school: any) => (
                    <TableRow key={school.id}>
                      <TableCell className="font-medium">{school.name}</TableCell>
                      <TableCell>{school.region || '-'}</TableCell>
                      <TableCell>{school.district || '-'}</TableCell>
                      <TableCell>
                        <Badge variant={school.suspended ? 'destructive' : 'success'}>
                          {school.suspended ? 'Suspended' : 'Active'}
                        </Badge>
                      </TableCell>
                      <TableCell>{school.userCount || 0}</TableCell>
                      <TableCell>
                        <DropdownMenu
                          trigger={<button className="rounded-full p-1 hover:bg-muted"><MoreVertical className="h-4 w-4" /></button>}
                        >
                          <DropdownMenuItem onClick={() => window.location.href = `/platform/schools/${school.id}`}>
                            <Eye className="mr-2 h-4 w-4" /> View
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
    </div>
  );
}

