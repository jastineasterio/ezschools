import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';

import { PlatformNotificationManagementPage } from './PlatformNotificationManagementPage';
import * as apiModule from '@/services/api';

const configuration = {
  notificationsEnabled: true,
  smsEnabled: true,
  smsConfigured: true,
  emailEnabled: true,
  emailConfigured: false,
};

const getSpy = vi.spyOn(apiModule.api, 'get');
const postSpy = vi.spyOn(apiModule.api, 'post');

const renderPage = (configResp: unknown = configuration) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  getSpy.mockImplementation((url: string) => {
    if (String(url).includes('/platform/notifications/configuration')) {
      return Promise.resolve({ data: { success: true, data: configResp } } as any);
    }
    if (String(url).includes('/platform/notifications/managers')) {
      return Promise.resolve({ data: { success: true, data: [] } } as any);
    }
    if (String(url).includes('/platform/notifications/history')) {
      return Promise.resolve({
        data: { success: true, data: { content: [], pageNumber: 0, pageSize: 50, totalElements: 0, totalPages: 0, last: true } },
      } as any);
    }
    return Promise.resolve({ data: { success: true, data: null } } as any);
  });
  return {
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/platform/notifications']}>
          <Routes>
            <Route path="/platform/notifications" element={<PlatformNotificationManagementPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    ),
  };
};

beforeEach(() => {
  vi.resetAllMocks();
  vi.spyOn(window, 'confirm').mockImplementation(() => true);
});

describe('PlatformNotificationManagementPage', () => {
  test('renders DARSMS as configured and Gmail SMTP as not configured', async () => {
    renderPage();

    await waitFor(() => expect(screen.getByText('Notification Management')).toBeInTheDocument());
    await waitFor(() => expect(screen.getByText('DARSMS (SMS)')).toBeInTheDocument());
    await waitFor(() => expect(screen.getByText('Gmail SMTP (Email)')).toBeInTheDocument());

    await waitFor(() => {
      expect(screen.getAllByText('Configured')).toHaveLength(1);
    });
    await waitFor(() => {
      expect(screen.getAllByText('Not configured')).toHaveLength(1);
    });
  });

  test('disables test email and shows a hint when SMTP is not configured', async () => {
    renderPage(configuration);

    await waitFor(() => expect(screen.getByText('Gmail SMTP (Email)')).toBeInTheDocument());
    await waitFor(() => {
      expect(screen.getAllByText('Not configured')).toHaveLength(1);
    });

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'admin@example.com' } });
    const button = screen.getByRole('button', { name: 'Send test' });
    expect(button).toBeDisabled();
    expect(screen.getByText(/Test email is disabled/)).toBeInTheDocument();
    expect(postSpy).not.toHaveBeenCalled();
  });

  test('enables test email and reports SMTP configured when credentials are present', async () => {
    renderPage({ ...configuration, emailConfigured: true });

    await waitFor(() => expect(screen.getByText('Gmail SMTP (Email)')).toBeInTheDocument());
    await waitFor(() => {
      expect(screen.getAllByText('Configured')).toHaveLength(2);
    });

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'admin@example.com' } });
    const button = screen.getByRole('button', { name: 'Send test' });
    expect(button).not.toBeDisabled();

    fireEvent.click(button);
    await waitFor(() => expect(postSpy).toHaveBeenCalled());
    const [url, body] = postSpy.mock.calls[0];
    expect(String(url)).toContain('/platform/notifications/test-email');
    expect((body as any).recipient).toBe('admin@example.com');
    expect((body as any).confirmSend).toBe(true);
  });
});
