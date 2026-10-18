import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent, DialogClose } from '@/components/ui/Dialog';
import { Skeleton } from '@/components/ui/Skeleton';
import { Plus } from 'lucide-react';
import { CreatePackageRequest } from '@/types/api';

export function PackagesPage() {
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['packages'],
    queryFn: () => api.get('/platform/packages').then(r => r.data.data),
  });

  const createMutation = useMutation({
    mutationFn: (req: CreatePackageRequest) => api.post('/platform/packages', req).then(r => r.data.data),
    onSuccess: () => {
      setError(null);
      setSuccess('Package created successfully');
      queryClient.invalidateQueries({ queryKey: ['packages'] });
      setOpen(false);
    },
    onError: (err: any) => {
      const message = err?.response?.data?.message || err?.message || 'Failed to create package';
      setError(message);
    },
  });

  const [formData, setFormData] = useState<CreatePackageRequest>({
    name: '',
    displayName: '',
    description: '',
    price: 0,
    billingPeriod: 'MONTHLY',
    active: true,
    default: false,
    sortOrder: 0,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!formData.name.trim()) {
      setError('Name is required');
      return;
    }
    if (!formData.displayName.trim()) {
      setError('Display name is required');
      return;
    }
    if (!(formData.billingPeriod || '').trim()) {
      setError('Billing period is required');
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
        displayName: '',
        description: '',
        price: 0,
        billingPeriod: 'MONTHLY',
        active: true,
        default: false,
        sortOrder: 0,
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
          <h1 className="text-3xl font-bold">Packages</h1>
          <p className="mt-1 text-muted-foreground">Manage subscription packages</p>
        </div>
        <Button onClick={() => setOpen(true)}>
          <Plus className="mr-2 h-4 w-4" /> Add Package
        </Button>
      </div>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(3)].map((_, i) => (
            <Card key={i} className="p-6"><Skeleton className="h-40" /></Card>
          ))}
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {data?.length === 0 && (
            <Card className="p-6 col-span-full">
              <p className="text-center text-muted-foreground">No packages found.</p>
            </Card>
          )}
          {data?.map((pkg: any) => (
            <Card key={pkg.id} className="p-6">
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="font-semibold">{pkg.displayName}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{pkg.description}</p>
                  <p className="mt-2 text-2xl font-bold">${pkg.price}<span className="text-sm text-muted-foreground">/{pkg.billingPeriod?.toLowerCase() || ''}</span></p>
                </div>
                <Badge variant={pkg.active ? 'success' : 'secondary'}>{pkg.active ? 'Active' : 'Inactive'}</Badge>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                {pkg.roles?.map((role: string) => (
                  <Badge key={role} variant="outline">{role}</Badge>
                ))}
              </div>
            </Card>
          ))}
        </div>
      )}

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent title="Add Package" description="Create a new package" className="flex flex-col h-full max-h-[85vh]">
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
                  <label className="text-sm font-medium">Name <span className="text-red-500">*</span></label>
                  <Input
                    required
                    value={formData.name}
                    onChange={e => setFormData({ ...formData, name: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Display Name <span className="text-red-500">*</span></label>
                  <Input
                    required
                    value={formData.displayName}
                    onChange={e => setFormData({ ...formData, displayName: e.target.value })}
                  />
                </div>
                <div className="space-y-1 sm:col-span-2">
                  <label className="text-sm font-medium">Description</label>
                  <Input
                    value={formData.description || ''}
                    onChange={e => setFormData({ ...formData, description: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Price <span className="text-red-500">*</span></label>
                  <Input
                    type="number"
                    required
                    value={formData.price}
                    onChange={e => setFormData({ ...formData, price: parseFloat(e.target.value) || 0 })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Billing Period <span className="text-red-500">*</span></label>
                  <Input
                    required
                    value={formData.billingPeriod}
                    onChange={e => setFormData({ ...formData, billingPeriod: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-sm font-medium">Sort Order</label>
                  <Input
                    type="number"
                    value={formData.sortOrder}
                    onChange={e => setFormData({ ...formData, sortOrder: parseInt(e.target.value) || 0 })}
                  />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-4 border-t mt-4">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Creating...' : 'Create Package'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
