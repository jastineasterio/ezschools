import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';

import { PlatformBrandingPage } from './PlatformBrandingPage';
import * as apiModule from '@/services/api';

const schoolId = 7;

const school = {
  id: schoolId,
  uuid: '11111111-1111-1111-1111-111111111111',
  name: 'Greenwood High',
  registrationNumber: 'REG-001',
  email: 'info@greenwood.edu',
  phone: null,
  address: null,
  region: null,
  district: null,
  isActive: true,
  isSuspended: false,
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
};

const branding = {
  id: 1,
  schoolId,
  primaryColor: '#10b981',
  secondaryColor: '#64748b',
  accentColor: '#ea580c',
  backgroundColor: '#ffffff',
  textColor: '#0f172a',
  logoUrl: null,
  faviconUrl: null,
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
};

const getSpy = vi.spyOn(apiModule.api, 'get');
const putSpy = vi.spyOn(apiModule.api, 'put');
const postSpy = vi.spyOn(apiModule.api, 'post');
const deleteSpy = vi.spyOn(apiModule.api, 'delete');

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/platform/schools/7/branding']}>
          <Routes>
            <Route path="/platform/schools/:id/branding" element={<PlatformBrandingPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    ),
  };
};

beforeEach(() => {
  vi.resetAllMocks();
  vi.spyOn(window, 'URL').mockImplementation({
    createObjectURL: () => 'blob:mock-preview',
    revokeObjectURL: vi.fn(),
  } as unknown as typeof window.URL);
});

const setupPage = (schoolResp: unknown, brandingResp: unknown) => {
  getSpy
    .mockResolvedValueOnce({ data: { success: true, data: schoolResp } } as any)
    .mockResolvedValue({ data: { success: true, data: brandingResp } } as any);
};

describe('PlatformBrandingPage', () => {
  test('renders school name and branding actions', async () => {
    setupPage(school, branding);

    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText('Greenwood High').length).toBeGreaterThan(0);
    });
    expect(screen.getByText('Branding Management')).toBeInTheDocument();
    expect(screen.getByText('Save Changes')).toBeInTheDocument();
    expect(screen.getByText('Restore Default')).toBeInTheDocument();
  });

  test('submits updated branding colors via PUT', async () => {
    setupPage(school, branding);

    renderPage();

    await waitFor(() => screen.getByText('Save Changes'));

    const inputs = document.getElementsByTagName('input');
    // inputs[0] is the file input; [1] is the primary color picker; [2] is the primary text input.
    const primaryText = inputs[2] as HTMLInputElement;
    fireEvent.change(primaryText, { target: { value: '#ff0000' } });

    const form = document.querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => expect(putSpy).toHaveBeenCalled());
    const [url, body] = putSpy.mock.calls[0];
    expect(String(url)).toContain(`/api/platform/schools/${schoolId}/branding`);
    expect((body as any).primaryColor).toBe('#ff0000');

    await waitFor(() => screen.getByText('Branding saved successfully.'));
  });

  test('restores default branding via POST reset', async () => {
    setupPage(school, branding);

    renderPage();

    await waitFor(() => screen.getByText('Restore Default'));

    vi.spyOn(window, 'confirm').mockImplementation(() => true);

    const resetButton = screen.getByText('Restore Default').closest('button')!;
    fireEvent.click(resetButton);

    await waitFor(() => expect(postSpy).toHaveBeenCalled());
    const [url] = postSpy.mock.calls[0];
    expect(String(url)).toContain(`/api/platform/schools/${schoolId}/branding/reset`);
  });

  test('removes logo via DELETE when a logo exists', async () => {
    setupPage(school, { ...branding, logoUrl: 'http://localhost/uploads/logos/abc.png' });

    renderPage();

    await waitFor(() => screen.getByText('Remove'));

    const removeButton = screen.getByText('Remove').closest('button')!;
    fireEvent.click(removeButton);

    await waitFor(() => expect(deleteSpy).toHaveBeenCalled());
    const [url] = deleteSpy.mock.calls[0];
    expect(String(url)).toContain(`/api/platform/schools/${schoolId}/branding/logo`);
  });

  test('uploads logo via POST when a file is selected', async () => {
    setupPage(school, branding);

    renderPage();

    await waitFor(() => screen.getByText('Save Changes'));

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['hello'], 'logo.png', { type: 'image/png' });
    Object.defineProperty(fileInput, 'files', { value: [file] });
    fireEvent.change(fileInput);

    await waitFor(() => expect(postSpy).toHaveBeenCalled());
    const [url, body] = postSpy.mock.calls[0];
    expect(String(url)).toContain(`/api/platform/schools/${schoolId}/branding/logo`);
    expect(body).toBeInstanceOf(FormData);
  });
});
