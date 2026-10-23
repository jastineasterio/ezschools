import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/store/AuthContext';
import { getDashboardPath } from '@/lib/roles';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import { Alert } from '@/components/ui/Alert';
import { Loader2, School } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginForm) => {
    setLoading(true);
    setError(null);

    try {
      const userData = await login(data.email, data.password);
      navigate(getDashboardPath(userData.roles), { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md">
        <div className="mb-6 flex flex-col items-center">
          <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-white">
            <School className="h-6 w-6" />
          </div>

          <h1 className="text-2xl font-bold">EduManage Platform</h1>

          <p className="mt-1 text-sm text-muted-foreground">
            Sign in to your account
          </p>
        </div>

        {error && <Alert variant="destructive" message={error} />}

        <form
          onSubmit={handleSubmit(onSubmit)}
          className="space-y-4"
        >
          <div>
            <label className="label">Email</label>

            <Input
              type="email"
              placeholder="admin@edumanage.com"
              {...register('email')}
            />

            {errors.email && (
              <p className="mt-1 text-sm text-red-600">
                {errors.email.message}
              </p>
            )}
          </div>

          <div>
            <label className="label">Password</label>

            <Input
              type="password"
              placeholder="��������"
              {...register('password')}
            />

            {errors.password && (
              <p className="mt-1 text-sm text-red-600">
                {errors.password.message}
              </p>
            )}
          </div>

          <Button
            type="submit"
            className="w-full"
            disabled={loading}
          >
            {loading && (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            )}

            Sign In
          </Button>
        </form>

        <div className="mt-6 rounded-lg border border-accent bg-accent p-4 text-sm">
          <p className="font-medium text-accent-foreground">
            Development Credentials
          </p>

          <p className="mt-1 text-sm text-accent-foreground">
            Email: admin@edumanage.com
          </p>

          <p className="mt-1 text-sm text-accent-foreground">
            Password: Admin@123
          </p>
        </div>
      </Card>
    </div>
  );
}
