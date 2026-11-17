import { useState, useEffect, type FormEventHandler, type ReactNode } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, type UseFormRegister, type FieldErrors } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import { Badge } from '@/components/ui/Badge';
import { Avatar } from '@/components/ui/Avatar';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Select } from '@/components/ui/Select';
import {
  getMyTeacherProfileDetail,
  updateMyTeacherProfile,
  changePassword,
} from '@/services/people';
import type { UpdateTeacherProfileRequest, ChangePasswordRequest } from '@/types/api';
import { getApiErrorMessage } from '@/services/api';
import { formatDate } from '@/lib/format';
import { getRoleDisplayName, normalizeRole, type DashboardRole } from '@/lib/roles';
import {
  User,
  Mail,
  Phone,
  Building2,
  Calendar,
  Shield,
  Clock,
  Lock,
  Edit,
  Loader2,
  Activity,
} from 'lucide-react';

const updateSchema = z.object({
  firstName: z.string().min(1, 'First name is required'),
  lastName: z.string().min(1, 'Last name is required'),
  email: z.string().email('Invalid email format').min(1, 'Email is required'),
  phone: z.string().optional(),
  gender: z.string().optional(),
  dateOfBirth: z.string().optional(),
  photoUrl: z.string().optional(),
});

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password is required'),
  newPassword: z.string().min(6, 'New password must be at least 6 characters'),
  confirmPassword: z.string().min(1, 'Confirm your new password'),
});

type UpdateFormValues = z.infer<typeof updateSchema>;
type PasswordFormValues = z.infer<typeof passwordSchema>;

const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
];

function initials(firstName: string | null, lastName: string | null): string {
  const first = (firstName ?? '').trim();
  const last = (lastName ?? '').trim();
  if (first && last) return `${first[0]}${last[0]}`.toUpperCase();
  if (first) return first[0].toUpperCase();
  if (last) return last[0].toUpperCase();
  return '?';
}

export function TeacherProfilePage() {
  const queryClient = useQueryClient();
  const [editOpen, setEditOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const query = useQuery({
    queryKey: ['teacher-profile'],
    queryFn: () => getMyTeacherProfileDetail(),
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateTeacherProfileRequest) => updateMyTeacherProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teacher-profile'] });
      setToast({ message: 'Profile updated successfully.', type: 'success' });
      setEditOpen(false);
    },
    onError: (error: unknown) => {
      setToast({
        message: getApiErrorMessage(error, 'Failed to update profile.'),
        type: 'error',
      });
    },
  });

  const passwordMutation = useMutation({
    mutationFn: (data: ChangePasswordRequest) => changePassword(data),
    onSuccess: () => {
      setToast({ message: 'Password changed successfully. Use your new password to sign in.', type: 'success' });
      setPasswordOpen(false);
    },
    onError: (error: unknown) => {
      setToast({
        message: getApiErrorMessage(error, 'Failed to change password.'),
        type: 'error',
      });
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting: updatePending },
  } = useForm<UpdateFormValues>({
    resolver: zodResolver(updateSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      gender: '',
      dateOfBirth: '',
      photoUrl: '',
    },
  });

  const {
    register: registerPassword,
    handleSubmit: handleSubmitPassword,
    formState: { errors: passwordErrors, isSubmitting: passwordPending },
  } = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const profile = query.data;

  useEffect(() => {
    if (editOpen && profile) {
      reset({
        firstName: profile.firstName ?? '',
        lastName: profile.lastName ?? '',
        email: profile.email ?? '',
        phone: profile.phone ?? '',
        gender: profile.gender ?? '',
        dateOfBirth: profile.dateOfBirth ?? '',
        photoUrl: profile.photoUrl ?? '',
      });
    }
  }, [editOpen, profile, reset]);

  if (query.isLoading) {
    return (
      <main className="mx-auto max-w-4xl space-y-6">
        <header>
          <h1 className="text-3xl font-bold">My Profile</h1>
          <p className="mt-1 text-muted-foreground">Your account information.</p>
        </header>
        <div className="grid gap-4 md:grid-cols-3">
          {[1, 2, 3, 4, 5].map((i) => (
            <Card key={i} className="p-6">
              <SkeletonBlock className="h-8 w-full" />
              <SkeletonBlock className="mt-3 h-4 w-3/4" />
            </Card>
          ))}
        </div>
      </main>
    );
  }

  if (query.isError) {
    return (
      <main className="mx-auto max-w-4xl space-y-6">
        <header>
          <h1 className="text-3xl font-bold">My Profile</h1>
          <p className="mt-1 text-muted-foreground">Your account information.</p>
        </header>
        <Alert variant="destructive">
          <AlertTitle>Unable to load profile</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(query.error, 'The profile could not be loaded.')}{' '}
            <Button
              className="ml-3"
              size="sm"
              variant="outline"
              onClick={() => void query.refetch()}
            >
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      </main>
    );
  }

  if (!profile) {
    return (
      <main className="mx-auto max-w-4xl space-y-6">
        <header>
          <h1 className="text-3xl font-bold">My Profile</h1>
          <p className="mt-1 text-muted-foreground">Your account information.</p>
        </header>
        <Card>
          <CardContent className="p-6 text-center text-muted-foreground">
            No profile found.
          </CardContent>
        </Card>
      </main>
    );
  }

  const roleLabel = profile.role
    ? getRoleDisplayName(normalizeRole(profile.role) as DashboardRole)
    : 'Teacher';
  const fullName = [profile.firstName, profile.lastName]
    .filter(Boolean)
    .join(' ')
    .trim() || profile.email || 'Teacher';

  const handleUpdate = (data: UpdateFormValues) => {
    const payload: UpdateTeacherProfileRequest = {
      firstName: data.firstName,
      lastName: data.lastName,
      email: data.email,
      phone: data.phone || undefined,
      gender: data.gender || undefined,
      dateOfBirth: data.dateOfBirth || undefined,
      photoUrl: data.photoUrl || undefined,
    };
    updateMutation.mutate(payload);
  };

  const handlePassword = (data: PasswordFormValues) => {
    if (data.newPassword !== data.confirmPassword) {
      setToast({ message: 'New passwords do not match.', type: 'error' });
      return;
    }
    passwordMutation.mutate({
      currentPassword: data.currentPassword,
      newPassword: data.newPassword,
      confirmPassword: data.confirmPassword,
    });
  };

  return (
    <main className="mx-auto max-w-4xl space-y-6">
      {toast && (
        <Alert variant={toast.type === 'error' ? 'destructive' : 'default'} className="mb-4">
          {toast.type === 'error' ? <AlertTitle>Error</AlertTitle> : <AlertTitle>Success</AlertTitle>}
          <AlertDescription>{toast.message}</AlertDescription>
        </Alert>
      )}

      <header>
        <h1 className="text-3xl font-bold">My Profile</h1>
        <p className="mt-1 text-muted-foreground">Your account information.</p>
      </header>

      <Card className="relative overflow-hidden">
        <CardContent className="flex flex-col md:flex-row md:items-center gap-6 p-6">
          <div className="flex flex-col items-center md:text-left">
            {profile.photoUrl ? (
              <img src={profile.photoUrl} alt="Profile" className="h-24 w-24 rounded-full object-cover mb-3" />
            ) : (
              <Avatar className="h-24 w-24 mb-3" fallback={initials(profile.firstName, profile.lastName)} />
            )}
            <h2 className="text-2xl font-bold">{fullName}</h2>
            <div className="flex items-center gap-2 mt-1">
              <Badge variant="secondary">{roleLabel}</Badge>
              {profile.isActive === false && (
                <Badge variant="destructive">Inactive</Badge>
              )}
            </div>
          </div>
          <div className="ml-auto md:ml-0 flex flex-col gap-3 w-full md:w-auto justify-end">
            <Button onClick={() => setEditOpen(true)} className="w-full md:w-auto">
              <Edit className="h-4 w-4 mr-2" /> Edit Profile
            </Button>
            <Button variant="outline" onClick={() => setPasswordOpen(true)} className="w-full md:w-auto">
              <Lock className="h-4 w-4 mr-2" /> Change Password
            </Button>
          </div>
        </CardContent>
        <div className="border-t border-border bg-muted/50 p-4 text-center md:text-left">
          <p className="text-sm font-medium">{profile.schoolName || 'School not assigned'}</p>
          {profile.schoolCode && (
            <p className="text-xs text-muted-foreground">School code: {profile.schoolCode}</p>
          )}
        </div>
      </Card>

      <div className="grid gap-6 md:grid-cols-3">
        <Card className="col-span-1 md:col-span-2">
          <CardHeader>
            <CardTitle>Personal Information</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <ProfileField label="First name" value={profile.firstName ?? 'Not provided'} icon={<User className="h-4 w-4" />} />
            <ProfileField label="Last name" value={profile.lastName ?? 'Not provided'} icon={<User className="h-4 w-4" />} />
            <ProfileField label="Email address" value={profile.email ?? 'Not provided'} icon={<Mail className="h-4 w-4" />} />
            <ProfileField label="Phone number" value={profile.phone ?? 'Not provided'} icon={<Phone className="h-4 w-4" />} />
            <ProfileField label="Gender" value={profile.gender ?? 'Not provided'} />
            <ProfileField label="Date of birth" value={formatDate(profile.dateOfBirth)} icon={<Calendar className="h-4 w-4" />} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Employment Information</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <ProfileField label="Role" value={roleLabel} icon={<Shield className="h-4 w-4" />} />
            <ProfileField label="School" value={profile.schoolName ?? 'Not assigned'} icon={<Building2 className="h-4 w-4" />} />
            <ProfileField label="School code" value={profile.schoolCode ?? 'Not provided'} />
            <ProfileField label="Employee number" value={profile.employeeNumber ?? 'Not provided'} />
            <ProfileField label="Employment status" value={profile.employmentStatus ?? 'Not provided'} />
            <ProfileField label="Qualification" value={profile.qualification ?? 'Not provided'} />
            <ProfileField label="Experience (years)" value={profile.experienceYears == null ? 'Not provided' : String(profile.experienceYears)} />
            <ProfileField label="Date joined" value={formatDate(profile.createdAt ?? undefined)} icon={<Clock className="h-4 w-4" />} />
          </CardContent>
        </Card>

        <Card className="col-span-1 md:col-span-1">
          <CardHeader>
            <CardTitle>Account Information</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4">
            <ProfileField label="Account status" value={profile.isActive ? 'Active' : 'Inactive'} icon={<Shield className="h-4 w-4" />} />
            <ProfileField label="Email verification" value="Not supported" />
            <ProfileField label="Account created" value={formatDate(profile.createdAt ?? undefined)} icon={<Clock className="h-4 w-4" />} />
            <ProfileField label="Last updated" value={formatDate(profile.updatedAt ?? undefined)} icon={<Clock className="h-4 w-4" />} />
            <ProfileField label="Last login" value={formatDate(profile.lastLogin ?? undefined)} icon={<Activity className="h-4 w-4" />} />
          </CardContent>
        </Card>
      </div>

      <EditProfileDialog
        open={editOpen}
        onOpenChange={() => setEditOpen(false)}
        onSubmit={handleSubmit(handleUpdate)}
        pending={updatePending}
        register={register}
        errors={errors}
      />

      <ChangePasswordDialog
        open={passwordOpen}
        onOpenChange={() => setPasswordOpen(false)}
        onSubmit={handleSubmitPassword(handlePassword)}
        pending={passwordPending}
        register={registerPassword}
        errors={passwordErrors}
      />
    </main>
  );
}

function SkeletonBlock({ className }: { className?: string }) {
  return <div className={className} />;
}

function ProfileField({ label, value, icon }: { label: string; value: ReactNode; icon?: ReactNode }) {
  return (
    <div className="flex items-start gap-3 p-3 rounded-md border border-border bg-muted/50">
      {icon && <div className="flex h-8 w-8 items-center justify-center rounded text-muted-foreground shrink-0">{icon}</div>}
      <div className="min-w-0 flex-1">
        <p className="text-xs text-muted-foreground leading-none">{label}</p>
        <p className="text-sm font-medium">{value}</p>
      </div>
    </div>
  );
}

function EditProfileDialog({
  open,
  onOpenChange,
  onSubmit,
  pending,
  register,
  errors,
}: {
  open: boolean;
  onOpenChange: () => void;
  onSubmit: FormEventHandler<HTMLFormElement>;
  pending: boolean;
  register: UseFormRegister<UpdateFormValues>;
  errors: FieldErrors<UpdateFormValues>;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title="Edit Profile" description="Update your personal information.">
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-1">
              <label className="label">First name</label>
              <Input {...register('firstName')} placeholder="First name" />
              {errors.firstName && (
                <p className="text-xs text-destructive">{errors.firstName.message}</p>
              )}
            </div>
            <div className="space-y-1">
              <label className="label">Last name</label>
              <Input {...register('lastName')} placeholder="Last name" />
              {errors.lastName && (
                <p className="text-xs text-destructive">{errors.lastName.message}</p>
              )}
            </div>
          </div>
          <div className="space-y-1">
            <label className="label">Email address</label>
            <Input type="email" {...register('email')} placeholder="name@school.com" />
            {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
          </div>
          <div className="space-y-1">
            <label className="label">Phone number</label>
            <Input type="tel" {...register('phone')} placeholder="+255 7xx xxx xxx" />
            {errors.phone && <p className="text-xs text-destructive">{errors.phone.message}</p>}
          </div>
          <div className="space-y-1">
            <label className="label">Gender</label>
            <Select {...register('gender')} options={GENDER_OPTIONS} />
            {errors.gender && <p className="text-xs text-destructive">{errors.gender.message}</p>}
          </div>
          <div className="space-y-1">
            <label className="label">Date of birth</label>
            <Input type="date" {...register('dateOfBirth')} />
            {errors.dateOfBirth && <p className="text-xs text-destructive">{errors.dateOfBirth.message}</p>}
          </div>
          <div className="space-y-1">
            <label className="label">Profile photo URL</label>
            <Input type="url" {...register('photoUrl')} placeholder="https://example.com/photo.jpg" />
            {errors.photoUrl && <p className="text-xs text-destructive">{errors.photoUrl.message}</p>}
          </div>
          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button type="button" variant="outline" onClick={onOpenChange}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              {pending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : 'Save changes'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ChangePasswordDialog({
  open,
  onOpenChange,
  onSubmit,
  pending,
  register,
  errors,
}: {
  open: boolean;
  onOpenChange: () => void;
  onSubmit: FormEventHandler<HTMLFormElement>;
  pending: boolean;
  register: UseFormRegister<PasswordFormValues>;
  errors: FieldErrors<PasswordFormValues>;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title="Change Password" description="Enter your current password and a new password.">
        <form onSubmit={onSubmit} className="space-y-4">
          <div className="space-y-1">
            <label className="label">Current password</label>
            <Input type="password" {...register('currentPassword')} placeholder="Current password" />
            {errors.currentPassword && (
              <p className="text-xs text-destructive">{errors.currentPassword.message}</p>
            )}
          </div>
          <div className="space-y-1">
            <label className="label">New password</label>
            <Input type="password" {...register('newPassword')} placeholder="New password" />
            {errors.newPassword && (
              <p className="text-xs text-destructive">{errors.newPassword.message}</p>
            )}
          </div>
          <div className="space-y-1">
            <label className="label">Confirm new password</label>
            <Input type="password" {...register('confirmPassword')} placeholder="Confirm new password" />
            {errors.confirmPassword && (
              <p className="text-xs text-destructive">{errors.confirmPassword.message}</p>
            )}
          </div>
          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button type="button" variant="outline" onClick={onOpenChange}>
              Cancel
            </Button>
            <Button type="submit" disabled={pending}>
              {pending ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : 'Change password'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
