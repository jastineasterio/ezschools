import { useEffect, useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getResults,
  getResultsAnalytics,
  createResult,
  updateResult,
  submitResult,
  approveResult,
  rejectResult,
  publishResult,
  getExaminations,
  getAllExaminationSubjects,
} from "@/services/assessments";
import { getClasses, getStreamsByClass, getSubjects, type Class, type Stream } from "@/services/academic";
import { getStudents } from "@/services/people";
import type { Student } from "@/services/people";
import type { ResultResponse, CreateResultRequest } from "@/types/academic";
import { getApiErrorMessage } from "@/services/api";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Dialog, DialogContent } from "@/components/ui/Dialog";
import { Badge } from "@/components/ui/Badge";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/Table";
import { Skeleton } from "@/components/ui/Skeleton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/Alert";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { Separator } from "@/components/ui/Separator";
import { Plus, RefreshCw, Edit, CheckCircle, XCircle, Send, Eye } from "lucide-react";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

const resultSchema = z.object({
  studentId: z.number().min(1, "Student is required"),
  examinationSubjectId: z.number().min(1, "Examination subject is required"),
  marksObtained: z.coerce.number().optional(),
  remarks: z.string().optional(),
});

type ResultFormValues = z.infer<typeof resultSchema>;

type FilterOption = { value: string; label: string };

const formatMarks = (value: number | null) => (value == null ? "-" : String(value));
const formatAverage = (value: number | null) => (value == null ? "-" : value.toFixed(1));
const formatPercent = (value: number) => value.toFixed(1);

function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-center">
      <p className="text-sm text-muted-foreground">{title}</p>
      <p className="mt-1 text-sm text-muted-foreground">{description}</p>
    </div>
  );
}

export function ManagerResultsPage() {
  const [page, setPage] = useState(0);
  const size = 10;
  const [examinationId, setExaminationId] = useState<string>("");
  const [classId, setClassId] = useState<string>("");
  const [streamId, setStreamId] = useState<string>("");
  const [subjectId, setSubjectId] = useState<string>("");
  const [applyVersion, setApplyVersion] = useState(0);
  const [open, setOpen] = useState(false);
  const [editResult, setEditResult] = useState<ResultResponse | null>(null);
  const [selectedExaminationId, setSelectedExaminationId] = useState<string>("");
  const queryClient = useQueryClient();

  const parsedExaminationId = examinationId ? Number(examinationId) : undefined;
  const parsedClassId = classId ? Number(classId) : undefined;
  const parsedStreamId = streamId ? Number(streamId) : undefined;
  const parsedSubjectId = subjectId ? Number(subjectId) : undefined;

  const {
    data,
    isLoading: resultsLoading,
    isError: resultsError,
    error: resultsErrorObj,
    refetch: refetchResults,
  } = useQuery({
    queryKey: [
      "manager-results",
      page,
      size,
      parsedExaminationId,
      parsedClassId,
      parsedStreamId,
      parsedSubjectId,
      applyVersion,
    ],
    queryFn: () =>
      getResults({
        page,
        size,
        examinationId: parsedExaminationId,
        classId: parsedClassId,
        streamId: parsedStreamId,
        subjectId: parsedSubjectId,
      }),
  });

  const {
    data: analytics,
    isLoading: analyticsLoading,
  } = useQuery({
    queryKey: [
      "manager-results-analytics",
      parsedExaminationId,
      parsedClassId,
      parsedStreamId,
      parsedSubjectId,
    ],
    queryFn: () =>
      getResultsAnalytics({
        examinationId: parsedExaminationId,
        classId: parsedClassId,
        streamId: parsedStreamId,
        subjectId: parsedSubjectId,
      }),
  });

  const { data: examinationsData } = useQuery({
    queryKey: ["manager-result-examinations"],
    queryFn: () => getExaminations(0, 500),
  });

  const { data: classesData } = useQuery({
    queryKey: ["manager-result-classes"],
    queryFn: () => getClasses(0, 500),
  });

  const { data: subjectsData } = useQuery({
    queryKey: ["manager-result-subjects"],
    queryFn: () => getSubjects(0, 500),
  });

  const { data: streamsData, isLoading: streamsLoading } = useQuery({
    queryKey: ["manager-result-streams", parsedClassId],
    queryFn: () =>
      parsedClassId ? getStreamsByClass(parsedClassId) : Promise.resolve<Stream[]>([]),
    enabled: !!parsedClassId,
  });

  const { data: examinationSubjects, isLoading: subjectsLoading } = useQuery({
    queryKey: ["manager-result-examination-subjects", parsedExaminationId],
    queryFn: () =>
      parsedExaminationId
        ? getAllExaminationSubjects(parsedExaminationId)
        : Promise.resolve([]),
    enabled: !!parsedExaminationId,
  });

  const { data: studentsData } = useQuery({
    queryKey: ["manager-result-students"],
    queryFn: () => getStudents(0, 500),
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateResultRequest) => createResult(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager-results"] });
      queryClient.invalidateQueries({ queryKey: ["manager-results-analytics"] });
      setOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateResultRequest }) =>
      updateResult(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager-results"] });
      queryClient.invalidateQueries({ queryKey: ["manager-results-analytics"] });
      setOpen(false);
      setEditResult(null);
    },
  });

  const submitMutation = useMutation({
    mutationFn: submitResult,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager-results"] });
      queryClient.invalidateQueries({ queryKey: ["manager-results-analytics"] });
    },
  });

  const approveMutation = useMutation({
    mutationFn: approveResult,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager-results"] });
      queryClient.invalidateQueries({ queryKey: ["manager-results-analytics"] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, comment }: { id: number; comment?: string }) =>
      rejectResult(id, comment),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager-results"] });
      queryClient.invalidateQueries({ queryKey: ["manager-results-analytics"] });
    },
  });

  const publishMutation = useMutation({
    mutationFn: publishResult,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["manager-results"] });
      queryClient.invalidateQueries({ queryKey: ["manager-results-analytics"] });
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ResultFormValues>({
    resolver: zodResolver(resultSchema),
    defaultValues: {
      studentId: 0,
      examinationSubjectId: 0,
      marksObtained: undefined,
      remarks: "",
    },
  });

  const studentIdValue = watch("studentId");
  const examSubjectIdValue = watch("examinationSubjectId");

  useEffect(() => {
    if (!open) {
      setSelectedExaminationId("");
    }
  }, [open]);

  useEffect(() => {
    setStreamId("");
  }, [classId, streamsData]);

  useEffect(() => {
    const subjectValues = new Set(
      (examinationSubjects || []).map((es) => String(es.id)),
    );
    if (subjectId !== "" && !subjectValues.has(subjectId)) {
      setSubjectId("");
    }
  }, [examinationSubjects, classId, examinationId, subjectId]);

  const handleCreate = (values: ResultFormValues) => {
    createMutation.mutate(values);
  };

  const handleUpdate = (values: ResultFormValues) => {
    if (editResult) {
      updateMutation.mutate({ id: editResult.id, data: values });
    }
  };

  const handleFormSubmit = editResult
    ? handleSubmit(handleUpdate)
    : handleSubmit(handleCreate);

  const mutationError =
    createMutation.error ||
    updateMutation.error ||
    submitMutation.error ||
    approveMutation.error ||
    rejectMutation.error ||
    publishMutation.error;

  const openCreateDialog = () => {
    setEditResult(null);
    setSelectedExaminationId("");
    reset({
      studentId: 0,
      examinationSubjectId: 0,
      marksObtained: undefined,
      remarks: "",
    });
    setOpen(true);
  };

  const openEditDialog = (record: ResultResponse) => {
    setEditResult(record);
    setSelectedExaminationId(record.examinationId ? String(record.examinationId) : "");
    reset({
      studentId: record.studentId,
      examinationSubjectId: record.examinationSubjectId,
      marksObtained: record.marksObtained || undefined,
      remarks: record.remarks || "",
    });
    setOpen(true);
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "PUBLISHED":
      case "APPROVED":
        return <Badge variant="success">{status}</Badge>;
      case "SUBMITTED":
        return <Badge variant="warning">{status}</Badge>;
      case "REJECTED":
        return <Badge variant="destructive">{status}</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const examinationOptions: FilterOption[] = useMemo(
    () =>
      (examinationsData?.content || []).map((exam: any) => {
        const labelParts = [exam.name];
        if (exam.academicYearName) labelParts.push(exam.academicYearName);
        if (exam.typeName) labelParts.push(exam.typeName);
        if (exam.startDate) labelParts.push(exam.startDate);
        return {
          value: String(exam.id),
          label: labelParts.filter(Boolean).join(" – "),
        };
      }),
    [examinationsData],
  );

  const classOptions: FilterOption[] = useMemo(
    () =>
      (classesData?.content || []).map((c: Class) => ({
        value: String(c.id),
        label: c.name,
      })),
    [classesData],
  );

  const streamOptions: FilterOption[] = useMemo(
    () =>
      (streamsData || []).map((s: Stream) => ({
        value: String(s.id),
        label: `${s.name}${s.code ? ` (${s.code})` : ""}`,
      })),
    [streamsData],
  );

  const subjectOptions: FilterOption[] = useMemo(() => {
    const subjectById = new Map(
      (subjectsData?.content || []).map((s: any) => [String(s.id), s.name]),
    );
    const classById = new Map(
      (classesData?.content || []).map((c: Class) => [String(c.id), c.name]),
    );
    const streamById = new Map(
      (streamsData || []).map((s: Stream) => [String(s.id), s.name]),
    );
    const items: FilterOption[] = [];
    const seen = new Set<string>();
    (examinationSubjects || []).forEach((es: any) => {
      if (parsedClassId !== undefined && es.classId !== parsedClassId) {
        return;
      }
      const key = String(es.id);
      if (seen.has(key)) return;
      seen.add(key);
      const subjectName = subjectById.get(String(es.subjectId)) || "";
      const className = classById.get(String(es.classId)) || "";
      const streamName =
        es.streamId != null ? streamById.get(String(es.streamId)) || "" : "";
      items.push({
        value: key,
        label: [
          subjectName,
          className ? `[${className}]` : null,
          streamName ? `(${streamName})` : null,
          `Max: ${es.maximumMarks ?? "-"}`,
        ]
          .filter(Boolean)
          .join(" "),
      });
    });
    return items;
  }, [examinationSubjects, subjectsData, classesData, streamsData, parsedClassId]);

  const studentOptions = useMemo(() => {
    return (studentsData?.content || []).map((student: Student) => ({
      value: String(student.id),
      label: `${student.firstName} ${student.lastName}${
        student.admissionNumber ? ` (${student.admissionNumber})` : ""
      }`,
    }));
  }, [studentsData]);

  const dialogSubjectOptions: FilterOption[] = useMemo(() => {
    const subjectById = new Map(
      (subjectsData?.content || []).map((s: any) => [String(s.id), s.name]),
    );
    return (examinationSubjects || []).map((es: any) => {
      const subjectName = subjectById.get(String(es.subjectId)) || "";
      return {
        value: String(es.id),
        label: `${subjectName} (Max: ${es.maximumMarks ?? "-"})`,
      };
    });
  }, [examinationSubjects, subjectsData]);

  const results = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const hasAnyFilter =
    examinationId !== "" || classId !== "" || streamId !== "" || subjectId !== "";

  const a = analytics;
  const totalStudents = a?.totalStudentsAssessed ?? 0;
  const totalResults = a?.totalResults ?? 0;
  const averageMarks = a?.overallAverage ?? null;
  const passed = a?.passedResults ?? 0;
  const failed = a?.failedResults ?? 0;
  const draftResults = a?.draftResults ?? 0;
  const passRate = a?.passRate ?? 0;
  const failRate = a?.failRate ?? 0;

  const gradeDistribution = useMemo(() => {
    return (a?.gradeDistribution || []).map((item, index) => ({
      ...item,
      fill: `hsl(${200 + index * 40}, 70%, 50%)`,
    }));
  }, [a]);

  const subjectPerformance = useMemo(() => {
    return (a?.subjectPerformance || []).slice(0, 10).map((item, index) => ({
      ...item,
      fill: `hsl(${140 + index * 30}, 70%, 50%)`,
    }));
  }, [a]);

  const classPerformance = useMemo(() => {
    return (a?.classPerformance || []).slice(0, 10).map((item, index) => ({
      ...item,
      fill: `hsl(${260 + index * 35}, 70%, 50%)`,
    }));
  }, [a]);

  const resetFilters = () => {
    setExaminationId("");
    setClassId("");
    setStreamId("");
    setSubjectId("");
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold">Results</h1>
          <p className="mt-1 text-muted-foreground">
            Review, filter, and analyse examination results for the school.
          </p>
        </div>
        <Button onClick={openCreateDialog}>
          <Plus className="mr-2 h-4 w-4" /> Add Result
        </Button>
      </div>

      {resultsError && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load results</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(resultsErrorObj, "Results could not be loaded.")}
            <Button
              className="ml-3"
              size="sm"
              variant="outline"
              onClick={() => void refetchResults()}
            >
              Try again
            </Button>
          </AlertDescription>
        </Alert>
      )}
      {mutationError && (
        <Alert variant="destructive">
          <AlertTitle>Result action failed</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(mutationError, "Check the result details and try again.")}
          </AlertDescription>
        </Alert>
      )}

      <Card className="p-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between border-b border-border pb-4">
          <div className="flex flex-wrap gap-3">
            <SearchableSelect
              label="Examination"
              value={examinationId}
              options={examinationOptions}
              placeholder="Select examination"
              onChange={(event) => {
                setExaminationId(event.target.value);
                setSubjectId("");
              }}
              emptyMessage="No examinations available"
            />
            <SearchableSelect
              label="Class"
              value={classId}
              options={classOptions}
              placeholder="Select class"
              onChange={(event) => {
                setClassId(event.target.value);
                setStreamId("");
                setSubjectId("");
              }}
              emptyMessage="No classes available"
            />
            <SearchableSelect
              label="Stream"
              value={streamId}
              options={streamOptions}
              placeholder="Select stream"
              onChange={(event) => {
                setStreamId(event.target.value);
              }}
              disabled={!parsedClassId || streamsLoading}
              emptyMessage={!parsedClassId ? "Select a class first" : "No streams available"}
            />
            <SearchableSelect
              label="Subject"
              value={subjectId}
              options={subjectOptions}
              placeholder="Select subject"
              onChange={(event) => {
                setSubjectId(event.target.value);
              }}
              disabled={
                !parsedExaminationId || subjectsLoading || subjectOptions.length === 0
              }
              emptyMessage={!parsedExaminationId
                ? "Select an examination first"
                : "No subjects available"}
            />
          </div>
          <div className="flex gap-2">
            <Button
              onClick={() => {
                setPage(0);
                setApplyVersion((v) => v + 1);
              }}
              disabled={
                resultsLoading ||
                analyticsLoading ||
                createMutation.isPending ||
                updateMutation.isPending ||
                submitMutation.isPending ||
                approveMutation.isPending ||
                rejectMutation.isPending ||
                publishMutation.isPending
              }
            >
              Apply Filters
            </Button>
            <Button
              variant="outline"
              onClick={resetFilters}
              disabled={
                !hasAnyFilter ||
                resultsLoading ||
                analyticsLoading ||
                createMutation.isPending ||
                updateMutation.isPending ||
                submitMutation.isPending ||
                approveMutation.isPending ||
                rejectMutation.isPending ||
                publishMutation.isPending
              }
            >
              <RefreshCw className="mr-2 h-4 w-4" /> Reset
            </Button>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
          <Card className="p-4">
            <CardDescription className="mb-1">Students assessed</CardDescription>
            <div className="text-2xl font-bold">{totalStudents}</div>
          </Card>
          <Card className="p-4">
            <CardDescription className="mb-1">Total results</CardDescription>
            <div className="text-2xl font-bold">{totalResults}</div>
          </Card>
          <Card className="p-4">
            <CardDescription className="mb-1">Average marks</CardDescription>
            <div className="text-2xl font-bold">{formatAverage(averageMarks)}</div>
          </Card>
          <Card className="p-4">
            <CardDescription className="mb-1">
              Passed <span className="text-xs font-normal text-muted-foreground block">
                {formatPercent(passRate)}%
              </span>
            </CardDescription>
            <div className="text-2xl font-bold">{passed}</div>
          </Card>
          <Card className="p-4">
            <CardDescription className="mb-1">
              Failed <span className="text-xs font-normal text-muted-foreground block">
                {formatPercent(failRate)}%
              </span>
            </CardDescription>
            <div className="text-2xl font-bold">{failed}</div>
          </Card>
          <Card className="p-4">
            <CardDescription className="mb-1">Draft results</CardDescription>
            <div className="text-2xl font-bold">{draftResults}</div>
          </Card>
        </div>

        <Separator className="mt-6 mb-4" />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card className="p-4 col-span-1 lg:col-span-1">
            <CardHeader>
              <CardTitle>Grade distribution</CardTitle>
              <CardDescription>
                Results per grade based on the examination's grading scheme.
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-0">
              {!analyticsLoading && a && totalResults > 0 && gradeDistribution.length > 0 ? (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={gradeDistribution}>
                    <CartesianGrid strokeDasharray="3 3" className="text-border" />
                    <XAxis dataKey="grade" />
                    <YAxis />
                    <Tooltip
                      labelFormatter={(label: string) => `Grade ${label}`}
                      formatter={(value: number) => `${value} result(s)`}
                    />
                    <Legend />
                    <Bar dataKey="count" fill="#2563eb" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState
                  title="No grades to display"
                  description="Select an examination or record results to see the grade distribution."
                />
              )}
            </CardContent>
          </Card>

          <Card className="p-4 col-span-1 lg:col-span-1">
            <CardHeader>
              <CardTitle>Pass vs fail</CardTitle>
              <CardDescription>
                Submitted results compared against the examination pass mark.
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-0">
              {!analyticsLoading && totalResults > 0 ? (
                <ResponsiveContainer width="100%" height={240}>
                  <PieChart>
                    <Tooltip
                      labelFormatter={(label: string) => label}
                      formatter={(value: number) => `${value} (${formatPercent(
                        (value / totalResults) * 100,
                      )}%)`}
                    />
                    <Legend />
                    <Pie
                      data={[
                        { name: "Passed", value: passed, fill: "hsl(142, 70%, 50%)" },
                        { name: "Failed", value: failed, fill: "hsl(0, 70%, 50%)" },
                      ]}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      outerRadius={80}
                      innerRadius={40}
                      paddingAngle={3}
                    >
                      {[
                        { name: "Passed", fill: "hsl(142, 70%, 50%)" },
                        { name: "Failed", fill: "hsl(0, 70%, 50%)" },
                      ].map((option, index) => (
                        <Cell key={`cell-${index}`} fill={option.fill} />
                      ))}
                    </Pie>
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState
                  title="No results to compare"
                  description="Select filters that return results to see the pass vs fail breakdown."
                />
              )}
            </CardContent>
          </Card>
        </div>

        <div className="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card className="p-4">
            <CardHeader>
              <CardTitle>Average marks by subject</CardTitle>
              <CardDescription>
                Average obtained marks per subject for the selected filters.
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-0">
              {!analyticsLoading && a && totalResults > 0 && subjectPerformance.length > 0 ? (
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={subjectPerformance}>
                    <CartesianGrid strokeDasharray="3 3" className="text-border" />
                    <XAxis dataKey="subjectName" />
                    <YAxis />
                    <Tooltip
                      labelFormatter={(label: string) => label}
                      formatter={(value: number) => `${formatAverage(value)} marks`}
                    />
                    <Legend />
                    <Bar dataKey="average" fill="#059669" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState
                  title="No subject averages to display"
                  description="Select filters that return results with marks to see per-subject averages."
                />
              )}
            </CardContent>
          </Card>

          <Card className="p-4">
            <CardHeader>
              <CardTitle>Performance by class</CardTitle>
              <CardDescription>
                Average marks per class for the selected filters.
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-0">
              {!analyticsLoading && a && totalResults > 0 && classPerformance.length > 0 ? (
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={classPerformance}>
                    <CartesianGrid strokeDasharray="3 3" className="text-border" />
                    <XAxis dataKey="className" />
                    <YAxis />
                    <Tooltip
                      labelFormatter={(label: string) => label}
                      formatter={(value: number) => `${formatAverage(value)} marks`}
                    />
                    <Legend />
                    <Bar dataKey="average" fill="#7c3aed" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <EmptyState
                  title="No class performance to display"
                  description="Select filters that return results with marks to see per-class averages."
                />
              )}
            </CardContent>
          </Card>
        </div>

        <div className="mt-6 rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Student</TableHead>
                <TableHead>Admission No</TableHead>
                <TableHead>Examination</TableHead>
                <TableHead>Class</TableHead>
                <TableHead>Stream</TableHead>
                <TableHead>Subject</TableHead>
                <TableHead className="text-right">Marks</TableHead>
                <TableHead className="text-center">Grade</TableHead>
                <TableHead className="text-center">Grade Point</TableHead>
                <TableHead className="text-center">Status</TableHead>
                <TableHead className="w-32">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {!resultsLoading && !resultsError && results.length === 0 && (
                <TableRow>
                  <TableCell
                    colSpan={11}
                    className="text-center text-muted-foreground py-4"
                  >
                    {hasAnyFilter
                      ? "No results match the selected filters"
                      : "No results found"}
                  </TableCell>
                </TableRow>
              )}
              {results.map((result) => (
                <TableRow key={result.id}>
                  <TableCell className="font-medium">
                    {result.studentName || "Unknown student"}
                  </TableCell>
                  <TableCell>{result.admissionNumber ?? "-"}</TableCell>
                  <TableCell>{result.examinationName ?? "-"}</TableCell>
                  <TableCell>{result.className ?? "-"}</TableCell>
                  <TableCell>{result.streamName ?? "-"}</TableCell>
                  <TableCell>{result.subjectName ?? "-"}</TableCell>
                  <TableCell className="text-right">{formatMarks(result.marksObtained)}</TableCell>
                  <TableCell className="text-center">
                    {result.grade ?? "-"}
                  </TableCell>
                  <TableCell className="text-center">
                    {result.gradePoint != null ? result.gradePoint.toFixed(2) : "-"}
                  </TableCell>
                  <TableCell className="text-center">{getStatusBadge(result.status)}</TableCell>
                  <TableCell>
                <div className="flex items-center gap-1">
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => openEditDialog(result)}
                    title="Edit"
                  >
                    <Edit className="h-4 w-4" />
                  </Button>
                  {result.status === "DRAFT" && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => submitMutation.mutate(result.id)}
                      disabled={submitMutation.isPending}
                      title="Submit"
                    >
                      <Send className="h-4 w-4" />
                    </Button>
                  )}
                  {result.status === "SUBMITTED" && (
                    <>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => approveMutation.mutate(result.id)}
                        disabled={approveMutation.isPending}
                        title="Approve"
                      >
                        <CheckCircle className="h-4 w-4 text-green-600 dark:text-green-400" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => rejectMutation.mutate({ id: result.id, comment: "" })}
                        disabled={rejectMutation.isPending}
                        title="Reject"
                      >
                        <XCircle className="h-4 w-4 text-destructive" />
                      </Button>
                    </>
                  )}
                  {result.status === "APPROVED" && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => publishMutation.mutate(result.id)}
                      disabled={publishMutation.isPending}
                      title="Publish"
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  )}
                </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-muted-foreground">
              {resultsError ? "" : `Page ${page + 1} of ${totalPages}`}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0 || resultsLoading}
                onClick={() => setPage((p) => p - 1)}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages || resultsLoading}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </div>

        {resultsLoading && (
          <div className="mt-4 space-y-3">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        )}
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent
          title={editResult ? "Edit Result" : "Add Result"}
          description={editResult ? "Update result details" : "Record a new result"}
        >
          <form onSubmit={handleFormSubmit} className="space-y-4">
            <SearchableSelect
              label="Student *"
              value={studentIdValue ? String(studentIdValue) : ""}
              options={studentOptions}
              placeholder="Select Student"
              onChange={(event) =>
                setValue("studentId", Number(event.target.value), {
                  shouldValidate: true,
                })}
              emptyMessage="No students available"
            />
            {errors.studentId && (
              <p className="text-sm text-destructive">{errors.studentId.message}</p>
            )}

            <SearchableSelect
              label="Examination *"
              value={selectedExaminationId}
              options={examinationOptions}
              placeholder="Select Examination"
              onChange={(event) => {
                setSelectedExaminationId(event.target.value);
                setValue("examinationSubjectId", 0, { shouldValidate: true });
              }}
              emptyMessage="No examinations available"
            />

            <SearchableSelect
              label="Examination Subject *"
              value={examSubjectIdValue ? String(examSubjectIdValue) : ""}
              options={dialogSubjectOptions}
              placeholder={
                selectedExaminationId
                  ? subjectsLoading
                    ? "Loading..."
                    : "Select Examination Subject"
                  : "Select an examination first"
              }
              onChange={(event) =>
                setValue("examinationSubjectId", Number(event.target.value), {
                  shouldValidate: true,
                })}
              disabled={!selectedExaminationId || subjectsLoading}
              emptyMessage={selectedExaminationId
                ? "No subjects available for this examination"
                : "Select an examination first"}
            />
            {errors.examinationSubjectId && (
              <p className="text-sm text-destructive">
                {errors.examinationSubjectId.message}
              </p>
            )}

            <div className="space-y-2">
              <label className="text-sm font-medium">Marks Obtained</label>
              <Input type="number" {...register("marksObtained", { valueAsNumber: true })} />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Remarks</label>
              <Input {...register("remarks")} />
            </div>
            <div className="flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setOpen(false);
                  setEditResult(null);
                  setSelectedExaminationId("");
                }}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={
                  createMutation.isPending || updateMutation.isPending
                }
              >
                {createMutation.isPending || updateMutation.isPending
                  ? "Saving..."
                  : editResult
                    ? "Update"
                    : "Create"}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export type { ResultResponse } from "@/types/academic";
export type { Result } from "@/services/assessments";