import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import type { CreateSchoolRequest, UpdateSchoolRequest, SchoolResponse } from '@/types/api';

const createSchoolSchema = z.object({
  name: z.string().min(1, 'School name is required'),
  registrationNumber: z.string().min(1, 'Registration number is required'),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  phone: z.string().optional().or(z.literal('')),
  address: z.string().optional().or(z.literal('')),
  region: z.string().optional().or(z.literal('')),
  district: z.string().optional().or(z.literal('')),
});

const updateSchoolSchema = z.object({
  name: z.string().min(1, 'School name is required'),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  phone: z.string().optional().or(z.literal('')),
  address: z.string().optional().or(z.literal('')),
  region: z.string().optional().or(z.literal('')),
  district: z.string().optional().or(z.literal('')),
});

type CreateSchoolFormValues = z.infer<typeof createSchoolSchema>;
type UpdateSchoolFormValues = z.infer<typeof updateSchoolSchema>;

interface SchoolFormProps {
  initialData?: Partial<SchoolResponse> & { id?: number };
  onSubmit: (data: CreateSchoolRequest | UpdateSchoolRequest) => Promise<void>;
  submitLabel?: string;
  onCancel?: () => void;
}

export function SchoolForm({ initialData, onSubmit, submitLabel = 'Create School', onCancel }: SchoolFormProps) {
  const isEditing = !!initialData?.id;
  const schema = isEditing ? updateSchoolSchema : createSchoolSchema;

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<CreateSchoolFormValues | UpdateSchoolFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: initialData?.name || '',
      registrationNumber: initialData?.registrationNumber || '',
      email: initialData?.email || '',
      phone: initialData?.phone || '',
      address: initialData?.address || '',
      region: initialData?.region || '',
      district: initialData?.district || '',
    },
  });

  useEffect(() => {
    if (initialData) {
      reset({
        name: initialData.name || '',
        registrationNumber: initialData.registrationNumber || '',
        email: initialData.email || '',
        phone: initialData.phone || '',
        address: initialData.address || '',
        region: initialData.region || '',
        district: initialData.district || '',
      });
    }
  }, [initialData, reset]);

  const handleFormSubmit = async (data: CreateSchoolFormValues | UpdateSchoolFormValues) => {
    await onSubmit(data as CreateSchoolRequest | UpdateSchoolRequest);
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      <div className="space-y-4">
        <h3 className="text-lg font-medium">School Information</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">School Name *</label>
            <Input placeholder="Greenwood High School" {...register('name')} />
            {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
          </div>
          {!isEditing && (
            <div className="space-y-2">
              <label className="text-sm font-medium">Registration Number *</label>
              <Input placeholder="REG-001" {...register('registrationNumber')} />
              {'registrationNumber' in errors && errors.registrationNumber && <p className="text-sm text-destructive">{errors.registrationNumber.message}</p>}
            </div>
          )}
          <div className="space-y-2">
            <label className="text-sm font-medium">Email</label>
            <Input type="email" placeholder="info@school.edu" {...register('email')} />
            {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Phone</label>
            <Input placeholder="+1 234 567 8900" {...register('phone')} />
            {errors.phone && <p className="text-sm text-destructive">{errors.phone.message}</p>}
          </div>
          <div className="space-y-2 sm:col-span-2">
            <label className="text-sm font-medium">Address</label>
            <Input placeholder="123 Main Street" {...register('address')} />
            {errors.address && <p className="text-sm text-destructive">{errors.address.message}</p>}
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Region</label>
            <Input placeholder="Greater Accra" {...register('region')} />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">District</label>
            <Input placeholder="Accra Metropolitan" {...register('district')} />
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
