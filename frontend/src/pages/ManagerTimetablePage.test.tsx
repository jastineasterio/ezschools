import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ManagerTimetablePage } from './ManagerTimetablePage';
import * as apiModule from '@/services/api';

const getSpy = vi.spyOn(apiModule.api, 'get');
const postSpy = vi.spyOn(apiModule.api, 'post');

const assignment = {
  id: 1,
  teacherId: 6,
  teacherName: 'jastine john',
  subjectName: 'kiswahili',
  className: 'form 1',
  streamName: 'A',
  academicYearName: '2025/26',
};

const getForm = () => {
  const select = screen.getByRole('combobox', { name: 'Teacher assignment' });
  return select.closest('form')!;
};

const fillForm = (form: HTMLFormElement, assignmentId: string, start: string, end: string) => {
  const assignmentSelect = form.elements.namedItem('assignment') as HTMLSelectElement;
  fireEvent.change(assignmentSelect, { target: { value: assignmentId } });
  const startInput = form.elements.namedItem('startTime') as HTMLInputElement;
  const endInput = form.elements.namedItem('endTime') as HTMLInputElement;
  fireEvent.change(startInput, { target: { value: start } });
  fireEvent.change(endInput, { target: { value: end } });
};

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    ...render(
      <QueryClientProvider client={queryClient}>
        <ManagerTimetablePage />
      </QueryClientProvider>,
    ),
    invalidateQueries: vi.spyOn(queryClient, 'invalidateQueries'),
  };
};

beforeEach(() => {
  vi.resetAllMocks();
});

const setupPage = (assignmentsResp: unknown, timetableResp: unknown, createResp: unknown) => {
  getSpy
    .mockResolvedValueOnce({ data: { success: true, data: assignmentsResp } })
    .mockResolvedValue({ data: { success: true, data: timetableResp } });
  postSpy.mockResolvedValue({ data: { success: true, data: createResp } });
};

describe('ManagerTimetablePage', () => {
  test('renders teacher assignment label with correct separator encoding', async () => {
    setupPage({ content: [assignment] }, [], { id: 12 });

    renderPage();
    await waitFor(() => screen.getByRole('option', { name: /jastine john/ }));

    const option = screen.getByRole('option', { name: /jastine john/ });
    expect(option.textContent).toContain('·');
    expect(option.textContent).not.toContain('Â·');
  });

  test('successful lesson creation sends expected payload and shows success', async () => {
    setupPage({ content: [assignment] }, [], { id: 12, teacherAssignmentId: 1 });

    renderPage();
    await waitFor(() => screen.getByRole('button', { name: 'Add lesson' }));

    const form = getForm();
    fillForm(form, '1', '04:54', '05:22');

    fireEvent.submit(form);

    await waitFor(() => expect(postSpy).toHaveBeenCalledTimes(1));
    const [, body] = postSpy.mock.calls[0];
    expect(body).toMatchObject({ teacherAssignmentId: 1, dayOfWeek: 1, startTime: '04:54', endTime: '05:22' });
    await waitFor(() => screen.getByText('Lesson scheduled.'));
  });

  test('failed creation shows useful error and does not show success', async () => {
    postSpy.mockRejectedValue(new Error('Request failed (400): end time must be after start time'));
    getSpy
      .mockResolvedValueOnce({ data: { success: true, data: { content: [assignment] } } })
      .mockResolvedValue({ data: { success: true, data: [] } });

    renderPage();
    await waitFor(() => screen.getByRole('button', { name: 'Add lesson' }));

    const form = getForm();
    fillForm(form, '1', '04:54', '05:22');

    fireEvent.submit(form);

    await waitFor(() => screen.getByRole('alert'));
    expect(screen.getByRole('alert').textContent).toContain('Request failed');
    expect(screen.queryByText('Lesson scheduled.')).toBeNull();
  });

  test('timetable list refreshes after successful creation', async () => {
    setupPage({ content: [assignment] }, [], { id: 12 });

    const { invalidateQueries } = renderPage();
    await waitFor(() => screen.getByRole('button', { name: 'Add lesson' }));

    const form = getForm();
    fillForm(form, '1', '04:54', '05:22');

    fireEvent.submit(form);

    await waitFor(() => screen.getByText('Lesson scheduled.'));
    expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: ['school-timetable'] });
  });
});
