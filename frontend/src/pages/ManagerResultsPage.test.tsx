import { expect, test, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ManagerResultsPage } from "./ManagerResultsPage";
import {
  getResults,
  getResultsAnalytics,
  getExaminations,
  getAllExaminationSubjects,
} from "@/services/assessments";
import { getClasses, getStreamsByClass, getSubjects } from "@/services/academic";
import { getStudents } from "@/services/people";

const examination = {
  id: 1,
  name: "Mid Term",
  academicYearName: "2024",
  typeName: "Final",
  startDate: "2024-09-01",
};
const schoolClass = { id: 10, name: "Grade 10" };
const stream = { id: 20, name: "East", code: "E" };
const subject = { id: 100, name: "Mathematics" };
const examinationSubject = {
  id: 1000,
  examinationId: 1,
  subjectId: 100,
  classId: 10,
  streamId: 20,
  maximumMarks: 100,
};

const result = {
  id: 5,
  studentId: 42,
  studentName: "Jane Doe",
  admissionNumber: "ADM001",
  examinationId: 1,
  examinationName: "Mid Term",
  classId: 10,
  className: "Grade 10",
  streamId: 20,
  streamName: "East",
  subjectId: 100,
  subjectName: "Mathematics",
  marksObtained: 85,
  percentage: 85,
  grade: "A",
  gradePoint: 4,
  status: "SUBMITTED",
  submittedAt: "2024-09-15T10:00:00",
  teacherId: 7,
  teacherName: "John Smith",
  remarks: null,
  examinationSubjectId: 1000,
  schoolId: 1,
  uuid: "00000000-0000-0000-0000-000000000005",
  createdAt: "2024-09-15T10:00:00",
  updatedAt: "2024-09-15T10:00:00",
};

const analytics = {
  overallAverage: 85,
  totalStudentsAssessed: 1,
  passRate: 100,
  failRate: 0,
  highestScore: 85,
  lowestScore: 85,
  totalExaminations: 1,
  submittedResults: 1,
  approvedResults: 0,
  publishedResults: 0,
  totalResults: 1,
  passedResults: 1,
  failedResults: 0,
  draftResults: 0,
  gradeDistribution: [{ grade: "A", count: 1, percentage: 100 }],
  classPerformance: [{ classId: 10, className: "Grade 10", average: 85, studentCount: 1, passRate: 100, highestScore: 85, lowestScore: 85 }],
  streamPerformance: [],
  subjectPerformance: [{ subjectId: 100, subjectName: "Mathematics", average: 85, studentCount: 1, passRate: 100, highestScore: 85, lowestScore: 85 }],
  topStudents: [],
  studentsNeedingSupport: [],
  performanceTrends: [],
};

vi.mock("@/services/people", () => ({
  getStudents: vi.fn(),
}));

vi.mock("@/services/assessments", () => ({
  getResults: vi.fn(),
  getResultsAnalytics: vi.fn(),
  getExaminations: vi.fn(),
  getAllExaminationSubjects: vi.fn(),
}));

vi.mock("@/services/academic", () => ({
  getClasses: vi.fn(),
  getStreamsByClass: vi.fn(),
  getSubjects: vi.fn(),
}));

const mockGetResults = getResults as unknown as ReturnType<typeof vi.fn>;
const mockGetResultsAnalytics = getResultsAnalytics as unknown as ReturnType<typeof vi.fn>;
const mockGetExaminations = getExaminations as unknown as ReturnType<typeof vi.fn>;
const mockGetClasses = getClasses as unknown as ReturnType<typeof vi.fn>;
const mockGetStreamsByClass = getStreamsByClass as unknown as ReturnType<typeof vi.fn>;
const mockGetSubjects = getSubjects as unknown as ReturnType<typeof vi.fn>;
const mockGetAllExaminationSubjects = getAllExaminationSubjects as unknown as ReturnType<typeof vi.fn>;
const mockGetStudentsFn = getStudents as unknown as ReturnType<typeof vi.fn>;

function setupMocks() {
  vi.clearAllMocks();
  mockGetExaminations.mockResolvedValue({
    content: [examination],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 500,
  });
  mockGetClasses.mockResolvedValue({
    content: [schoolClass],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 500,
  });
  mockGetStreamsByClass.mockResolvedValue([stream]);
  mockGetSubjects.mockResolvedValue({
    content: [subject],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 500,
  });
  mockGetAllExaminationSubjects.mockResolvedValue([examinationSubject]);
  mockGetStudentsFn.mockResolvedValue({
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: 500,
  });
  mockGetResults.mockResolvedValue({
    content: [result],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 10,
  });
  mockGetResultsAnalytics.mockResolvedValue(analytics);
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ManagerResultsPage />
    </QueryClientProvider>,
  );
}

test("displays the student's full name, admission number, class, stream and subject", async () => {
  setupMocks();
  renderPage();
  await waitFor(() => expect(screen.getByText("Jane Doe")).toBeInTheDocument());
  expect(screen.getByText("ADM001")).toBeInTheDocument();
  expect(screen.getByText("Mid Term")).toBeInTheDocument();
  expect(screen.getByText("Grade 10")).toBeInTheDocument();
  expect(screen.getByText("East")).toBeInTheDocument();
  expect(screen.getByText("Mathematics")).toBeInTheDocument();
  expect(mockGetResults).toHaveBeenCalledWith({ page: 0, size: 10 });
});

test("loads filtered results when an examination is selected and Apply is clicked", async () => {
  setupMocks();
  renderPage();
  const examinationSelect = screen.getByLabelText(/examination/i);
  fireEvent.change(examinationSelect, { target: { value: String(examination.id) } });
  fireEvent.click(screen.getByText("Apply Filters"));
  await waitFor(() => {
    expect(mockGetResults).toHaveBeenLastCalledWith({
      page: 0,
      size: 10,
      examinationId: examination.id,
    });
  });
});

test("loads stream options for the selected class", async () => {
  setupMocks();
  renderPage();
  await waitFor(() => expect(screen.getByLabelText(/class/i)).toBeInTheDocument());
  const classSelect = screen.getByLabelText(/class/i);
  fireEvent.change(classSelect, { target: { value: String(schoolClass.id) } });
  await waitFor(() => {
    expect(mockGetStreamsByClass).toHaveBeenCalledWith(schoolClass.id);
  });
  expect(screen.getByRole("option", { name: /East/ })).toBeInTheDocument();
});

test("loads subject options for the selected examination and class", async () => {
  setupMocks();
  renderPage();
  const examinationSelect = screen.getByLabelText(/examination/i);
  fireEvent.change(examinationSelect, { target: { value: String(examination.id) } });
  const classSelect = screen.getByLabelText(/class/i);
  fireEvent.change(classSelect, { target: { value: String(schoolClass.id) } });
  fireEvent.click(screen.getByText("Apply Filters"));
  await waitFor(() => {
    expect(mockGetAllExaminationSubjects).toHaveBeenCalledWith(examination.id);
  });
  expect(screen.getByRole("option", { name: /Mathematics/ })).toBeInTheDocument();
});

test("shows analytics summary cards driven by the same filters", async () => {
  setupMocks();
  renderPage();
  await waitFor(() => expect(screen.getAllByText("1").length).toBeGreaterThan(0));
  expect(screen.getByText("Students assessed")).toBeInTheDocument();
  expect(screen.getByText("100%")).toBeInTheDocument();
  expect(mockGetResultsAnalytics).toHaveBeenCalledWith({
    examinationId: undefined,
    classId: undefined,
    streamId: undefined,
    subjectId: undefined,
  });
});

test("shows an empty state when no results match the filters", async () => {
  setupMocks();
  mockGetResults.mockResolvedValue({
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: 10,
  });
  renderPage();
  await waitFor(() => expect(screen.getByText("No results found")).toBeInTheDocument());
});

test("shows no results while results are still fetching", async () => {
  let resolveResults!: (value: any) => void;
  mockGetResults.mockImplementation(() =>
    new Promise((resolve) => {
      resolveResults = resolve;
    }),
  );
  setupMocks();
  renderPage();
  expect(screen.queryByText("Jane Doe")).not.toBeInTheDocument();
  resolveResults({
    content: [result],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 10,
  });
  await waitFor(() => expect(screen.getByText("Jane Doe")).toBeInTheDocument());
});
