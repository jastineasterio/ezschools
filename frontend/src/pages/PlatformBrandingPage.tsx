import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { api, getApiErrorMessage } from '@/services/api';
import type { SchoolBrandingResponse, SchoolResponse, UpdateBrandingRequest } from '@/types/api';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  Palette,
  Image,
  School,
  ArrowLeft,
  Save,
  RefreshCw,
  Trash2,
} from 'lucide-react';

const MAX_LOGO_SIZE = 2 * 1024 * 1024;
const ACCEPTED_LOGO_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];

const brandingSchema = z.object({
  primaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  secondaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  accentColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  backgroundColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  textColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
});

type BrandingFormValues = z.infer<typeof brandingSchema>;

const COLOR_FIELDS: { key: keyof BrandingFormValues; label: string }[] = [
  { key: 'primaryColor', label: 'Primary Color' },
  { key: 'secondaryColor', label: 'Secondary Color' },
  { key: 'accentColor', label: 'Accent Color' },
  { key: 'backgroundColor', label: 'Background Color' },
  { key: 'textColor', label: 'Text Color' },
];

export function PlatformBrandingPage() {
  const { id } = useParams();
  const schoolId = id ? Number(id) : NaN;
  const queryClient = useQueryClient();

  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [logoPreview, setLogoPreview] = useState<string | null>(null);
  const [removingLogo, setRemovingLogo] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const {
    data: school,
    isLoading: schoolLoading,
    error: schoolError,
  } = useQuery({
    queryKey: ['platform-school', schoolId],
    queryFn: async () => {
      const response = await api.get<{ data: SchoolResponse }>(`/platform/schools/${schoolId}`);
      return response.data.data;
    },
    enabled: Number.isFinite(schoolId),
  });

  const {
    data: branding,
    isLoading: brandingLoading,
    error: brandingError,
    refetch: refetchBranding,
  } = useQuery({
    queryKey: ['platform-school-branding', schoolId],
    queryFn: async () => {
      const response = await api.get<{ data: SchoolBrandingResponse }>(
        `/platform/schools/${schoolId}/branding`,
      );
      return response.data.data;
    },
    enabled: Number.isFinite(schoolId),
  });

  const updateMutation = useMutation({
    mutationFn: (req: UpdateBrandingRequest) =>
      api
        .put<{ data: SchoolBrandingResponse }>(
          `/platform/schools/${schoolId}/branding`,
          req,
        )
        .then((r) => r.data.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['platform-school-branding', schoolId] });
      setToast({ message: 'Branding saved successfully.', type: 'success' });
    },
    onError: () => {
      setToast({ message: 'Failed to save branding.', type: 'error' });
    },
  });

  const uploadLogoMutation = useMutation({
    mutationFn: (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      return api
        .post<{ data: SchoolBrandingResponse }>(
          `/platform/schools/${schoolId}/branding/logo`,
          formData,
        )
        .then((r) => r.data.data);
    },
    onSuccess: (data) => {
      setLogoFile(null);
      setLogoPreview(data.logoUrl || null);
      queryClient.invalidateQueries({ queryKey: ['platform-school-branding', schoolId] });
      setToast({ message: 'Logo uploaded successfully.', type: 'success' });
    },
    onError: (error) => {
      setToast({ message: getApiErrorMessage(error, 'Logo upload failed.'), type: 'error' });
    },
  });

  const removeLogoMutation = useMutation({
    mutationFn: () =>
      api
        .delete<{ data: SchoolBrandingResponse }>(
          `/platform/schools/${schoolId}/branding/logo`,
        )
        .then((r) => r.data.data),
    onSuccess: () => {
      setRemovingLogo(false);
      setLogoPreview(null);
      setLogoFile(null);
      queryClient.invalidateQueries({ queryKey: ['platform-school-branding', schoolId] });
      setToast({ message: 'Logo removed.', type: 'success' });
    },
    onError: () => {
      setRemovingLogo(false);
      setToast({ message: 'Failed to remove logo.', type: 'error' });
    },
  });

  const resetMutation = useMutation({
    mutationFn: () =>
      api
        .post<{ data: SchoolBrandingResponse }>(
          `/platform/schools/${schoolId}/branding/reset`,
        )
        .then((r) => r.data.data),
    onSuccess: (data) => {
      setLogoFile(null);
      setLogoPreview(data.logoUrl || null);
      queryClient.invalidateQueries({ queryKey: ['platform-school-branding', schoolId] });
      setToast({ message: 'Branding restored to defaults.', type: 'success' });
    },
    onError: () => {
      setToast({ message: 'Failed to restore defaults.', type: 'error' });
    },
  });

  const form = useForm<BrandingFormValues>({
    resolver: zodResolver(brandingSchema),
    defaultValues: {
      primaryColor: branding?.primaryColor || '#3b82f6',
      secondaryColor: branding?.secondaryColor || '#64748b',
      accentColor: branding?.accentColor || '#06b6d4',
      backgroundColor: branding?.backgroundColor || '#ffffff',
      textColor: branding?.textColor || '#0f172a',
    },
  });

  useEffect(() => {
    if (branding) {
      form.reset({
        primaryColor: branding.primaryColor,
        secondaryColor: branding.secondaryColor,
        accentColor: branding.accentColor,
        backgroundColor: branding.backgroundColor,
        textColor: branding.textColor,
      });
    }
  }, [branding, form]);

  useEffect(() => {
    if (logoFile) {
      setLogoPreview(URL.createObjectURL(logoFile));
    }
  }, [logoFile]);

  const colors = form.getValues();
  const currentLogo = logoPreview || branding?.logoUrl || null;

  const isPending =
    updateMutation.isPending ||
    uploadLogoMutation.isPending ||
    removeLogoMutation.isPending ||
    resetMutation.isPending;

  const onSubmit = form.handleSubmit(async (values: BrandingFormValues) => {
    await updateMutation.mutateAsync(values);
  });

  const handleLogoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    if (!file) return;
    if (!ACCEPTED_LOGO_TYPES.includes(file.type)) {
      setToast({ message: 'Unsupported format. Use PNG, JPG, JPEG or WebP.', type: 'error' });
      return;
    }
    if (file.size > MAX_LOGO_SIZE) {
      setToast({ message: 'Logo is too large. Maximum size is 2 MiB.', type: 'error' });
      return;
    }
    setLogoFile(file);
    void uploadLogoMutation.mutateAsync(file);
  };

  const handleRemoveLogo = async () => {
    setRemovingLogo(true);
    try {
      await removeLogoMutation.mutateAsync();
    } catch {
      // Error toast set by mutation onError.
    }
  };

  const handleReset = async () => {
    if (!window.confirm('Restore default branding? This removes the logo and resets all colors.')) {
      return;
    }
    try {
      await resetMutation.mutateAsync();
    } catch {
      // Error toast set by mutation onError.
    }
  };

  if (schoolLoading || brandingLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Branding</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  if (schoolError || brandingError) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">Branding</h1>
        <Alert variant="destructive">
          <AlertTitle>Unable to load branding</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(
              schoolError ?? brandingError,
              'The school branding could not be loaded.',
            )}
            <Button
              className="ml-3"
              size="sm"
              variant="outline"
              onClick={() => void refetchBranding()}
            >
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {toast && (
        <Alert variant={toast.type === 'error' ? 'destructive' : 'default'}>
          {toast.type === 'error' ? <AlertTitle>Error</AlertTitle> : <AlertTitle>Success</AlertTitle>}
          <AlertDescription>{toast.message}</AlertDescription>
        </Alert>
      )}

      <div className="flex items-center gap-4">
        <Link to="/platform/schools">
          <Button variant="ghost" size="icon"><ArrowLeft className="h-4 w-4" /></Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">Branding Management</h1>
          <p className="mt-1 text-muted-foreground">
            Configure the branding for <strong>{school?.name}</strong>.
          </p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="col-span-1 lg:col-span-1">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <School className="h-4 w-4" /> School Information
            </CardTitle>
            <CardDescription>
              Identity details for {school?.name}.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Name</p>
              <p className="font-medium">{school?.name}</p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Registration Number</p>
              <p className="font-medium">{school?.registrationNumber || 'Not provided'}</p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Status</p>
              <Badge variant={school?.isSuspended ? 'destructive' : 'success'}>
                {school?.isSuspended ? 'Suspended' : 'Active'}
              </Badge>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Current Logo</p>
              <div className="flex items-center gap-3">
                {currentLogo ? (
                  <img
                    src={currentLogo}
                    alt="School logo"
                    className="h-12 w-12 rounded-lg border object-cover"
                  />
                ) : (
                  <div className="h-12 w-12 rounded-lg border border-dashed flex items-center justify-center text-muted-foreground">
                    <Image className="h-5 w-5" />
                  </div>
                )}
                {branding?.logoUrl && !removingLogo && !uploadLogoMutation.isPending ? (
                  <Button
                    size="sm"
                    variant="destructive"
                    onClick={handleRemoveLogo}
                    disabled={isPending}
                  >
                    <Trash2 className="h-3 w-3 mr-2" /> Remove
                  </Button>
                ) : null}
              </div>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Upload Logo</p>
              <label className="block">
                <Input
                  type="file"
                  accept=".png,.jpg,.jpeg,.webp"
                  aria-label="School logo file"
                  onChange={handleLogoChange}
                  disabled={isPending || uploadLogoMutation.isPending}
                />
              </label>
              {logoFile && (
                <p className="text-xs text-muted-foreground">
                  {logoFile.name} · {(logoFile.size / 1024).toFixed(1)} KB
                </p>
              )}
              {uploadLogoMutation.isPending && (
                <p className="text-xs text-muted-foreground">Uploading…</p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Palette className="h-4 w-4" /> Brand Colors
            </CardTitle>
            <CardDescription>
              Update the school's brand color palette.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <form onSubmit={onSubmit} className="space-y-4">
              {COLOR_FIELDS.map(({ key, label }) => {
                const fieldErrors = form.formState.errors[key];
                return (
                  <div key={key} className="space-y-2">
                    <label className="flex items-center justify-between text-sm">
                      <span>{label}</span>
                      {fieldErrors ? (
                        <span className="text-xs text-destructive">{fieldErrors.message}</span>
                      ) : null}
                    </label>
                    <div className="flex items-center gap-2">
                      <Input
                        type="color"
                        {...form.register(key)}
                        className="h-10 w-20 rounded border cursor-pointer p-0"
                        disabled={updateMutation.isPending}
                      />
                      <Input
                        {...form.register(key)}
                        placeholder="#3b82f6"
                        disabled={updateMutation.isPending}
                      />
                    </div>
                  </div>
                );
              })}
              <div className="pt-2 flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleReset}
                  disabled={isPending || resetMutation.isPending}
                >
                  {resetMutation.isPending && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                  Restore Default
                </Button>
                <Button type="submit" disabled={isPending || updateMutation.isPending}>
                  {updateMutation.isPending && <Save className="h-4 w-4 mr-2 animate-spin" />}
                  Save Changes
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Image className="h-4 w-4" /> Live Preview
            </CardTitle>
            <CardDescription>
              How branding appears in the school portal sidebar and header.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col lg:flex-row gap-6">
              <aside className="w-full lg:w-64 shrink-0" style={previewSidebarStyle(colors)}>
                <div className="flex h-12 items-center justify-between px-4 border-b">
                  <div className="flex items-center gap-2">
                    <div className="h-8 w-8 rounded-lg flex items-center justify-center text-white font-semibold" style={previewBadgeStyle(colors.primaryColor)}>
                      {school?.name?.[0] || 'S'}
                    </div>
                    <span className="font-semibold text-sm text-foreground truncate">{school?.name}</span>
                  </div>
                </div>
                <nav className="p-3 space-y-1">
                  <div className="px-3 py-2 text-xs font-semibold text-muted-foreground uppercase">Navigation</div>
                  <div className="space-y-0.5">
                    {['Dashboard', 'Students', 'Teachers', 'Results'].map((label, i) => (
                      <div
                        key={label}
                        className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors ${
                          i === 0
                            ? 'bg-school-active-nav-background text-white shadow-sm'
                            : 'text-school-sidebar-text hover:bg-school-muted hover:text-foreground'
                        }`}
                        style={previewNavItemStyle(colors.primaryColor, i === 0)}
                      >
                        <div className="h-4 w-4 shrink-0" style={previewIconStyle(i === 0 ? colors.primaryColor : colors.textColor)} />
                        <span className="truncate">{label}</span>
                      </div>
                    ))}
                  </div>
                </nav>
              </aside>

              <div className="flex-1 min-w-0">
                <div className="border rounded-xl p-4" style={previewHeaderStyle(colors)}>
                  <div className="flex items-center gap-3">
                    <button className="p-2 rounded-lg hover:bg-accent" aria-label="Open navigation">
                      <div className="h-5 w-5" style={previewIconStyle(colors.textColor)} />
                    </button>
                    <div className="min-w-0">
                      <h1 className="text-lg font-semibold text-foreground">{school?.name}</h1>
                      <p className="text-xs text-muted-foreground">Branding Management · Super Admin</p>
                    </div>
                  </div>
                </div>
                <div className="mt-4 grid gap-4 md:grid-cols-2">
                  <Card className="p-4" style={previewCardStyle(colors)}>
                    <h3 className="text-sm font-semibold text-foreground mb-2">Overview</h3>
                    <p className="text-sm text-muted-foreground">
                      Welcome to {school?.name}. Manage your school's branding and appearance from this portal.
                    </p>
                    <Button variant="outline" size="sm" className="mt-3" style={previewButtonStyle(colors.primaryColor)}>
                      Get Started
                    </Button>
                  </Card>
                  <Card className="p-4" style={previewCardStyle(colors)}>
                    <h3 className="text-sm font-semibold text-foreground mb-2">Quick Actions</h3>
                    <div className="space-y-2">
                      <Button variant="default" size="sm" className="w-full" disabled style={previewButtonStyle(colors.primaryColor)}>
                        View Students
                      </Button>
                      <Button variant="outline" size="sm" className="w-full" disabled style={previewButtonOutlineStyle(colors)}>
                        View Results
                      </Button>
                    </div>
                  </Card>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {updateMutation.isError && (
        <Alert variant="destructive">
          <AlertTitle>Branding was not updated</AlertTitle>
          <AlertDescription>{getApiErrorMessage(updateMutation.error, 'Please check the colors and try again.')}</AlertDescription>
        </Alert>
      )}
    </div>
  );
}

function previewSidebarStyle(colors: BrandingFormValues) {
  return { backgroundColor: colors.backgroundColor, borderColor: '#e2e8f0' } as React.CSSProperties;
}

function previewBadgeStyle(color: string) {
  return { backgroundColor: color } as React.CSSProperties;
}

function previewNavItemStyle(primaryColor: string, active: boolean) {
  if (active) {
    return { backgroundColor: primaryColor, color: '#ffffff' } as React.CSSProperties;
  }
  return {} as React.CSSProperties;
}

function previewIconStyle(color: string) {
  return { color } as React.CSSProperties;
}

function previewHeaderStyle(colors: BrandingFormValues) {
  return { backgroundColor: colors.backgroundColor, borderColor: '#e2e8f0' } as React.CSSProperties;
}

function previewCardStyle(colors: BrandingFormValues) {
  return { backgroundColor: colors.backgroundColor, borderColor: '#e2e8f0' } as React.CSSProperties;
}

function previewButtonStyle(primaryColor: string) {
  return { backgroundColor: primaryColor, borderColor: primaryColor, color: '#ffffff' } as React.CSSProperties;
}

function previewButtonOutlineStyle(colors: BrandingFormValues) {
  return { borderColor: colors.primaryColor, color: colors.primaryColor } as React.CSSProperties;
}
