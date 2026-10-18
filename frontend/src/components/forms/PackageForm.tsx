import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import type { CreatePackageRequest, PackageResponse } from '@/types/api';

const packageSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  displayName: z.string().min(1, 'Display name is required'),
  description: z.string().optional().or(z.literal('')),
  price: z.number({ invalid_type_error: 'Price must be a number' }).min(0, 'Price must be positive'),
  billingPeriod: z.string().min(1, 'Billing period is required'),
  isActive: z.boolean().optional(),
  isDefault: z.boolean().optional(),
  sortOrder: z.number().optional(),
});

type PackageFormValues = z.infer<typeof packageSchema>;

interface PackageFormProps {
  initialData?: Partial<PackageResponse> & { id?: number };
  onSubmit: (data: CreatePackageRequest) => Promise<void>;
  submitLabel?: string;
  onCancel?: () => void;
}

export function PackageForm({ initialData, onSubmit, submitLabel = 'Create Package', onCancel }: PackageFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<PackageFormValues>({
    resolver: zodResolver(packageSchema),
    defaultValues: {
      name: initialData?.name || '',
      displayName: initialData?.displayName || '',
      description: initialData?.description || '',
      price: initialData?.price || 0,
      billingPeriod: initialData?.billingPeriod || 'MONTHLY',
      isActive: initialData?.active ?? true,
      isDefault: initialData?.default ?? false,
      sortOrder: initialData?.sortOrder || 0,
    },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        name: initialData.name || '',
        displayName: initialData.displayName || '',
        description: initialData.description || '',
        price: initialData.price || 0,
        billingPeriod: initialData.billingPeriod || 'MONTHLY',
        isActive: initialData.active ?? true,
        isDefault: initialData.default ?? false,
        sortOrder: initialData.sortOrder || 0,
      });
    }
  }, [initialData, reset]);

  const handleFormSubmit = async (data: PackageFormValues) => {
    const { isActive, isDefault, ...packageFields } = data;
    await onSubmit({
      ...packageFields,
      active: isActive,
      default: isDefault,
    });
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      <div className="space-y-4">
        <h3 className="text-lg font-medium">Package Information</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Name *</label>
            <Input placeholder="basic" {...register('name')} />
            {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Display Name *</label>
            <Input placeholder="Basic Plan" {...register('displayName')} />
            {errors.displayName && <p className="text-sm text-destructive">{errors.displayName.message}</p>}
          </div>
          <div className="space-y-2 sm:col-span-2">
            <label className="text-sm font-medium">Description</label>
            <Input placeholder="Package description..." {...register('description')} />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Price *</label>
            <Input type="number" placeholder="99.00" {...register('price', { valueAsNumber: true })} />
            {errors.price && <p className="text-sm text-destructive">{errors.price.message}</p>}
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Billing Period *</label>
            <Input placeholder="MONTHLY" {...register('billingPeriod')} />
            {errors.billingPeriod && <p className="text-sm text-destructive">{errors.billingPeriod.message}</p>}
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Sort Order</label>
            <Input type="number" {...register('sortOrder', { valueAsNumber: true })} />
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" id="isActive" {...register('isActive')} className="h-4 w-4" />
            <label htmlFor="isActive" className="text-sm">Active</label>
          </div>
          <div className="flex items-center gap-2">
            <input type="checkbox" id="isDefault" {...register('isDefault')} className="h-4 w-4" />
            <label htmlFor="isDefault" className="text-sm">Default</label>
          </div>
        </div>
      </div>

      <div className="flex justify-end gap-3 pt-4 border-t">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancel
          </Button>
        )}
        <Button type="submit">
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
