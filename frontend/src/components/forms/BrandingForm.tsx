import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import type { UpdateBrandingRequest } from '@/types/api';
import { Loader2 } from 'lucide-react';

const brandingSchema = z.object({
  primaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  secondaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  accentColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  backgroundColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  textColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/, 'Invalid hex color'),
  logoUrl: z.string().url().optional().or(z.literal('')),
  faviconUrl: z.string().url().optional().or(z.literal('')),
});

type BrandingFormValues = z.infer<typeof brandingSchema>;

interface BrandingFormProps {
  initialData?: Partial<UpdateBrandingRequest>;
  onSubmit: (data: UpdateBrandingRequest) => Promise<void>;
  submitLabel?: string;
  onCancel?: () => void;
}

export function BrandingForm({ initialData, onSubmit, submitLabel = 'Save Branding', onCancel }: BrandingFormProps) {
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BrandingFormValues>({
    resolver: zodResolver(brandingSchema),
    defaultValues: {
      primaryColor: initialData?.primaryColor || '#3b82f6',
      secondaryColor: initialData?.secondaryColor || '#64748b',
      accentColor: initialData?.accentColor || '#06b6d4',
      backgroundColor: initialData?.backgroundColor || '#ffffff',
      textColor: initialData?.textColor || '#0f172a',
      logoUrl: initialData?.logoUrl || '',
      faviconUrl: initialData?.faviconUrl || '',
    },
  });

  const handleFormSubmit = async (data: BrandingFormValues) => {
    setIsSubmitting(true);
    setError(null);
    try {
      await onSubmit(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Operation failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      {error && (
        <Alert variant="destructive">
          <AlertTitle>Error</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <div className="space-y-4">
        <h3 className="text-lg font-medium">Colors</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[
            { key: 'primaryColor', label: 'Primary Color', default: '#3b82f6' },
            { key: 'secondaryColor', label: 'Secondary Color', default: '#64748b' },
            { key: 'accentColor', label: 'Accent Color', default: '#06b6d4' },
            { key: 'backgroundColor', label: 'Background Color', default: '#ffffff' },
            { key: 'textColor', label: 'Text Color', default: '#0f172a' },
          ].map((color) => (
            <div key={color.key} className="space-y-2">
              <label className="text-sm font-medium">{color.label}</label>
              <div className="flex gap-2">
                <Input type="color" {...register(color.key as keyof BrandingFormValues)} className="w-16 h-10 p-1" />
                <Input placeholder={color.default} {...register(color.key as keyof BrandingFormValues)} />
              </div>
              {errors[color.key as keyof BrandingFormValues] && (
                <p className="text-sm text-destructive">
                  {String(errors[color.key as keyof BrandingFormValues]?.message)}
                </p>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-lg font-medium">Media</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Logo URL</label>
            <Input placeholder="https://example.com/logo.png" {...register('logoUrl')} />
            {errors.logoUrl && <p className="text-sm text-destructive">{errors.logoUrl.message}</p>}
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Favicon URL</label>
            <Input placeholder="https://example.com/favicon.ico" {...register('faviconUrl')} />
            {errors.faviconUrl && <p className="text-sm text-destructive">{errors.faviconUrl.message}</p>}
          </div>
        </div>
      </div>

      <div className="flex justify-end gap-3">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancel
          </Button>
        )}
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
