import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getReports,
  createReport,
  getReportDefinitions,
  getReportOptions,
  downloadReport,
  regenerateReport,
  deleteReport,
} from '@/services/reports';
import type {
  CreateReportJobRequest,
  ReportJob,
  ReportFilterDefinitionsResponse,
  ReportFilterFieldDto,
  ReportOptionDto,
} from '@/types/reports';
import type { PaginatedResponse } from '@/types/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Separator } from '@/components/ui/Separator';
import { Skeleton } from '@/components/ui/Skeleton';
import { SearchableSelect } from '@/components/ui/SearchableSelect';
import { Plus, Download, RefreshCw, Trash2, Eye, FileText, DownloadIcon } from 'lucide-react';
import { useState, useEffect, useMemo, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';

const schema = z.object({
  reportType: z.string().min(1, 'Report type is required'),
  reportName: z.string().min(1, 'Report name is required'),
  dateFrom: z.string().optional(),
  dateTo: z.string().optional(),
});

type FormValues = z.infer<typeof schema> & {
  academicYearId?: number | null;
  classId?: number | null;
  streamId?: number | null;
  departmentId?: number | null;
  subjectId?: number | null;
  examinationId?: number | null;
  studentId?: number | null;
  status?: string | null;
  gender?: string | null;
  includeFields?: string[] | null;
};

const DATE_RANGE_TYPES = new Set([
  'DAILY_ATTENDANCE',
  'ATTENDANCE_BY_DATE_RANGE',
  'ATTENDANCE_BY_CLASS',
  'ATTENDANCE_BY_STREAM',
  'INDIVIDUAL_STUDENT_ATTENDANCE',
  'ATTENDANCE_SUMMARY',
  'RESULTS_BY_CLASS',
  'RESULTS_BY_STREAM',
  'RESULTS_BY_SUBJECT',
  'EXAMINATION_PERFORMANCE_SUMMARY',
  'GRADE_DISTRIBUTION',
  'SUBJECT_PERFORMANCE_REPORT',
  'FINANCIAL_TRANSACTIONS_REPORT',
  'PAYMENTS_REPORT',
  'OUTSTANDING_FEES_REPORT',
  'FEE_COLLECTION_SUMMARY',
  'PAYMENT_HISTORY',
  'SMS_HISTORY',
  'NOTIFICATION_HISTORY',
  'DELIVERY_STATUS_SUMMARY',
  'BOARDING_OCCUPANCY',
  'BOARDING_STATUS_REPORT',
]);

const ACADEMIC_YEAR_TYPES = new Set([
  'STUDENT_DIRECTORY',
  'STUDENTS_BY_CLASS',
  'STUDENTS_BY_STREAM',
  'STUDENT_ENROLLMENT_REPORT',
  'STUDENT_ADMISSION_REPORT',
  'STUDENT_STATUS_REPORT',
  'CLASS_REPORT',
  'STREAM_REPORT',
  'SUBJECT_REPORT',
  'TEACHING_ASSIGNMENTS_REPORT',
  'ACADEMIC_YEAR_REPORT',
  'TIMETABLE_REPORT',
  'DEPARTMENT_REPORT',
  'EXAMINATION_RESULTS',
  'RESULTS_BY_CLASS',
  'RESULTS_BY_STREAM',
  'RESULTS_BY_SUBJECT',
  'INDIVIDUAL_STUDENT_RESULTS',
  'TEACHER_DIRECTORY',
  'TEACHERS_BY_DEPARTMENT',
  'TEACHING_ASSIGNMENTS',
  'BOARDING_STUDENTS',
  'BOARDING_ALLOCATIONS',
]);

function getStatusVariant(status: string) {
  switch (status) {
    case 'COMPLETED': return 'success';
    case 'PROCESSING': return 'warning';
    case 'PENDING': return 'secondary';
    case 'FAILED': return 'destructive';
    default: return 'secondary';
  }
}

function formatDateTime(iso: string | null) {
  if (!iso) return '-';
  try { return new Date(iso).toLocaleString(); } catch { return '-'; }
}

export function ManagerReportsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [preview, setPreview] = useState<{ label: string; value: string }[]>([]);
  const [pdfJobId, setPdfJobId] = useState<number | null>(null);
  const [pdfFilename, setPdfFilename] = useState<string>('');

  const { data: definitionsData } = useQuery<ReportFilterDefinitionsResponse>({
    queryKey: ['report-definitions'],
    queryFn: () => getReportDefinitions(),
  });

  const { data, isLoading } = useQuery<PaginatedResponse<ReportJob>>({
    queryKey: ['reports'],
    queryFn: () => getReports(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateReportJobRequest) => createReport(payload),
    onSuccess: (res: ReportJob) => {
      queryClient.invalidateQueries({ queryKey: ['reports'] });
      setOpen(false);
      setPdfJobId(res.id);
      setPdfFilename(res.fileName || '');
    },
  });

  const regenerateMutation = useMutation({
    mutationFn: (id: number) => regenerateReport(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteReport(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reports'] });
    },
  });

  const reportTypes = useMemo(() => definitionsData?.reportTypes || [], [definitionsData]);
  const items = data?.content || [];

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    setValue,
    getValues,
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      reportType: reportTypes.length > 0 ? reportTypes[0].id : '',
      reportName: '',
      dateFrom: '',
      dateTo: '',
    },
  });

  const reportType = watch('reportType');

  const selectedType = useMemo(
    () => reportTypes.find((t) => t.id === reportType) || null,
    [reportTypes, reportType]
  );

  const showDateRange = selectedType ? DATE_RANGE_TYPES.has(selectedType.id) : false;
  const showAcademicYear = selectedType ? ACADEMIC_YEAR_TYPES.has(selectedType.id) : false;

  // Load options for fields that have endpoints
  const [optionsCache, setOptionsCache] = useState<Record<string, ReportOptionDto[]>>({});

  const loadFieldOptions = useCallback(async (field: ReportFilterFieldDto) => {
    if (!field.optionsEndpoint) return;
    const params: Record<string, number | undefined> = {};
    if (field.dependsOn === 'class') params.classId = Number(getValues('classId') || undefined);
    if (field.dependsOn === 'academicYear') params.academicYearId = Number(getValues('academicYearId') || undefined);
    if (field.dependsOn === 'department') params.departmentId = Number(getValues('departmentId') || undefined);
    try {
      const opts = await getReportOptions(field.optionsEndpoint, params.classId, params.academicYearId, params.departmentId);
      setOptionsCache((prev) => ({ ...prev, [field.name]: opts }));
    } catch {
      setOptionsCache((prev) => ({ ...prev, [field.name]: [] }));
    }
  }, [getValues]);

  useEffect(() => {
    if (!selectedType) return;
    const promises = selectedType.fields
      .filter((f) => f.optionsEndpoint)
      .map((f) => loadFieldOptions(f));
    Promise.all(promises);
  }, [selectedType, loadFieldOptions]);

  useEffect(() => {
    if (selectedType && !reportType) {
      setValue('reportType', selectedType.id);
    }
  }, [selectedType, reportType, setValue]);

  const buildPayload = useCallback((): CreateReportJobRequest => {
    const values = getValues();
    const payload: CreateReportJobRequest = {
      reportType: values.reportType,
      reportName: values.reportName,
    };
    if (showDateRange) {
      if (values.dateFrom) payload.dateFrom = values.dateFrom;
      if (values.dateTo) payload.dateTo = values.dateTo;
    }
    if (showAcademicYear) {
      const ayId = Number(getValues('academicYearId') || 0);
      if (ayId) payload.academicYearId = ayId;
    }
    for (const field of selectedType?.fields || []) {
      const val = getValues(field.name as any);
      if (val === undefined || val === null || val === '') continue;
      switch (field.name) {
        case 'academicYear': payload.academicYearId = Number(val); break;
        case 'class': payload.classId = Number(val); break;
        case 'stream': payload.streamId = Number(val); break;
        case 'department': payload.departmentId = Number(val); break;
        case 'subject': payload.subjectId = Number(val); break;
        case 'examination': payload.examinationId = Number(val); break;
        case 'student': payload.studentId = Number(val); break;
        case 'status': payload.status = String(val); break;
        case 'gender': payload.gender = String(val); break;
      }
    }
    const include = getValues('includeFields') as string[] | undefined;
    if (include && include.length) payload.includeFields = include;
    return payload;
  }, [getValues, selectedType, showDateRange, showAcademicYear]);

  const onSubmit = () => {
    const payload = buildPayload();
    createMutation.mutate(payload);
  };

  const buildPreview = useCallback(() => {
    const values = getValues();
    const rows: { label: string; value: string }[] = [];
    rows.push({ label: 'Report Type', value: selectedType?.label || values.reportType });
    rows.push({ label: 'Report Name', value: values.reportName });
    if (showAcademicYear) rows.push({ label: 'Academic Year', value: String(getValues('academicYearId') || '-') });
    if (showDateRange) {
      if (values.dateFrom) rows.push({ label: 'Date From', value: values.dateFrom });
      if (values.dateTo) rows.push({ label: 'Date To', value: values.dateTo });
    }
    for (const field of selectedType?.fields || []) {
      const val = getValues(field.name as any);
      if (val === undefined || val === null || val === '') continue;
      let label = String(val);
      if (field.options) {
        const opt = field.options.find((o) => o.id === String(val));
        if (opt) label = opt.name;
      }
      rows.push({ label: field.label, value: label });
    }
    const include = getValues('includeFields') as string[] | undefined;
    if (include && include.length) rows.push({ label: 'Information to Include', value: include.join(', ') });
    setPreview(rows);
  }, [getValues, selectedType, showAcademicYear, showDateRange]);

  const handleDownload = async (jobId: number, fileName: string) => {
    try {
      const blob = await downloadReport(jobId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName || `report_${jobId}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch {
      alert('Failed to download PDF. Please try again.');
    }
  };

  const columns = [
    { key: 'reportName', label: 'Report Name' },
    { key: 'reportType', label: 'Type' },
    { key: 'status', label: 'Status' },
    { key: 'startedAt', label: 'Started At' },
    { key: 'completedAt', label: 'Completed At' },
    { key: 'actions', label: 'Actions' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Reports</h1>
          <p className="text-muted-foreground text-sm">
            Generate and download professional, school-branded PDF reports.
          </p>
        </div>
        <Button onClick={() => setOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" /> Generate Report
        </Button>
      </div>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                {columns.map((col) => (
                  <th key={col.key} className="text-left py-3 px-4 font-medium">
                    {col.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i} className="border-b">
                    {columns.map((_, j) => (
                      <td key={j} className="py-3 px-4"><Skeleton className="h-4 w-24" /></td>
                    ))}
                  </tr>
                ))
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={columns.length} className="py-8 text-center text-muted-foreground">
                    No reports found. Generate one to get started.
                  </td>
                </tr>
              ) : (
                items.map((item) => (
                  <tr key={item.id} className="border-b hover:bg-accent">
                    <td className="py-3 px-4 font-medium">{item.reportName}</td>
                    <td className="py-3 px-4">{item.reportType}</td>
                    <td className="py-3 px-4">
                      <Badge variant={getStatusVariant(item.status)}>{item.status}</Badge>
                    </td>
                    <td className="py-3 px-4">{formatDateTime(item.startedAt)}</td>
                    <td className="py-3 px-4">{formatDateTime(item.completedAt)}</td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center gap-1">
                        {item.status === 'COMPLETED' && item.fileName && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDownload(item.id, item.fileName!)}
                            title="Download PDF"
                          >
                            <Download className="h-3 w-3" />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            const j = items.find((x) => x.id === item.id);
                            setPreview([
                              { label: 'Report Name', value: j?.reportName || '' },
                              { label: 'Type', value: j?.reportType || '' },
                              { label: 'Status', value: j?.status || '' },
                              { label: 'Generated', value: formatDateTime(j?.completedAt || null) },
                              { label: 'Parameters', value: j?.parameters || '-' },
                            ]);
                            setOpen(true);
                          }}
                          title="View Details"
                        >
                          <Eye className="h-3 w-3" />
                        </Button>
                        {item.status === 'COMPLETED' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => regenerateMutation.mutate(item.id)}
                            title="Regenerate"
                            disabled={regenerateMutation.isPending}
                          >
                            <RefreshCw className="h-3 w-3" />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => deleteMutation.mutate(item.id)}
                          title="Delete"
                        >
                          <Trash2 className="h-3 w-3" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {pdfJobId && pdfFilename && (
        <div className="flex items-center gap-2 p-3 bg-blue-50 border border-blue-100 rounded-lg text-sm text-blue-900">
          <FileText className="h-4 w-4" /> Report ready.{' '}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleDownload(pdfJobId, pdfFilename)}
            className="text-blue-700 hover:bg-blue-100"
          >
            <DownloadIcon className="h-3 w-3" /> Download PDF
          </Button>
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent title="Generate Report" description="Select a report type and fill in the filters. No JSON required.">
          <Separator />
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="col-span-full md:col-span-1">
                <label className="block text-sm font-medium mb-1">Report Type</label>
                <Select
                  options={reportTypes.map((t) => ({ value: t.id, label: t.label }))}
                  {...register('reportType')}
                  onChange={(e) => {
                    setValue('reportType', e.target.value);
                    setValue('dateFrom', '');
                    setValue('dateTo', '');
                  }}
                />
                {errors.reportType && <p className="text-sm text-destructive mt-1">{errors.reportType.message}</p>}
              </div>
              <div className="col-span-full md:col-span-1">
                <label className="block text-sm font-medium mb-1">Report Name</label>
                <Input {...register('reportName')} placeholder="e.g. Student Directory 2026/2027" />
                {errors.reportName && <p className="text-sm text-destructive mt-1">{errors.reportName.message}</p>}
              </div>
            </div>

            {selectedType && (
              <div className="space-y-3">
                {selectedType.fields.map((field) => {
                  const show =
                    field.type === 'date' ? showDateRange :
                    field.name === 'academicYear' ? showAcademicYear :
                    true;
                  if (!show) return null;
                  const isDependent = field.dependsOn && !getValues(field.dependsOn as any);
                  const rawOpts = optionsCache[field.name] || field.options || [];
                  const opts = rawOpts.map((o) => ({ value: o.id, label: o.name }));
                  return (
                    <div key={field.name} className="space-y-1">
                      <label className="block text-sm font-medium">
                        {field.label}
                        {field.required && <span className="text-destructive ml-1">*</span>}
                      </label>
                      {field.type === 'multiselect' && (
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                          {opts.map((o) => (
                            <label key={o.value} className="flex items-center gap-2 text-sm p-2 border rounded cursor-pointer hover:bg-accent">
                              <input
                                type="checkbox"
                                value={o.value}
                                onChange={(e) => {
                                  const cur = (getValues('includeFields') as string[]) || [];
                                  if (e.target.checked) setValue('includeFields', [...cur, o.value]);
                                  else setValue('includeFields', cur.filter((id) => id !== o.value));
                                }}
                                defaultChecked={(getValues('includeFields') as string[] | undefined)?.includes(o.value)}
                              />
                              {o.label}
                            </label>
                          ))}
                        </div>
                      )}
                      {field.type !== 'multiselect' && (
                        <>
                          <SearchableSelect
                            label={field.label}
                            value={String(getValues(field.name as any) || '')}
                            options={opts}
                            placeholder={field.placeholder || `Select ${field.label.toLowerCase()}`}
                            disabled={Boolean(isDependent)}
                            onChange={(e) => setValue(field.name as any, e.target.value)}
                            emptyMessage={isDependent ? `Select a ${field.dependsOn} first` : 'No options available'}
                          />
                          {field.type === 'date' && (
                            <Input type="date" {...register(field.name as any)} />
                          )}
                        </>
                      )}
                      {field.helperText && (
                        <p className="text-xs text-muted-foreground">{field.helperText}</p>
                      )}
                      {errors[field.name as keyof FormValues] && (
                        <p className="text-sm text-destructive mt-1">
                          {errors[field.name as keyof FormValues]?.message}
                        </p>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            <div className="border-t pt-4">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={buildPreview}
                disabled={createMutation.isPending}
              >
                <FileText className="h-3 w-3" /> Preview Parameters
              </Button>
              {preview.length > 0 && (
                <div className="mt-3 p-3 bg-muted rounded border border-border">
                  <h3 className="text-xs font-semibold mb-2 uppercase tracking-wider">Preview</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">
                    {preview.map((p) => (
                      <div key={p.label} className="flex justify-between border-b pb-1">
                        <span className="text-muted-foreground">{p.label}</span>
                        <span>{p.value}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setOpen(false)} disabled={createMutation.isPending}>
                Cancel
              </Button>
              <Button type="submit" disabled={createMutation.isPending || !selectedType}>
                {createMutation.isPending ? 'Generating...' : 'Generate PDF'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
