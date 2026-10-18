import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="flex h-screen flex-col items-center justify-center">
      <h1 className="text-6xl font-bold text-muted-foreground">404</h1>
      <p className="mt-4 text-muted-foreground">Page not found</p>
      <Link to="/login" className="mt-6 text-primary hover:underline">Go to login</Link>
    </div>
  );
}

