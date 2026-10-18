import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { ArrowLeft, Package, Users, Calendar, Mail, Phone, MapPin } from 'lucide-react';

export function SchoolDetailPage() {
  const { id } = useParams();
  const { data, isLoading } = useQuery({
    queryKey: ['school', id],
    queryFn: () => api.get(`/platform/schools/${id}`).then(r => r.data.data),
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Card className="p-6"><Skeleton className="h-64" /></Card>
      </div>
    );
  }

  const school = data;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/platform/schools">
          <Button variant="ghost" size="icon"><ArrowLeft className="h-4 w-4" /></Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">{school?.name}</h1>
          <p className="mt-1 text-muted-foreground">School Details</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="text-lg font-semibold mb-4">School Information</h2>
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Mail className="h-4 w-4" /> {school?.email || 'No email'}
            </div>
            <div className="flex items-center gap-2 text-muted-foreground">
              <Phone className="h-4 w-4" /> {school?.phone || 'No phone'}
            </div>
            <div className="flex items-center gap-2 text-muted-foreground">
              <MapPin className="h-4 w-4" /> {school?.address || 'No address'}
            </div>
            <div className="flex items-center gap-2 text-muted-foreground">
              <Calendar className="h-4 w-4" /> Created {new Date(school?.createdAt).toLocaleDateString()}
            </div>
            <div>
              <Badge variant={school?.suspended ? 'destructive' : 'success'}>
                {school?.suspended ? 'Suspended' : 'Active'}
              </Badge>
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="text-lg font-semibold mb-4">Subscription</h2>
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-muted-foreground">
              <Package className="h-4 w-4" /> {school?.package?.displayName || 'No package'}
            </div>
            <div className="flex items-center gap-2 text-muted-foreground">
              <Users className="h-4 w-4" /> {school?.userCount || 0} users
            </div>
            <div>
              <Badge variant="outline">{school?.subscription?.status || 'No subscription'}</Badge>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}

