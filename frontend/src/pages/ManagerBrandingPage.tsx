import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { getApiErrorMessage, unwrapApiResponse, api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/Skeleton';
import { Palette } from 'lucide-react';
import type { ApiResponse, SchoolBrandingResponse, UpdateBrandingRequest } from '@/types/api';

const colorFields: { key: keyof Pick<
  UpdateBrandingRequest,
  'primaryColor' | 'secondaryColor' | 'accentColor' | 'backgroundColor' | 'textColor'
>; label: string }[] = [
  { key: 'primaryColor', label: 'Primary Color' },
  { key: 'secondaryColor', label: 'Secondary Color' },
  { key: 'accentColor', label: 'Accent Color' },
  { key: 'backgroundColor', label: 'Background Color' },
  { key: 'textColor', label: 'Text Color' },
];

export function ManagerBrandingPage() {
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['school-branding'],
    queryFn: () =>
      unwrapApiResponse(
        api.get<ApiResponse<SchoolBrandingResponse>>('/school/branding')
      ),
  });

  const queryClient = useQueryClient();
  const updateMutation = useMutation({
    mutationFn: (req: UpdateBrandingRequest) =>
      unwrapApiResponse(
        api.put<ApiResponse<SchoolBrandingResponse>>('/school/branding', req)
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['school-branding'] });
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
    const request: UpdateBrandingRequest = {};
    request[field] = value;
    updateMutation.mutate(request);
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

      {isError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load school branding</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(error, 'The branding settings could not be loaded.')}
            <Button className="ml-3" size="sm" variant="outline" onClick={() => void refetch()}>
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      )}
      {updateMutation.isError && (
        <Alert variant="destructive">
          <AlertTitle>Branding was not updated</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(updateMutation.error, 'Please check the color and try again.')}
          </AlertDescription>
        </Alert>
      )}
      {updateMutation.isSuccess && (
        <Alert>
          <AlertTitle>Branding saved</AlertTitle>
          <AlertDescription>Your school colors were updated.</AlertDescription>
        </Alert>
      )}

      {!isError && data && (
      <Card className="p-6">
        <div className="grid gap-6 md:grid-cols-2">
          <div className="space-y-4">
            <h2 className="font-semibold">Colors</h2>
            {colorFields.map(({ key, label }) => (
              <div key={key} className="flex items-center gap-4">
                <label className="w-32 text-sm">{label}</label>
                <input
                  type="color"
                  value={data[key] || '#3b82f6'}
                  onChange={e => handleUpdate(key, e.target.value)}
                  className="h-10 w-20 rounded border cursor-pointer"
                  disabled={updateMutation.isPending}
                />
                <span className="text-sm text-muted-foreground">{data[key]}</span>
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
      )}
    </div>
  );
}
