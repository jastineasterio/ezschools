import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/Skeleton';
import { Palette } from 'lucide-react';
import type { UpdateBrandingRequest } from '@/types/api';

export function ManagerBrandingPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['school-branding'],
    queryFn: () => api.get('/school/branding').then(r => r.data),
  });

  const queryClient = useQueryClient();
  const updateMutation = useMutation({
    mutationFn: (req: UpdateBrandingRequest) => api.put('/school/branding', req).then(r => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['school-branding'] });
    },
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Branding</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  const handleUpdate = (field: keyof UpdateBrandingRequest, value: string) => {
    updateMutation.mutate({ ...data, [field]: value } as UpdateBrandingRequest);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <div className="rounded-lg bg-muted p-3"><Palette className="h-6 w-6" /></div>
        <div>
          <h1 className="text-3xl font-bold">Branding</h1>
          <p className="mt-1 text-muted-foreground">Customize your school's appearance</p>
        </div>
      </div>

      <Card className="p-6">
        <div className="grid gap-6 md:grid-cols-2">
          <div className="space-y-4">
            <h2 className="font-semibold">Colors</h2>
            {[
              { key: 'primaryColor', label: 'Primary Color' },
              { key: 'secondaryColor', label: 'Secondary Color' },
              { key: 'accentColor', label: 'Accent Color' },
              { key: 'backgroundColor', label: 'Background Color' },
              { key: 'textColor', label: 'Text Color' },
            ].map(({ key, label }) => (
              <div key={key} className="flex items-center gap-4">
                <label className="w-32 text-sm">{label}</label>
                <input
                  type="color"
                  value={(data as any)?.[key] || '#3b82f6'}
                  onChange={e => handleUpdate(key as keyof UpdateBrandingRequest, e.target.value)}
                  className="h-10 w-20 rounded border cursor-pointer"
                />
                <span className="text-sm text-muted-foreground">{(data as any)?.[key]}</span>
              </div>
            ))}
          </div>

          <div className="space-y-4">
            <h2 className="font-semibold">Preview</h2>
            <div className="rounded-lg border p-4 space-y-3">
              <div
                className="h-24 rounded-lg flex items-center justify-center text-white font-semibold"
                style={{ backgroundColor: data?.primaryColor }}
              >
                Primary
              </div>
              <div
                className="h-12 rounded-lg flex items-center justify-center text-white font-semibold"
                style={{ backgroundColor: data?.secondaryColor }}
              >
                Secondary
              </div>
              <div
                className="h-12 rounded-lg flex items-center justify-center text-white font-semibold"
                style={{ backgroundColor: data?.accentColor }}
              >
                Accent
              </div>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}

