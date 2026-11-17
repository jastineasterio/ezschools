import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/store/AuthContext';
import { getDashboardPath } from '@/lib/roles';
import { getApiErrorMessage } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import { Alert } from '@/components/ui/Alert';
import { Loader2, School, Eye, EyeOff } from 'lucide-react';
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
  const [showPassword, setShowPassword] = useState(false);

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
      setError(getApiErrorMessage(err, 'Login failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background px-4 py-8">
      <Card className="w-full max-w-[440px] rounded-2xl border border-border shadow-xl overflow-hidden">
        <div className="p-8 sm:p-10">
          <div className="mb-6 flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-primary-foreground">
              <School className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-foreground">EduManage</h1>
              <p className="text-xs text-muted-foreground">School Management Platform</p>
            </div>
          </div>

          <h2 className="mb-2 text-2xl font-bold tracking-tight text-foreground">Welcome back</h2>
          <p className="mb-8 text-sm text-muted-foreground">Sign in to your EduManage account</p>

          {error && <Alert variant="destructive" title="Login failed" description={error} className="mb-6" />}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div>
              <label className="mb-2 block text-sm font-medium text-foreground">Email address</label>
              <Input
                type="email"
                autoComplete="email"
                placeholder="name@school.edu"
                {...register('email')}
              />
              {errors.email && (
                <p className="mt-1 text-xs text-destructive">{errors.email.message}</p>
              )}
            </div>

            <div className="relative">
              <label className="mb-2 block text-sm font-medium text-foreground">Password</label>
              <Input
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="••••••••"
                className="pr-10"
                {...register('password')}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-3 flex items-center pr-2 text-muted-foreground hover:text-foreground transition-colors"
                aria-label="Toggle password visibility"
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
              {errors.password && (
                <p className="mt-1 text-xs text-destructive">{errors.password.message}</p>
              )}
            </div>

            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 text-sm text-muted-foreground cursor-pointer select-none">
                <input type="checkbox" className="rounded border-input text-primary focus:ring-ring" />
                Remember me
              </label>
              <a href="#" className="text-sm font-medium text-primary hover:text-primary/80">
                Forgot password?
              </a>
            </div>

            <Button
              type="submit"
              className="w-full h-11 text-base font-semibold shadow-sm hover:shadow-md transition-all"
              disabled={loading}
            >
              {loading && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Sign In
            </Button>

            <p className="mt-6 text-center text-sm text-muted-foreground">
              Don't have an account?{' '}
              <a href="#" className="font-medium text-primary hover:text-primary/80">
                Create account
              </a>
            </p>
          </form>
        </div>
      </Card>
    </div>
  );
}
