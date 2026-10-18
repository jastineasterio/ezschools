import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/store/AuthProvider';
import { BrandingProvider } from '@/store/BrandingProvider';
import { ThemeProvider } from '@/store/ThemeContext';
import { queryClient } from '@/lib/query';
import { App } from '@/App';
import '@/index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <BrandingProvider>
            <ThemeProvider>
              <App />
            </ThemeProvider>
          </BrandingProvider>
        </AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  </React.StrictMode>
);
