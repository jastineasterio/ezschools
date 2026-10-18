import { useQuery } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Skeleton } from '@/components/ui/Skeleton';
import { Mail, Phone, MapPin } from 'lucide-react';

export function ManagerSchoolPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['school-profile'],
    queryFn: () => api.get('/school/profile').then(r => r.data),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold">School Profile</h1>
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">School Profile</h1>

      <Card className="p-6">
        <h2 className="text-lg font-semibold mb-4">School Information</h2>
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <p className="text-sm text-muted-foreground">Name</p>
            <p className="font-medium">{data?.name}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Registration Number</p>
            <p className="font-medium">{data?.registrationNumber || '-'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Email</p>
            <p className="font-medium flex items-center gap-2"><Mail className="h-4 w-4" /> {data?.email || '-'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Phone</p>
            <p className="font-medium flex items-center gap-2"><Phone className="h-4 w-4" /> {data?.phone || '-'}</p>
          </div>
          <div className="md:col-span-2">
            <p className="text-sm text-muted-foreground">Address</p>
            <p className="font-medium flex items-center gap-2"><MapPin className="h-4 w-4" /> {data?.address || '-'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Region</p>
            <p className="font-medium">{data?.region || '-'}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground">District</p>
            <p className="font-medium">{data?.district || '-'}</p>
          </div>
        </div>
      </Card>
    </div>
  );
}

