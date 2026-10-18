import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import { ArrowLeft } from 'lucide-react';
import { PackageForm } from '@/components/forms/PackageForm';
import { api } from '@/services/api';
import type { CreatePackageRequest } from '@/types/api';

export function PackageFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEdit = !!id;
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['package', id],
    queryFn: () => api.get(`/platform/packages/${id}`).then(r => r.data),
    enabled: isEdit,
  });

  const createMutation = useMutation({
    mutationFn: async (formData: CreatePackageRequest) => {
      const response = await api.post('/platform/packages', formData);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['packages'] });
      navigate('/platform/packages');
    },
    onError: (err: any) => {
      const message = err?.response?.data?.message || err?.message || 'Failed to create package';
      setError(message);
    },
  });

  const updateMutation = useMutation({
    mutationFn: async (formData: CreatePackageRequest) => {
      const response = await api.put(`/platform/packages/${id}`, formData);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['package', id] });
      queryClient.invalidateQueries({ queryKey: ['packages'] });
      navigate(`/platform/packages/${id}`);
    },
    onError: (err: any) => {
      const message = err?.response?.data?.message || err?.message || 'Failed to update package';
      setError(message);
    },
  });

  const handleSubmit = async (formData: CreatePackageRequest) => {
    setError(null);
    if (isEdit) {
      await updateMutation.mutateAsync(formData);
    } else {
      await createMutation.mutateAsync(formData);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/platform/packages">
          <Button variant="ghost" size="icon"><ArrowLeft className="h-4 w-4" /></Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">{isEdit ? 'Edit Package' : 'Create Package'}</h1>
          <p className="mt-1 text-muted-foreground">{isEdit ? data?.displayName : 'New package'}</p>
        </div>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertTitle>Error</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Package Details</CardTitle>
        </CardHeader>
        <CardContent>
          <PackageForm
            initialData={data as any}
            onSubmit={handleSubmit}
            submitLabel={isEdit ? 'Update Package' : 'Create Package'}
            onCancel={() => navigate('/platform/packages')}
          />
        </CardContent>
      </Card>
    </div>
  );
}

