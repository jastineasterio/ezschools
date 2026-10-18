import { Link } from 'react-router-dom';
import { MessageSquare } from 'lucide-react';
import { Card } from '@/components/ui/Card';

export function ManagerSmsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">SMS</h1>
      <Card className="p-6">
        <div className="flex items-start gap-4">
          <MessageSquare className="mt-1 h-6 w-6 text-primary" />
          <div className="space-y-3">
            <h2 className="font-semibold">Send school SMS from Communication Center</h2>
            <p className="text-sm text-muted-foreground">
              Recipients are resolved from your assigned school and SMS delivery status is tracked per recipient.
            </p>
            <Link
              to="/school/notifications"
              className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Open Communication Center
            </Link>
          </div>
        </div>
      </Card>
    </div>
  );
}

