import { expect, test, vi, beforeEach } from 'vitest';
import { render, screen, within, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ManagerExaminationsPage } from './ManagerExaminationsPage';
import {
  createExaminationSubject,
  getExaminations,
  getGradingSchemes,
  getExaminationSubjects,
} from '@/services/assessments';
import { getSubjects, getClasses, getStreamsByClass } from '@/services/academic';
import type { GradingScheme, GradingSchemeRange } from '@/services/assessments';

const mockCreateSubject = vi.mocked(createExaminationSubject);
const mockGetExaminations = vi.mocked(getExaminations);
const mockGetSubjects = vi.mocked(getSubjects);
const mockGetClasses = vi.mocked(getClasses);
const mockGetStreamsByClass = vi.mocked(getStreamsByClass);
const mockGetGradingSchemes = vi.mocked(getGradingSchemes);
const mockGetExaminationSubjects = vi.mocked(getExaminationSubjects);

const examination = {
  id: 1,
  academicYearId: 10,
  name: 'Midterm Exam',
  typeId: 1,
  startDate: null,
  endDate: null,
  status: 'SCHEDULED',
  description: null,
  createdAt: '2026-11-16T09:00:00Z',
  updatedAt: '2026-11-16T09:00:00Z',
  typeName: 'Internal',
  academicYearName: '2026',
};

const subject = { id: 5, name: 'Kiswahili', code: 'KISW' };
const clazz = { id: 3, name: 'Form 1', code: 'F1' };
const stream = { id: 7, name: 'A', code: 'F1A' };

const scheme: GradingScheme = {
  id: 6,
  name: 'internal exams',
  description: null,
  isDefault: false,
  status: 'ACTIVE',
  ranges: [
    { id: 1, gradingSchemeId: 6, grade: 'B', minMark: 0, maxMark: 74, gradePoint: null, remark: null, createdAt: '', updatedAt: '' },
    { id: 2, gradingSchemeId: 6, grade: 'A', minMark: 75, maxMark: 100, gradePoint: null, remark: null, createdAt: '', updatedAt: '' },
  ] as GradingSchemeRange[],
  createdAt: '2026-11-16T09:00:00Z',
  updatedAt: '2026-11-16T09:00:00Z',
};

vi.mock('@/services/assessments', async (importOriginal: () => Promise<unknown>) => {
  const actual = (await importOriginal()) as typeof import('@/services/assessments');
  return {
    ...actual,
    getExaminations: vi.fn(),
    getGradingSchemes: vi.fn(),
    getExaminationSubjects: vi.fn(),
    createExaminationSubject: vi.fn(),
  };
});

vi.mock('@/services/academic', async (importOriginal: () => Promise<unknown>) => {
  const actual = (await importOriginal()) as typeof import('@/services/academic');
  return {
    ...actual,
    getSubjects: vi.fn(),
    getClasses: vi.fn(),
    getStreamsByClass: vi.fn(),
  };
});

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return {
    ...render(
      <QueryClientProvider client={queryClient}>
        <ManagerExaminationsPage />
      </QueryClientProvider>,
    ),
  };
};

beforeEach(() => {
  vi.clearAllMocks();
  mockGetExaminations.mockResolvedValue({
    content: [examination],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 10,
  });
  mockGetSubjects.mockResolvedValue({ content: [subject], totalElements: 1, totalPages: 0, page: 0, size: 500 });
  mockGetClasses.mockResolvedValue({ content: [clazz], totalElements: 1, totalPages: 0, page: 0, size: 500 });
  mockGetStreamsByClass.mockResolvedValue([stream]);
  mockGetGradingSchemes.mockResolvedValue({ content: [scheme], totalElements: 1, totalPages: 0, page: 0, size: 500 });
  mockGetExaminationSubjects.mockResolvedValue({
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: 50,
  });
  mockCreateSubject.mockResolvedValue({ id: 100 } as any);
});

test('one subject-form submit produces exactly one create request', async () => {
  renderPage();

  await waitFor(() => screen.getByRole('button', { name: 'View Subjects' }));
  fireEvent.click(screen.getByRole('button', { name: 'View Subjects' }));

  const subjectsDialog = await screen.findByRole('dialog', { name: 'Examination Subjects' });
  fireEvent.click(within(subjectsDialog).getByRole('button', { name: 'Add Subject' }));

  const dialog = await screen.findByRole('dialog', { name: 'Add Examination Subject' });

  // Subject
  const subjectSelect = within(dialog).getByLabelText(/^Subject/) as HTMLSelectElement;
  fireEvent.change(subjectSelect, { target: { value: String(subject.id) } });

  // Class (enables and loads streams)
  const classSelect = within(dialog).getByLabelText(/^Class/) as HTMLSelectElement;
  fireEvent.change(classSelect, { target: { value: String(clazz.id) } });
  await waitFor(() => (within(dialog).getByLabelText(/^Stream/) as HTMLSelectElement).options.length > 1);

  // Stream
  const streamSelect = within(dialog).getByLabelText(/^Stream/) as HTMLSelectElement;
  fireEvent.change(streamSelect, { target: { value: String(stream.id) } });

  // Maximum marks and pass mark
  const marksInputs = within(dialog).getAllByRole('spinbutton');
  fireEvent.change(marksInputs[0], { target: { value: '100' } });
  fireEvent.change(marksInputs[1], { target: { value: '45' } });

  // Grading scheme (compatible with maximum marks 100)
  const schemeSelect = within(dialog).getByLabelText(/^Grading Scheme/) as HTMLSelectElement;
  fireEvent.change(schemeSelect, { target: { value: String(scheme.id) } });

  await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());

  fireEvent.click(within(dialog).getByRole('button', { name: 'Add Subject' }));

  await waitFor(() => expect(mockCreateSubject).toHaveBeenCalledTimes(1));
  expect(mockCreateSubject).toHaveBeenCalledWith(1, {
    subjectId: subject.id,
    classId: clazz.id,
    streamId: stream.id,
    maximumMarks: 100,
    passMark: 45,
    gradingSchemeId: scheme.id,
  });
});
