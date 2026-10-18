import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import { SchoolForm } from '@/components/forms/SchoolForm';
import { api } from '@/services/api';
import type { CreateSchoolRequest, UpdateSchoolRequest, SchoolResponse } from '@/types/api';
import { ArrowLeft } from 'lucide-react';

export function SchoolFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEditing = !!id;
  const [error, setError] = useState<string | null>(null);

  const { data: school, isLoading: schoolLoading } = useQuery({
    queryKey: ['school', id],
    queryFn: async () => {
      const { data } = await api.get<{ data: SchoolResponse }>(`/platform/schools/${id}`);
      return data.data;
    },
    enabled: isEditing,
  });

  const createMutation = useMutation({
    mutationFn: async (data: CreateSchoolRequest) => {
      const response = await api.post('/platform/schools', data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schools'] });
      navigate('/platform/schools');
    },
    onError: (err: any) => {
      const message = err?.response?.data?.message || err?.message || 'Failed to create school';
      setError(message);
    },
  });

  const updateMutation = useMutation({
    mutationFn: async (data: UpdateSchoolRequest) => {
      const response = await api.put(`/platform/schools/${id}`, data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['school', id] });
      queryClient.invalidateQueries({ queryKey: ['schools'] });
      navigate(`/platform/schools/${id}`);
    },
    onError: (err: any) => {
      const message = err?.response?.data?.message || err?.message || 'Failed to update school';
      setError(message);
    },
  });

  const handleSubmit = async (data: CreateSchoolRequest | UpdateSchoolRequest) => {
    setError(null);
    if (isEditing) {
      await updateMutation.mutateAsync(data as UpdateSchoolRequest);
    } else {
      await createMutation.mutateAsync(data as CreateSchoolRequest);
    }
  };

  if (isEditing && schoolLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-3xl font-bold">{isEditing ? 'Edit School' : 'Create School'}</h1>
          <p className="text-muted-foreground">
            {isEditing ? 'Update school information' : 'Add a new school to the platform'}
          </p>
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
          <CardTitle>School Details</CardTitle>
        </CardHeader>
        <CardContent>
          {isEditing && school ? (
            <SchoolForm
              initialData={school as any}
              onSubmit={handleSubmit}
              submitLabel="Update School"
              onCancel={() => navigate(`/platform/schools/${id}`)}
            />
          ) : (
            <SchoolForm onSubmit={handleSubmit} submitLabel="Create School" onCancel={() => navigate('/platform/schools')} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
