import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/services/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';

const defaultSettings = {
  'notifications.enabled': true,
  'email.enabled': true,
  'sms.enabled': true,
  'platform.announcements.enabled': true,
  'manager.account.creation.notifications.enabled': true,
};

export function PlatformSettingsPage() {
  const queryClient = useQueryClient();
  const [settings, setSettings] = useState<Record<string, boolean>>({});

  const { data, isLoading } = useQuery({
    queryKey: ['platform-settings'],
    queryFn: () => api.get('/platform/settings').then((r) => r.data.data),
  });

  useEffect(() => {
    const next = { ...defaultSettings, ...(data?.settings ?? {}) };
    setSettings(Object.fromEntries(Object.entries(next).map(([key, value]) => [key, Boolean(value)])));
  }, [data]);

  const updateMutation = useMutation({
    mutationFn: (payload: Record<string, boolean>) => api.put('/platform/settings', payload).then((r) => r.data.data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['platform-settings'] }),
  });

  const handleToggle = (key: string, value: boolean) => {
    const payload = { ...settings, [key]: value };
    setSettings(payload);
    updateMutation.mutate(payload);
  };

  if (isLoading) {
    return <div className="text-muted-foreground">Loading settings…</div>;
  }

  const rows = [
    { key: 'notifications.enabled', label: 'Notifications', description: 'Enable platform notifications and alerts' },
    { key: 'email.enabled', label: 'Email', description: 'Allow system emails to be sent' },
    { key: 'sms.enabled', label: 'SMS', description: 'Allow SMS delivery for campaigns and alerts' },
    { key: 'platform.announcements.enabled', label: 'Announcements', description: 'Show platform-wide announcements' },
    { key: 'manager.account.creation.notifications.enabled', label: 'Manager account alerts', description: 'Notify managers when account creation events happen' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Platform Settings</h1>
        <p className="mt-1 text-muted-foreground">Configure global platform behavior</p>
      </div>

      <Card className="p-6">
        <div className="space-y-4">
          {rows.map((row) => (
            <div key={row.key} className="flex items-center justify-between rounded-lg border p-4">
              <div>
                <p className="font-medium">{row.label}</p>
                <p className="text-sm text-muted-foreground">{row.description}</p>
              </div>
              <div className="flex items-center gap-3">
                <Badge variant={settings[row.key] ? 'success' : 'secondary'}>{settings[row.key] ? 'Enabled' : 'Disabled'}</Badge>
                <Button
                  type="button"
                  variant={settings[row.key] ? 'default' : 'outline'}
                  onClick={() => handleToggle(row.key, !settings[row.key])}
                  disabled={updateMutation.isPending}
                >
                  {settings[row.key] ? 'Disable' : 'Enable'}
                </Button>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card className="p-6">
        <h2 className="text-lg font-semibold">Platform identity</h2>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <div>
            <label className="text-sm font-medium">Platform name</label>
            <Input value="EduManage" readOnly />
          </div>
          <div>
            <label className="text-sm font-medium">Default currency</label>
            <Input value="TZS (TSh)" readOnly />
          </div>
        </div>
      </Card>
    </div>
  );
}

