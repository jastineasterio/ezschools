import { expect, test, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ManagerResultsNotificationsPage } from './ManagerResultsNotificationsPage';
import {
  getResultNotificationExaminations,
  getResultNotificationClasses,
  getResultNotificationStreams,
  previewResultNotificationRecipients,
  sendResultNotification,
  getResultNotificationHistory,
  getResultNotificationDeliveries,
  getResultNotificationTemplate,
  saveResultNotificationTemplate,
  restoreResultNotificationTemplate,
} from '@/services/resultsNotifications';

const examination = {
  id: 10,
  name: 'Mid-Term Exam',
};
const schoolClass = { id: 20, name: 'Form 1' };
const stream = { id: 30, name: 'A' };

const preview = {
  examinationId: 10,
  examinationName: 'Mid-Term Exam',
  classId: 20,
  className: 'Form 1',
  streamId: 30,
  streamName: 'A',
  recipientType: 'BOTH',
  eligibleCount: 2,
  excludedCount: 1,
  students: [
    {
      studentId: 100,
      studentName: 'John Doe',
      admissionNumber: 'ADM001',
      classId: 20,
      className: 'Form 1',
      streamId: 30,
      streamName: 'A',
      subjectId: null,
      subjectName: null,
      examinationSubjectId: null,
      marksObtained: 78,
      maximumMarks: 100,
      grade: 'B',
      status: 'PUBLISHED',
      phone: '0712345678',
      eligible: true,
      exclusionReason: null,
    },
    {
      studentId: 101,
      studentName: 'Jane Smith',
      admissionNumber: 'ADM002',
      classId: 20,
      className: 'Form 1',
      streamId: 30,
      streamName: 'A',
      subjectId: null,
      subjectName: null,
      examinationSubjectId: null,
      marksObtained: 65,
      maximumMarks: 100,
      grade: 'C',
      status: 'PUBLISHED',
      phone: null,
      eligible: false,
      exclusionReason: 'No phone number on file',
    },
  ],
  parents: [
    {
      parentId: 200,
      parentName: 'Jane Doe (Parent)',
      phone: '0787654321',
      studentId: 100,
      studentName: 'John Doe',
      admissionNumber: 'ADM001',
      relationshipType: 'FATHER',
      eligible: true,
      exclusionReason: null,
    },
    {
      parentId: 201,
      parentName: 'No Phone Parent',
      phone: null,
      studentId: 101,
      studentName: 'Jane Smith',
      admissionNumber: 'ADM002',
      relationshipType: 'MOTHER',
      eligible: false,
      exclusionReason: 'No phone number on file',
    },
  ],
  sampleMessage: 'Dear Parent, John Doe (Form 1/A) results for Mid-Term Exam: Maths 78/100 (B). - MJ School',
  canSend: true,
  reason: null,
};

vi.mock('@/services/resultsNotifications', () => ({
  getResultNotificationExaminations: vi.fn(),
  getResultNotificationClasses: vi.fn(),
  getResultNotificationStreams: vi.fn(),
  previewResultNotificationRecipients: vi.fn(),
  sendResultNotification: vi.fn(),
  getResultNotificationHistory: vi.fn(),
  getResultNotificationDeliveries: vi.fn(),
  getResultNotificationTemplate: vi.fn(),
  saveResultNotificationTemplate: vi.fn(),
  restoreResultNotificationTemplate: vi.fn(),
}));

const mockGetExaminations = getResultNotificationExaminations as unknown as ReturnType<typeof vi.fn>;
const mockGetClasses = getResultNotificationClasses as unknown as ReturnType<typeof vi.fn>;
const mockGetStreams = getResultNotificationStreams as unknown as ReturnType<typeof vi.fn>;
const mockPreviewResultNotificationRecipients = previewResultNotificationRecipients as unknown as ReturnType<typeof vi.fn>;
const mockSendResultNotification = sendResultNotification as unknown as ReturnType<typeof vi.fn>;
const mockGetResultNotificationHistory = getResultNotificationHistory as unknown as ReturnType<typeof vi.fn>;
const mockGetResultNotificationDeliveries = getResultNotificationDeliveries as unknown as ReturnType<typeof vi.fn>;
const mockGetResultNotificationTemplate = getResultNotificationTemplate as unknown as ReturnType<typeof vi.fn>;
const mockSaveResultNotificationTemplate = saveResultNotificationTemplate as unknown as ReturnType<typeof vi.fn>;
const mockRestoreResultNotificationTemplate = restoreResultNotificationTemplate as unknown as ReturnType<typeof vi.fn>;

function setupMocks() {
  vi.clearAllMocks();
  mockGetExaminations.mockResolvedValue([examination]);
  mockGetClasses.mockResolvedValue([schoolClass]);
  mockGetStreams.mockResolvedValue([stream]);
  mockPreviewResultNotificationRecipients.mockResolvedValue(preview);
  mockSendResultNotification.mockResolvedValue({
    id: 500,
    uuid: 'uuid-500',
    schoolId: 1,
    examinationId: 10,
    classId: 20,
    streamId: 30,
    recipientType: 'BOTH',
    idempotencyKey: 'key-500',
    title: 'Results Notification - Mid-Term Exam - Form 1 A',
    totalRecipients: 3,
    excludedCount: 1,
    successfulCount: 2,
    failedCount: 0,
    pendingCount: 0,
    status: 'SENT',
    initiatedBy: 10,
    createdAt: '2024-09-15T10:00:00',
  });
  mockGetResultNotificationHistory.mockResolvedValue({
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: 20,
  });
  mockGetResultNotificationDeliveries.mockResolvedValue([]);
  mockGetResultNotificationTemplate.mockResolvedValue({
    schoolId: 1,
    smsTemplate: 'Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}',
    isDefault: true,
    updatedAt: null,
    supportedPlaceholders: [],
  });
  mockSaveResultNotificationTemplate.mockResolvedValue({
    schoolId: 1,
    smsTemplate: 'Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}',
    isDefault: true,
    updatedAt: null,
    supportedPlaceholders: [],
  });
  mockRestoreResultNotificationTemplate.mockResolvedValue({
    schoolId: 1,
    smsTemplate: 'Dear {recipient_name}, {student_name} ({class_name}/{stream_name}) results for {examination_name}: {subject_results}. - {school_name}',
    isDefault: true,
    updatedAt: null,
    supportedPlaceholders: [],
  });
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ManagerResultsNotificationsPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  setupMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});

async function selectExaminationClassStream() {
  const examinationSelect = screen.getByLabelText(/^examination$/i);
  fireEvent.change(examinationSelect, { target: { value: String(examination.id) } });
  await waitFor(() =>
    expect(screen.getByRole('option', { name: /Mid-Term Exam/ })).toBeInTheDocument()
  );
  await waitFor(() =>
    expect(screen.getByRole('option', { name: /Form 1/ })).toBeInTheDocument()
  );
  const classSelect = screen.getByLabelText(/^class$/i);
  fireEvent.change(classSelect, { target: { value: String(schoolClass.id) } });
  await waitFor(() =>
    expect(screen.getByRole('option', { name: /A/ })).toBeInTheDocument()
  );
  const streamSelect = screen.getByLabelText(/^stream$/i);
  fireEvent.change(streamSelect, { target: { value: String(stream.id) } });
}

test('renders the Results Notifications page title', async () => {
  renderPage();
  await waitFor(() => expect(screen.getByText('Results Notifications')).toBeInTheDocument());
  expect(screen.getByText('Send examination results to students and parents through personalized bulk SMS.')).toBeInTheDocument();
});

test('displays examination, class, stream, and recipient selection controls', async () => {
  renderPage();
  await waitFor(() => expect(screen.getByLabelText(/^examination$/i)).toBeInTheDocument());
  expect(screen.getByLabelText(/^class$/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/^stream$/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/^recipients$/i)).toBeInTheDocument();
});

test('loads examinations belonging to the current school', async () => {
  renderPage();
  await waitFor(() => expect(mockGetExaminations).toHaveBeenCalled());
  await waitFor(() => expect(screen.getByRole('option', { name: /Mid-Term Exam/ })).toBeInTheDocument());
});

test('loads classes belonging to the current school', async () => {
  renderPage();
  await waitFor(() => expect(mockGetClasses).toHaveBeenCalled());
  await waitFor(() => expect(screen.getByRole('option', { name: /Form 1/ })).toBeInTheDocument());
});

test('loads streams only for the selected class', async () => {
  renderPage();
  await waitFor(() =>
    expect(screen.getByRole('option', { name: /Mid-Term Exam/ })).toBeInTheDocument()
  );
  const examinationSelect = screen.getByLabelText(/^examination$/i);
  fireEvent.change(examinationSelect, { target: { value: String(examination.id) } });
  await waitFor(() =>
    expect(screen.getByRole('option', { name: /Form 1/ })).toBeInTheDocument()
  );
  const classSelect = screen.getByLabelText(/^class$/i);
  fireEvent.change(classSelect, { target: { value: String(schoolClass.id) } });
  await waitFor(() => {
    expect(mockGetStreams).toHaveBeenCalledWith(schoolClass.id);
  });
  await waitFor(() => expect(screen.getByRole('option', { name: /A/ })).toBeInTheDocument());
});

test('resets stream selection when class changes', async () => {
  renderPage();
  await waitFor(() =>
    expect(screen.getByRole('option', { name: /Mid-Term Exam/ })).toBeInTheDocument()
  );
  const examinationSelect = screen.getByLabelText(/^examination$/i);
  fireEvent.change(examinationSelect, { target: { value: String(examination.id) } });
  await waitFor(() =>
    expect(screen.getByRole('option', { name: /Form 1/ })).toBeInTheDocument()
  );
  const classSelect = screen.getByLabelText(/^class$/i);
  fireEvent.change(classSelect, { target: { value: String(schoolClass.id) } });
  await waitFor(() => {
    expect(mockGetStreams).toHaveBeenCalledWith(schoolClass.id);
  });
  const streamSelect = screen.getByLabelText(/^stream$/i);
  fireEvent.change(streamSelect, { target: { value: String(stream.id) } });
  expect(screen.getByRole('option', { name: /A/ })).toBeInTheDocument();

  fireEvent.change(classSelect, { target: { value: String(99) } });
  await waitFor(() => {
    expect(screen.getByLabelText(/^stream$/i)).toBeInTheDocument();
  });
});

test('displays results preview table with student information', async () => {
  renderPage();
  await selectExaminationClassStream();

  await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
  expect(screen.getByText('ADM001')).toBeInTheDocument();
  expect(screen.getByText('Form 1')).toBeInTheDocument();
  expect(screen.getByText('A')).toBeInTheDocument();
});

test('shows recipient count before sending', async () => {
  renderPage();
  await selectExaminationClassStream();

  await waitFor(() => expect(screen.getByText('2 eligible')).toBeInTheDocument());
  expect(screen.getByText('1 excluded')).toBeInTheDocument();
});

test('shows sample personalized SMS message', async () => {
  renderPage();
  await selectExaminationClassStream();

  await waitFor(() => expect(screen.getByText(/Dear Parent/)).toBeInTheDocument());
  expect(screen.getByText(/John Doe/)).toBeInTheDocument();
});

test('displays exclusion reason for missing phone numbers', async () => {
  renderPage();
  await selectExaminationClassStream();

  await waitFor(() => expect(screen.getByText('Jane Smith')).toBeInTheDocument());
  expect(screen.getByText('No phone number on file')).toBeInTheDocument();
});

test('opens confirmation dialog when review and send is clicked', async () => {
  renderPage();
  await selectExaminationClassStream();

  await waitFor(() => expect(screen.getByText('2 eligible')).toBeInTheDocument());
  fireEvent.click(screen.getByText('Review and send'));
  await waitFor(() => expect(screen.getByText('Confirm Results Notification')).toBeInTheDocument());
  expect(screen.getByText('Mid-Term Exam')).toBeInTheDocument();
  expect(screen.getByText('Form 1')).toBeInTheDocument();
  expect(screen.getByText('A')).toBeInTheDocument();
  expect(screen.getByText('2')).toBeInTheDocument();
});

test('sends notification when confirmed and shows success message', async () => {
  renderPage();
  await selectExaminationClassStream();

  await waitFor(() => expect(screen.getByText('2 eligible')).toBeInTheDocument());
  fireEvent.click(screen.getByText('Review and send'));
  await waitFor(() => expect(screen.getByText('Confirm Results Notification')).toBeInTheDocument());
  fireEvent.click(screen.getByText('Confirm and send'));
  await waitFor(() => expect(mockSendResultNotification).toHaveBeenCalled());
  await waitFor(() => expect(screen.getByText('Results notification queued for sending.')).toBeInTheDocument());
});

test('disables send button while sending is processing', async () => {
  // eslint-disable-next-line no-unused-vars
  let resolveSend!: (value: unknown) => void;
  mockSendResultNotification.mockImplementation(() => new Promise((resolve) => { resolveSend = resolve; }));

  renderPage();
  await selectExaminationClassStream();

  await waitFor(() => expect(screen.getByText('2 eligible')).toBeInTheDocument());
  fireEvent.click(screen.getByText('Review and send'));
  await waitFor(() => expect(screen.getByText('Confirm Results Notification')).toBeInTheDocument());

  const sendButton = screen.getByText('Confirm and send');
  fireEvent.click(sendButton);
  expect(sendButton).toBeDisabled();

  resolveSend(undefined);
  await waitFor(() => expect(sendButton).not.toBeDisabled());
});

test('shows notification history section', async () => {
  mockGetResultNotificationHistory.mockResolvedValue({
    content: [
      {
        id: 1,
        uuid: 'uuid-1',
        schoolId: 1,
        examinationId: 10,
        classId: 20,
        streamId: 30,
        recipientType: 'BOTH',
        idempotencyKey: 'key-1',
        title: 'Results Notification - Mid-Term Exam - Form 1 A',
        totalRecipients: 3,
        excludedCount: 1,
        successfulCount: 2,
        failedCount: 0,
        pendingCount: 0,
        status: 'SENT',
        initiatedBy: 10,
        createdAt: '2024-09-15T10:00:00',
      },
    ],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 20,
  });

  renderPage();
  await waitFor(() => expect(screen.getByText('Notification History')).toBeInTheDocument());
  await waitFor(() => expect(screen.getByText('Results Notification - Mid-Term Exam - Form 1 A')).toBeInTheDocument());
});

test('shows delivery details when expanded', async () => {
  mockGetResultNotificationHistory.mockResolvedValue({
    content: [
      {
        id: 1,
        uuid: 'uuid-1',
        schoolId: 1,
        examinationId: 10,
        classId: 20,
        streamId: 30,
        recipientType: 'BOTH',
        idempotencyKey: 'key-1',
        title: 'Results Notification - Mid-Term Exam - Form 1 A',
        totalRecipients: 3,
        excludedCount: 1,
        successfulCount: 2,
        failedCount: 0,
        pendingCount: 0,
        status: 'SENT',
        initiatedBy: 10,
        createdAt: '2024-09-15T10:00:00',
      },
    ],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 20,
  });
  mockGetResultNotificationDeliveries.mockResolvedValue([
    {
      id: 1,
      notificationId: 1,
      smsMessageId: 100,
      recipientType: 'STUDENT',
      recipientId: 100,
      recipientName: 'John Doe',
      recipientPhone: '255712345678',
      studentId: 100,
      studentName: 'John Doe',
      admissionNumber: 'ADM001',
      channel: 'SMS',
      status: 'SENT',
      providerMessageId: 'ref-123',
      batchId: 'batch-1',
      failureCode: null,
      errorMessage: null,
      deliveryStatus: null,
      attemptCount: 1,
      lastAttemptAt: '2024-09-15T10:00:00',
      sentAt: '2024-09-15T10:00:00',
      deliveredAt: null,
      failedAt: null,
    },
  ]);

  renderPage();
  await waitFor(() => expect(screen.getByText('Notification History')).toBeInTheDocument());
  await waitFor(() =>
    expect(screen.getByText('Results Notification - Mid-Term Exam - Form 1 A')).toBeInTheDocument()
  );
  fireEvent.click(screen.getByText('Delivery details'));
  await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
  await waitFor(() => expect(mockGetResultNotificationDeliveries).toHaveBeenCalledWith(1));
});

test('does not crash when examinations API fails', async () => {
  mockGetExaminations.mockRejectedValue(new Error('Examinations service unavailable'));

  renderPage();

  // Core regression: page must render its layout even when an API request fails
  await waitFor(() => expect(screen.getByText('Results Notifications')).toBeInTheDocument());
  expect(screen.getByLabelText(/^class$/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/^stream$/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/^recipients$/i)).toBeInTheDocument();
  // An error alert should be visible for the failed query
  await waitFor(() => {
    const alerts = screen.getAllByRole('alert');
    expect(alerts.some((a) => a.textContent?.includes('Failed to load examinations'))).toBe(true);
  });
});

test('does not crash when classes API fails', async () => {
  mockGetClasses.mockRejectedValue(new Error('Classes service unavailable'));

  renderPage();

  await waitFor(() => expect(screen.getByText('Results Notifications')).toBeInTheDocument());
  expect(screen.getByLabelText(/^examination$/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/^stream$/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/^recipients$/i)).toBeInTheDocument();
  await waitFor(() => {
    const alerts = screen.getAllByRole('alert');
    expect(alerts.some((a) => a.textContent?.includes('Failed to load classes'))).toBe(true);
  });
});

test('does not crash when preview returns null students and parents', async () => {
  mockPreviewResultNotificationRecipients.mockResolvedValue({
    ...preview,
    students: null,
    parents: null,
  });

  renderPage();
  await selectExaminationClassStream();

  // Core regression: page must render without crashing when preview data has null arrays
  await waitFor(() => expect(screen.getByText('Results Notifications')).toBeInTheDocument());
  expect(screen.queryByText('John Doe')).not.toBeInTheDocument();
});

test('shows empty state when no examinations are available', async () => {
  mockGetExaminations.mockResolvedValue([]);

  renderPage();

  await waitFor(() =>
    expect(screen.getByText('No examinations found')).toBeInTheDocument()
  );
});

test('renders the SMS Template section', async () => {
  renderPage();
  await waitFor(() => expect(screen.getByTestId('result-notification-template')).toBeInTheDocument());
  await waitFor(() => expect(screen.getByRole('button', { name: /Restore default/i })).toBeInTheDocument());
  await waitFor(() => expect(screen.getByRole('button', { name: /Save Template/i })).toBeInTheDocument());
});

test('updates live sample and character count when template is edited', async () => {
  renderPage();
  await waitFor(() =>
    expect(screen.getByTestId('result-notification-template')).not.toBeDisabled()
  );
  const textarea = screen.getByTestId('result-notification-template');
  fireEvent.input(textarea, { target: { value: 'Hello {student_name}, your results are ready. - {school_name}' } });

  await waitFor(() =>
    expect(screen.getByText(/Hello John Doe, your results are ready\. - MJ School/)).toBeInTheDocument()
  );
  await waitFor(() => expect(screen.getByText(/61 characters/)).toBeInTheDocument());
});

test('saves the customized template via API', async () => {
  mockSaveResultNotificationTemplate.mockResolvedValue({
    schoolId: 1,
    smsTemplate: 'Hello {student_name}. - {school_name}',
    isDefault: false,
    updatedAt: null,
    supportedPlaceholders: [],
  });
  renderPage();
  await waitFor(() =>
    expect(screen.getByTestId('result-notification-template')).not.toBeDisabled()
  );
  const textarea = screen.getByTestId('result-notification-template') as HTMLTextAreaElement;
  textarea.value = 'Hello {student_name}. - {school_name}';
  fireEvent.input(textarea);
  expect(textarea).toHaveValue('Hello {student_name}. - {school_name}');
  fireEvent.click(screen.getByRole('button', { name: /Save Template/i }));
  await waitFor(() =>
    expect(mockSaveResultNotificationTemplate).toHaveBeenCalled()
  );
  const lastSmsTemplate = mockSaveResultNotificationTemplate.mock.lastCall?.[0]?.smsTemplate;
  expect(lastSmsTemplate).toContain('{student_name}');
});

test('restores the default template via API', async () => {
  renderPage();
  fireEvent.click(screen.getByRole('button', { name: /Restore default/i }));
  await waitFor(() =>
    expect(mockRestoreResultNotificationTemplate).toHaveBeenCalled()
  );
});
