import { describe, expect, test, vi, beforeEach } from 'vitest';
import { render, screen, within, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ManagerGradingSchemesPage } from './ManagerGradingSchemesPage';
import {
  createGradingScheme,
  addGradingSchemeRange,
  getGradingSchemes,
  getGradingScheme,
  type AddGradingSchemeRangeRequest,
  type GradingScheme,
  type GradingSchemeRange,
} from '@/services/assessments';

const mockCreate = vi.mocked(createGradingScheme);
const mockAddRange = vi.mocked(addGradingSchemeRange);
const mockList = vi.mocked(getGradingSchemes);
const mockGet = vi.mocked(getGradingScheme);

const createdScheme: GradingScheme = {
  id: 42,
  name: 'internal exams',
  description: null,
  isDefault: false,
  status: 'ACTIVE',
  ranges: [],
  createdAt: '2026-11-16T09:00:00Z',
  updatedAt: '2026-11-16T09:00:00Z',
};

const createdRange: GradingSchemeRange = {
  id: 1,
  gradingSchemeId: 42,
  grade: 'B',
  minMark: 0,
  maxMark: 74,
  gradePoint: 4,
  remark: 'Good',
  createdAt: '2026-11-16T09:00:00Z',
  updatedAt: '2026-11-16T09:00:00Z',
};

vi.mock('@/services/assessments', async (importOriginal: () => Promise<unknown>) => {
  const actual = (await importOriginal()) as typeof import('@/services/assessments');
  return {
    ...actual,
    getGradingSchemes: vi.fn(),
    getGradingScheme: vi.fn(),
    createGradingScheme: vi.fn(),
    addGradingSchemeRange: vi.fn(),
    updateGradingScheme: vi.fn(),
    updateGradingSchemeRange: vi.fn(),
    deleteGradingSchemeRange: vi.fn(),
    toggleGradingSchemeStatus: vi.fn(),
    deleteGradingScheme: vi.fn(),
  };
});

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    ...render(
      <QueryClientProvider client={queryClient}>
        <ManagerGradingSchemesPage />
      </QueryClientProvider>,
    ),
  };
};

beforeEach(() => {
  vi.clearAllMocks();
  mockList.mockResolvedValue({
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: 20,
  });
});

function fillRow(container: HTMLElement, index: number, values: {
  grade: string;
  minMark: string;
  maxMark: string;
  gradePoint: string;
  remark: string;
}) {
  const gradeInputs = within(container).getAllByPlaceholderText('A');
  const minInputs = within(container).getAllByPlaceholderText('0');
  const maxInputs = within(container).getAllByPlaceholderText('50');
  const pointInputs = within(container).getAllByPlaceholderText('4.0');
  const remarkInputs = within(container).getAllByPlaceholderText('Excellent');
  fireEvent.input(gradeInputs[index], { target: { value: values.grade } });
  fireEvent.input(minInputs[index], { target: { value: values.minMark } });
  fireEvent.input(maxInputs[index], { target: { value: values.maxMark } });
  fireEvent.input(pointInputs[index], { target: { value: values.gradePoint } });
  fireEvent.input(remarkInputs[index], { target: { value: values.remark } });
}

describe('ManagerGradingSchemesPage create scheme flow', () => {
  test('creates the scheme once and saves every range with the returned scheme ID, never /null/', async () => {
    mockCreate.mockResolvedValue(createdScheme);
    mockAddRange.mockResolvedValue(createdRange);

    renderPage();
    await waitFor(() => screen.getByRole('button', { name: 'Create Scheme' }));
    fireEvent.click(screen.getByRole('button', { name: 'Create Scheme' }));
    const dialog = await screen.findByRole('dialog');

    fireEvent.input(within(dialog).getByPlaceholderText('e.g. O Level Grading'), {
      target: { value: 'internal exams' },
    });

    // Grade A: 75-100, 5 points, Excellent
    fillRow(dialog, 0, { grade: 'A', minMark: '75', maxMark: '100', gradePoint: '5', remark: 'Excellent' });
    // Grade B: 0-74, 4 points, Good (sorted by minMark this is sent first)
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add Grade' }));
    fillRow(dialog, 1, { grade: 'B', minMark: '0', maxMark: '74', gradePoint: '4', remark: 'Good' });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Scheme' }));

    await waitFor(() => expect(mockCreate).toHaveBeenCalledTimes(1));
    expect(mockCreate).toHaveBeenCalledWith({
      name: 'internal exams',
      description: undefined,
      isDefault: false,
      status: 'ACTIVE',
    });

    await waitFor(() => expect(mockAddRange).toHaveBeenCalledTimes(2));
    const schemeId = (await mockCreate.mock.results[0].value).id;
    for (const call of mockAddRange.mock.calls) {
      expect(call[0]).toBe(schemeId);
    }
    // No null/undefined scheme ID can ever produce a URL containing "/null/".
    expect(mockAddRange.mock.calls.some((call: [number, AddGradingSchemeRangeRequest]) => call[0] == null)).toBe(false);
    expect(mockAddRange.mock.calls[0][1]).toMatchObject({
      grade: 'B',
      minMark: 0,
      maxMark: 74,
      gradePoint: 4,
      remark: 'Good',
    });
    expect(mockAddRange.mock.calls[1][1]).toMatchObject({
      grade: 'A',
      minMark: 75,
      maxMark: 100,
      gradePoint: 5,
      remark: 'Excellent',
    });
  });

  test('retry after partial range creation retries only remaining ranges without another scheme', async () => {
    mockCreate.mockResolvedValue(createdScheme);
    mockAddRange
      .mockResolvedValueOnce(createdRange)
      .mockRejectedValueOnce(new Error('Request failed (500): Internal server error'))
      .mockResolvedValueOnce(createdRange);

    renderPage();
    await waitFor(() => screen.getByRole('button', { name: 'Create Scheme' }));
    fireEvent.click(screen.getByRole('button', { name: 'Create Scheme' }));
    const dialog = await screen.findByRole('dialog');

    fireEvent.input(within(dialog).getByPlaceholderText('e.g. O Level Grading'), {
      target: { value: 'internal exams' },
    });
    fillRow(dialog, 0, { grade: 'A', minMark: '75', maxMark: '100', gradePoint: '5', remark: 'Excellent' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add Grade' }));
    fillRow(dialog, 1, { grade: 'B', minMark: '0', maxMark: '74', gradePoint: '4', remark: 'Good' });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Scheme' }));

    await waitFor(() => expect(mockCreate).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockAddRange).toHaveBeenCalledTimes(2));
    await waitFor(() => screen.getByRole('alert'));

    expect(screen.getByRole('alert')).toHaveTextContent('Request failed (500): Internal server error');
    // The scheme was created; only the remaining range failed. No second scheme is created on retry.
    expect(mockCreate).toHaveBeenCalledTimes(1);

    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Retry remaining grade ranges' }));

    await waitFor(() => expect(mockAddRange).toHaveBeenCalledTimes(3));
    expect(mockCreate).toHaveBeenCalledTimes(1);
    for (const call of mockAddRange.mock.calls) {
      expect(call[0]).toBe(42);
    }
    expect(mockAddRange.mock.calls.some((call: [number, AddGradingSchemeRangeRequest]) => call[0] == null)).toBe(false);
    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
  });

  test('missing scheme ID stops the flow with an error and sends no range request', async () => {
    mockCreate.mockRejectedValue(new Error('Scheme creation failed'));

    renderPage();
    await waitFor(() => screen.getByRole('button', { name: 'Create Scheme' }));
    fireEvent.click(screen.getByRole('button', { name: 'Create Scheme' }));
    const dialog = await screen.findByRole('dialog');

    fireEvent.input(within(dialog).getByPlaceholderText('e.g. O Level Grading'), {
      target: { value: 'internal exams' },
    });
    fillRow(dialog, 0, { grade: 'A', minMark: '0', maxMark: '100', gradePoint: '5', remark: 'Excellent' });

    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Scheme' }));

    await waitFor(() => screen.getByRole('alert'));
    expect(screen.getByRole('alert')).toHaveTextContent('Scheme creation failed');
    expect(mockCreate).toHaveBeenCalledTimes(1);
    expect(mockAddRange).not.toHaveBeenCalled();
    mockGet.mockClear();
  });
});
