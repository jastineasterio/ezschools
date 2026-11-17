import { useState, useCallback, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getGradingSchemes,
  getGradingScheme,
  createGradingScheme,
  updateGradingScheme,
  addGradingSchemeRange,
  updateGradingSchemeRange,
  deleteGradingSchemeRange,
  toggleGradingSchemeStatus,
  deleteGradingScheme,
  type GradingScheme,
  type GradingSchemeRange,
  type CreateGradingSchemeRequest,
  type AddGradingSchemeRangeRequest,
} from '@/services/assessments';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { Dialog, DialogContent, DialogClose } from '@/components/ui/Dialog';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import { Separator } from '@/components/ui/Separator';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TableCaption,
} from '@/components/ui/Table';
import { Plus, Edit2, Trash2, ArrowLeft, Loader2 } from 'lucide-react';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { getApiErrorMessage } from '@/services/api';

const rangeSchema = z.object({
  grade: z.string().min(1, 'Grade is required'),
  minMark: z.number().min(0, 'Minimum mark must be >= 0'),
  maxMark: z.number().min(0, 'Maximum mark must be >= 0'),
  gradePoint: z.number().optional(),
  remark: z.string().optional(),
});

type RangeFormValues = z.infer<typeof rangeSchema>;

interface RangeRow {
  id: number;
  grade: string;
  minMark: number | null;
  maxMark: number | null;
  gradePoint: number | null;
  remark: string;
}

interface RangeFieldErrors {
  grade?: string;
  minMark?: string;
  maxMark?: string;
}

const dialogSchemeSchema = z.object({
  name: z.string().min(1, 'Scheme name is required'),
  description: z.string().optional().or(z.literal('')),
  isDefault: z.boolean().default(false),
  status: z.enum(['ACTIVE', 'INACTIVE']),
});

type DialogSchemeValues = z.infer<typeof dialogSchemeSchema>;

let createDialogRangeId = 0;
const nextCreateDialogRangeId = () => ++createDialogRangeId;

const emptyCreateDialogRange = (): RangeRow => ({
  id: nextCreateDialogRangeId(),
  grade: '',
  minMark: null,
  maxMark: null,
  gradePoint: null,
  remark: '',
});

const parseCreateDialogNum = (value: string): number | null => {
  if (value === '' || value == null) return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
};

function statusBadge(status: string) {
  return (
    <Badge variant={status === 'ACTIVE' ? 'success' : 'secondary'}>
      {status}
    </Badge>
  );
}

export function ManagerGradingSchemesPage() {
  const queryClient = useQueryClient();
  const [view, setView] = useState<'list' | 'scheme'>('list');
  const [selectedSchemeId, setSelectedSchemeId] = useState<number | null>(null);
  const [addRangeOpen, setAddRangeOpen] = useState(false);
  const [editingRange, setEditingRange] = useState<GradingSchemeRange | null>(null);

  const { data: schemesData, isLoading: schemesLoading } = useQuery({
    queryKey: ['manager-grading-schemes'],
    queryFn: () => unwrapGradingSchemes(),
  });

  const schemes = schemesData?.content ?? [];

  const updateSchemeMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateGradingSchemeRequest }) =>
      updateGradingScheme(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['manager-grading-schemes'] });
      if (selectedSchemeId) queryClient.invalidateQueries({ queryKey: ['manager-grading-scheme', selectedSchemeId] });
    },
  });

  const addRangeMutation = useMutation({
    mutationFn: ({ schemeId, data }: { schemeId: number; data: AddGradingSchemeRangeRequest }) =>
      addGradingSchemeRange(schemeId, data),
    onSuccess: () => {
      if (selectedSchemeId) queryClient.invalidateQueries({ queryKey: ['manager-grading-scheme', selectedSchemeId] });
    },
  });

  const updateRangeMutation = useMutation({
    mutationFn: ({ schemeId, rangeId, data }: { schemeId: number; rangeId: number; data: AddGradingSchemeRangeRequest }) =>
      updateGradingSchemeRange(schemeId, rangeId, data),
    onSuccess: () => {
      if (selectedSchemeId) queryClient.invalidateQueries({ queryKey: ['manager-grading-scheme', selectedSchemeId] });
      setEditingRange(null);
    },
  });

  const deleteRangeMutation = useMutation({
    mutationFn: ({ schemeId, rangeId }: { schemeId: number; rangeId: number }) =>
      deleteGradingSchemeRange(schemeId, rangeId),
    onSuccess: () => {
      if (selectedSchemeId) queryClient.invalidateQueries({ queryKey: ['manager-grading-scheme', selectedSchemeId] });
    },
  });

  const toggleStatusMutation = useMutation({
    mutationFn: ({ schemeId, status }: { schemeId: number; status: string }) =>
      toggleGradingSchemeStatus(schemeId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['manager-grading-schemes'] });
      if (selectedSchemeId) queryClient.invalidateQueries({ queryKey: ['manager-grading-scheme', selectedSchemeId] });
    },
  });

  const deleteSchemeMutation = useMutation({
    mutationFn: (schemeId: number) => deleteGradingScheme(schemeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['manager-grading-schemes'] });
      setView('list');
      setSelectedSchemeId(null);
    },
  });

  const schemeQuery = useQuery<GradingScheme | null>({
    queryKey: ['manager-grading-scheme', selectedSchemeId],
    queryFn: async () => {
      if (!selectedSchemeId) return null;
      const data = await getGradingScheme(selectedSchemeId);
      return data;
    },
    enabled: !!selectedSchemeId,
  });

  const scheme = schemeQuery.data;

  const schemeError =
    updateSchemeMutation.error ||
    addRangeMutation.error ||
    updateRangeMutation.error ||
    deleteRangeMutation.error ||
    toggleStatusMutation.error ||
    deleteSchemeMutation.error;

  if (schemesLoading) {
    return (
      <main className="mx-auto max-w-6xl space-y-6">
        <h1 className="text-3xl font-bold">Grading Schemes</h1>
        <Card>
          <CardContent>
            <div className="space-y-3">
              {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-8 w-full" />)}
            </div>
          </CardContent>
        </Card>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-6xl space-y-6">
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-3xl font-bold">Grading Schemes</h1>
        <p className="text-sm text-muted-foreground">
          Create and manage grade ranges for your school using actual marks.
        </p>
      </header>

      {schemeError && (
        <Alert variant="destructive">
          <AlertTitle>Operation failed</AlertTitle>
          <AlertDescription>{getApiErrorMessage(schemeError, 'Please try again.')}</AlertDescription>
        </Alert>
      )}

      {view === 'list' && (
        <Card>
          <CardHeader className="flex items-center justify-between">
            <CardTitle>Grading Schemes</CardTitle>
            <div className="flex gap-2">
              <Button
                onClick={() => {
                  setSelectedSchemeId(null);
                  setView('scheme');
                }}
              >
                <Plus className="mr-2 h-4 w-4" /> Create Scheme
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {schemes.length === 0 && (
              <p className="text-sm text-muted-foreground">
                No grading schemes configured. Create one to get started.
              </p>
            )}
            <div className="space-y-2">
              {schemes.map((s) => (
                <Card
                  key={s.id}
                  className="border hover:border-primary/50 transition-colors cursor-pointer"
                  onClick={() => {
                    setSelectedSchemeId(s.id);
                    setView('scheme');
                  }}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium">{s.name}</p>
                      <p className="text-xs text-muted-foreground">{s.description ?? 'No description'}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      {statusBadge(s.status)}
                      {s.isDefault && <Badge variant="secondary">Default</Badge>}
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {view === 'scheme' && selectedSchemeId == null && (
        <CreateSchemeDialog
          open={view === 'scheme' && selectedSchemeId == null}
          onOpenChange={(open) => {
            if (!open) setView('list');
          }}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: ['manager-grading-schemes'] });
          }}
        />
      )}

      {view === 'scheme' && scheme && (
        <>
          <Button variant="outline" size="sm" onClick={() => setView('list')}>
            <ArrowLeft className="mr-2 h-4 w-4" /> Back
          </Button>
          <Card>
            <CardHeader>
              <CardTitle>{scheme.name}</CardTitle>
              <p className="text-sm text-muted-foreground">{scheme.description ?? 'No description'}</p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-3 gap-4">
                <div className="bg-muted p-3 rounded-lg">
                  <p className="text-xs text-muted-foreground">Status</p>
                  <p className="font-medium">{statusBadge(scheme.status)}</p>
                </div>
                <div className="bg-muted p-3 rounded-lg">
                  <p className="text-xs text-muted-foreground">Default</p>
                  <p className="font-medium">{scheme.isDefault ? 'Yes' : 'No'}</p>
                </div>
                <div className="bg-muted p-3 rounded-lg">
                  <p className="text-xs text-muted-foreground">Ranges</p>
                  <p className="font-medium">{scheme.ranges?.length ?? 0}</p>
                </div>
              </div>
              <Separator />
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold">Grade Ranges (actual marks)</h3>
                <Button
                  size="sm"
                  onClick={() => {
                    setAddRangeOpen(true);
                    setEditingRange(null);
                  }}
                >
                  <Plus className="mr-2 h-4 w-4" /> Add Range
                </Button>
              </div>
              {scheme.ranges?.length === 0 && (
                <p className="text-sm text-muted-foreground">No ranges configured. Add one above.</p>
              )}
              <div className="space-y-2">
                {(scheme.ranges ?? []).map((r: GradingSchemeRange) => (
                  <Card key={r.id} className="flex items-center justify-between p-3">
                    <div>
                      <p className="font-medium">{r.grade}</p>
                      <p className="text-xs text-muted-foreground">
                        {r.minMark} – {r.maxMark} marks
                        {r.gradePoint != null ? ` · ${r.gradePoint} pts` : ''}
                      </p>
                      {r.remark && <p className="text-xs text-muted-foreground">{r.remark}</p>}
                    </div>
                    <div className="flex gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                          setEditingRange(r);
                        }}
                      >
                        <Edit2 className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                          if (
                            window.confirm(
                              `Delete the "${r.grade}" grade range from "${scheme.name}"? This cannot be undone.`,
                            )
                          ) {
                            deleteRangeMutation.mutate({ schemeId: scheme.id, rangeId: r.id });
                          }
                        }}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </Card>
                ))}
              </div>
            </CardContent>
            <div className="p-4 border-t flex justify-end gap-2">
              <Button
                onClick={() => {
                  if (
                    window.confirm(
                      `Delete the grading scheme "${scheme.name}" and all its grade ranges? This cannot be undone.`,
                    )
                  ) {
                    deleteSchemeMutation.mutate(scheme.id);
                  }
                }}
                variant="destructive"
              >
                Delete Scheme
              </Button>
              <Button
                onClick={() => {
                  updateSchemeMutation.mutate({
                    id: scheme.id,
                    data: {
                      name: scheme.name,
                      description: scheme.description ?? undefined,
                      isDefault: scheme.isDefault,
                      status: scheme.status,
                    },
                  });
                }}
              >
                Update Scheme
              </Button>
              <Button
                variant="outline"
                onClick={() => {
                  toggleStatusMutation.mutate({
                    schemeId: scheme.id,
                    status: scheme.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
                  });
                }}
              >
                {scheme.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
              </Button>
            </div>
          </Card>

          <div className="mt-6">
            <RangeFormDialog
              open={view === 'scheme' && !!selectedSchemeId && addRangeOpen}
              schemeId={scheme.id}
              onOpenChange={(open) => setAddRangeOpen(open)}
              onSuccess={() => {}}
            />
            <EditRangeDialog
              open={view === 'scheme' && !!selectedSchemeId && editingRange != null}
              schemeId={scheme.id}
              range={editingRange}
              onOpenChange={(open) => {
                if (!open) {
                  setEditingRange(null);
                }
              }}
              onSuccess={() => {}}
            />
          </div>
        </>
      )}
    </main>
  );

  function unwrapGradingSchemes() {
    return getGradingSchemes(undefined, undefined);
  }
}

function CreateSchemeDialog({
  open,
  onOpenChange,
  onSuccess,
}: {
  open: boolean;
  onOpenChange: (_open: boolean) => void;
  onSuccess: () => void;
}) {
  const [ranges, setRanges] = useState<RangeRow[]>([]);
  const [creating, setCreating] = useState(false);
  const [createdSchemeId, setCreatedSchemeId] = useState<number | null>(null);
  const [rangesAdded, setRangesAdded] = useState(0);
  const [rangeError, setRangeError] = useState<string | null>(null);
  const [rangeErrors, setRangeErrors] = useState<Record<number, RangeFieldErrors>>({});

  const {
    register,
    handleSubmit,
    getValues,
    reset,
    formState: { errors: formErrors },
  } = useForm<DialogSchemeValues>({
    resolver: zodResolver(dialogSchemeSchema),
    defaultValues: { name: '', description: '', status: 'ACTIVE' },
  });

  useEffect(() => {
    if (open) {
      reset({ name: '', description: '', status: 'ACTIVE' });
      setRanges([emptyCreateDialogRange()]);
      setCreatedSchemeId(null);
      setRangesAdded(0);
      setRangeError(null);
      setRangeErrors({});
    }
  }, [open, reset]);

  const addGrade = useCallback(() => {
    setRanges((prev) => [...prev, emptyCreateDialogRange()]);
  }, []);

  const removeGrade = useCallback((id: number) => {
    setRanges((prev) => prev.filter((r) => r.id !== id));
  }, []);

  const updateRange = useCallback(
    (id: number, field: 'grade' | 'minMark' | 'maxMark' | 'gradePoint' | 'remark', value: string | number | null) => {
      setRanges((prev) => prev.map((r) => (r.id === id ? { ...r, [field]: value } : r)));
      setRangeErrors((prev) => {
        const next = { ...prev };
        if (next[id]) delete next[id];
        return next;
      });
    },
    []
  );

  const validateCreateDialogRanges = useCallback((): Record<number, RangeFieldErrors> => {
    const errs: Record<number, RangeFieldErrors> = {};
    if (ranges.length === 0) return errs;
    const ordered = [...ranges].sort((a, b) => (a.minMark ?? -1) - (b.minMark ?? -1));
    let prevMax: number | null = null;
    ordered.forEach((row) => {
      const rowErrs: RangeFieldErrors = {};
      const min = row.minMark;
      const max = row.maxMark;
      if (!row.grade.trim()) rowErrs.grade = 'Grade symbol is required';
      if (min === null || min < 0) rowErrs.minMark = 'Minimum marks must be non-negative';
      if (max === null) rowErrs.maxMark = 'Maximum marks is required';
      else if (min !== null && !rowErrs.minMark && max < min) rowErrs.maxMark = 'Maximum must not be less than minimum';
      if (prevMax === null && min !== 0) rowErrs.minMark = 'Grading scale must start at 0';
      else if (prevMax !== null && min !== null) {
        if (min <= prevMax) rowErrs.minMark = 'Range overlaps previous range';
        else if (min > prevMax + 1) rowErrs.minMark = 'Gap in grading scale';
      }
      if (Object.keys(rowErrs).length) errs[row.id] = rowErrs;
      prevMax = max;
    });
    return errs;
  }, [ranges]);

  const onSubmit = useCallback(async (data: DialogSchemeValues) => {
    setCreating(true);
    setRangeError(null);
    let schemeId = createdSchemeId;
    try {
      const errs = validateCreateDialogRanges();
      if (Object.keys(errs).length) {
        setRangeErrors(errs);
        return;
      }

      if (schemeId === null) {
        const scheme = await createGradingScheme({
          name: data.name,
          description: data.description ?? undefined,
          isDefault: data.isDefault,
          status: data.status,
        });
        if (!Number.isFinite(scheme.id)) {
          throw new Error('Grading scheme was created but no ID was returned');
        }
        schemeId = scheme.id;
        setCreatedSchemeId(scheme.id);
      }

      const ordered = [...ranges].sort((a, b) => (a.minMark ?? -1) - (b.minMark ?? -1));
      for (let i = rangesAdded; i < ordered.length; i++) {
        setRangesAdded(i);
        await addGradingSchemeRange(schemeId, {
          grade: ordered[i].grade,
          minMark: ordered[i].minMark!,
          maxMark: ordered[i].maxMark!,
          gradePoint: ordered[i].gradePoint ?? undefined,
          remark: ordered[i].remark || undefined,
        });
      }
      onSuccess();
    } catch (err) {
      const failedAfterCreate = schemeId !== null;
      setRangeError(
        getApiErrorMessage(
          err,
          failedAfterCreate
            ? 'Grading scheme was created, but saving grade ranges failed'
            : 'Failed to create grading scheme',
        ),
      );
    } finally {
      setCreating(false);
    }
  }, [ranges, createdSchemeId, rangesAdded, onSuccess, validateCreateDialogRanges]);

  const retryRanges = useCallback(() => {
    setRangeError(null);
    onSubmit(getValues());
  }, [getValues, onSubmit]);

  const statusOptions = [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'INACTIVE', label: 'Inactive' },
  ];

  const hasRangeErrors = Object.keys(rangeErrors).length > 0;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <h2 className="text-lg font-semibold">Create Grading Scheme</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          {rangeError && (
            <Alert variant="destructive">
              <AlertTitle>Save failed</AlertTitle>
              <AlertDescription>{rangeError}</AlertDescription>
            </Alert>
          )}
          {rangeError && createdSchemeId !== null && rangesAdded < ranges.length && (
            <Button type="button" variant="outline" onClick={retryRanges} disabled={creating}>
              Retry remaining grade ranges
            </Button>
          )}

          <div className="space-y-4">
            <h3 className="text-lg font-medium">Scheme details</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Scheme name *</label>
                <Input placeholder="e.g. O Level Grading" {...register('name')} />
                {formErrors.name && <p className="text-sm text-destructive">{formErrors.name.message}</p>}
              </div>
              <div className="space-y-2 sm:col-span-2">
                <label className="text-sm font-medium">Description</label>
                <Textarea placeholder="Optional description" {...register('description')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Status</label>
                <Select options={statusOptions} {...register('status')} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium flex items-center gap-2">
                  <input type="checkbox" {...register('isDefault')} /> Default scheme
                </label>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <h3 className="text-lg font-medium">Grade ranges</h3>
            <p className="text-sm text-muted-foreground">
              Define the mark bands for each grade using actual marks. Ranges must start at 0, must not overlap,
              and must not leave gaps.
            </p>
            <div className="overflow-x-auto border rounded-md">
              <Table>
                <TableCaption>Grade ranges</TableCaption>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-20">Grade</TableHead>
                    <TableHead className="w-24">Minimum marks</TableHead>
                    <TableHead className="w-24">Maximum marks</TableHead>
                    <TableHead className="w-32">Grade point</TableHead>
                    <TableHead>Remark</TableHead>
                    <TableHead className="w-20" />
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {ranges.map((row) => (
                    <TableRow key={row.id}>
                      <TableCell>
                        <Input
                          value={row.grade}
                          onChange={(e) => updateRange(row.id, 'grade', e.target.value)}
                          placeholder="A"
                        />
                        {rangeErrors[row.id]?.grade && (
                          <p className="text-xs text-destructive mt-1">{rangeErrors[row.id].grade}</p>
                        )}
                      </TableCell>
                      <TableCell>
                        <Input
                          type="number"
                          value={row.minMark ?? ''}
                          onChange={(e) => updateRange(row.id, 'minMark', parseCreateDialogNum(e.target.value))}
                          placeholder="0"
                        />
                        {rangeErrors[row.id]?.minMark && (
                          <p className="text-xs text-destructive mt-1">{rangeErrors[row.id].minMark}</p>
                        )}
                      </TableCell>
                      <TableCell>
                        <Input
                          type="number"
                          value={row.maxMark ?? ''}
                          onChange={(e) => updateRange(row.id, 'maxMark', parseCreateDialogNum(e.target.value))}
                          placeholder="50"
                        />
                        {rangeErrors[row.id]?.maxMark && (
                          <p className="text-xs text-destructive mt-1">{rangeErrors[row.id].maxMark}</p>
                        )}
                      </TableCell>
                      <TableCell>
                        <Input
                          type="number"
                          step="any"
                          value={row.gradePoint ?? ''}
                          onChange={(e) => updateRange(row.id, 'gradePoint', parseCreateDialogNum(e.target.value))}
                          placeholder="4.0"
                        />
                      </TableCell>
                      <TableCell>
                        <Input
                          value={row.remark}
                          onChange={(e) => updateRange(row.id, 'remark', e.target.value)}
                          placeholder="Excellent"
                        />
                      </TableCell>
                      <TableCell>
                        <Button type="button" variant="outline" size="sm" onClick={() => removeGrade(row.id)}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
            <div className="flex gap-2">
              <Button type="button" variant="outline" size="sm" onClick={addGrade}>
                <Plus className="h-4 w-4 mr-2" /> Add Grade
              </Button>
              {ranges.length > 1 && (
                <Button type="button" variant="outline" size="sm" onClick={() => setRanges([])}>
                  Clear all
                </Button>
              )}
            </div>
            {hasRangeErrors && (
              <p className="text-xs text-destructive mt-2">
                Please fix the grade-range errors above before saving.
              </p>
            )}
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={creating}>
              Cancel
            </Button>
            <Button type="submit" disabled={creating}>
              {creating && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              Create Scheme
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function RangeFormDialog({
  open,
  schemeId,
  onOpenChange,
  onSuccess,
}: {
  open: boolean;
  schemeId: number;
  onOpenChange: (_open: boolean) => void;
  onSuccess: () => void;
}) {
  const { register, handleSubmit, reset } = useForm<RangeFormValues>({
    resolver: zodResolver(rangeSchema),
    defaultValues: { grade: '', minMark: 0, maxMark: 100, gradePoint: undefined, remark: '' },
  });

  const addMutation = useMutation({
    mutationFn: (data: AddGradingSchemeRangeRequest) => addGradingSchemeRange(schemeId, data),
    onSuccess: () => {
      reset({ grade: '', minMark: 0, maxMark: 100, gradePoint: undefined, remark: '' });
      onSuccess();
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogClose onClick={() => onOpenChange(false)} />
      <DialogContent>
        <h2 className="text-lg font-semibold">Add Grade Range</h2>
        <form
          onSubmit={handleSubmit((data) =>
            addMutation.mutate({
              grade: data.grade,
              minMark: data.minMark,
              maxMark: data.maxMark,
              gradePoint: data.gradePoint,
              remark: data.remark,
            }),
          )}
          className="space-y-4 mt-4"
        >
          <div className="grid gap-4 md:grid-cols-3">
            <Input {...register('grade')} label="Grade" placeholder="e.g. A" required />
            <Input {...register('minMark')} label="Min mark" type="number" required />
            <Input {...register('maxMark')} label="Max mark" type="number" required />
            <Input {...register('gradePoint')} label="Grade point" type="number" />
            <Input {...register('remark')} label="Remark" />
          </div>
          <p className="text-xs text-muted-foreground">
            Ranges must use actual marks (not percentages), start at 0, cover every mark up to the
            examination maximum without gaps, and must not overlap.
          </p>
          <div className="flex justify-end gap-2 mt-4">
            <Button variant="outline" type="button" onClick={() => onOpenChange(false)}>
              Close
            </Button>
            <Button type="submit" disabled={addMutation.isPending}>Add Range</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function EditRangeDialog({
  open,
  schemeId,
  range,
  onOpenChange,
  onSuccess,
}: {
  open: boolean;
  schemeId: number;
  range: GradingSchemeRange | null;
  onOpenChange: (_open: boolean) => void;
  onSuccess: () => void;
}) {
  const defaultValues = range
    ? {
        grade: range.grade,
        minMark: range.minMark,
        maxMark: range.maxMark,
        gradePoint: range.gradePoint ?? undefined,
        remark: range.remark ?? '',
      }
    : { grade: '', minMark: 0, maxMark: 100, gradePoint: undefined, remark: '' };

  const { register, handleSubmit, reset } = useForm<RangeFormValues>({
    resolver: zodResolver(rangeSchema),
    defaultValues,
  });

  const updateMutation = useMutation({
    mutationFn: (data: AddGradingSchemeRangeRequest) =>
      updateGradingSchemeRange(schemeId, range!.id, data),
    onSuccess: () => {
      reset(defaultValues);
      onSuccess();
    },
  });

  if (!range) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogClose onClick={() => onOpenChange(false)} />
      <DialogContent>
        <h2 className="text-lg font-semibold">Edit Grade Range</h2>
        <form
          onSubmit={handleSubmit((data) =>
            updateMutation.mutate({
              grade: data.grade,
              minMark: data.minMark,
              maxMark: data.maxMark,
              gradePoint: data.gradePoint,
              remark: data.remark,
            }),
          )}
          className="space-y-4 mt-4"
        >
          <div className="grid gap-4 md:grid-cols-3">
            <Input {...register('grade')} label="Grade" placeholder="e.g. A" required />
            <Input {...register('minMark')} label="Min mark" type="number" required />
            <Input {...register('maxMark')} label="Max mark" type="number" required />
            <Input {...register('gradePoint')} label="Grade point" type="number" />
            <Input {...register('remark')} label="Remark" />
          </div>
          <div className="flex justify-end gap-2 mt-4">
            <Button variant="outline" type="button" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={updateMutation.isPending}>Save Range</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
