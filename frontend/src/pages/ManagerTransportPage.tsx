import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  getVehicles, createVehicle, updateVehicle, deleteVehicle,
  getRoutes, createRoute, updateRoute, deleteRoute,
  getRouteStops, createRouteStop, updateRouteStop, deleteRouteStop,
  getTransportAssignments, createTransportAssignment, updateTransportAssignment, deleteTransportAssignment,
} from '@/services/transport';
import type {
  VehicleResponse, CreateVehicleRequest,
  RouteResponse, CreateRouteRequest,
  RouteStopResponse, CreateRouteStopRequest,
  TransportAssignmentResponse, CreateTransportAssignmentRequest,
} from '@/types/transport';
import type { PaginatedResponse } from '@/types/api';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/Table';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { DropdownMenu, DropdownMenuItem } from '@/components/ui/DropdownMenu';
import { Skeleton } from '@/components/ui/Skeleton';
import { Select } from '@/components/ui/Select';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/Alert';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/Tabs';
import { Plus, MoreVertical, Trash2 } from 'lucide-react';
import { Loader2 } from 'lucide-react';

const vehicleSchema = z.object({
  registrationNumber: z.string().min(1, 'Registration number is required'),
  vehicleType: z.string().min(1, 'Vehicle type is required'),
  capacity: z.number().min(1, 'Capacity must be at least 1'),
  driverId: z.number().optional(),
  driverName: z.string().optional(),
  driverPhone: z.string().optional(),
  insuranceNumber: z.string().optional(),
  insuranceExpiry: z.string().optional(),
  lastServiceDate: z.string().optional(),
  nextServiceDate: z.string().optional(),
  status: z.string().optional(),
  remarks: z.string().optional(),
});

const routeSchema = z.object({
  name: z.string().min(1, 'Route name is required'),
  code: z.string().optional(),
  startPoint: z.string().min(1, 'Start point is required'),
  endPoint: z.string().min(1, 'End point is required'),
  distanceKm: z.number().optional(),
  estimatedDurationMinutes: z.number().optional(),
  morningDeparture: z.string().optional(),
  afternoonDeparture: z.string().optional(),
  status: z.string().optional(),
  remarks: z.string().optional(),
});

const stopSchema = z.object({
  name: z.string().min(1, 'Stop name is required'),
  stopOrder: z.number().min(1, 'Stop order must be at least 1'),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  arrivalTime: z.string().optional(),
  departureTime: z.string().optional(),
  status: z.string().optional(),
  remarks: z.string().optional(),
});

const assignmentSchema = z.object({
  studentId: z.number().min(1, 'Student ID is required'),
  routeId: z.number().min(1, 'Route ID is required'),
  stopId: z.number().min(1, 'Stop ID is required'),
  vehicleId: z.number().min(1, 'Vehicle ID is required'),
  pickupPoint: z.string().optional(),
  dropPoint: z.string().optional(),
  academicYearId: z.number().optional(),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().optional(),
  feeApplicable: z.boolean().optional(),
  monthlyFee: z.number().optional(),
  status: z.string().optional(),
  remarks: z.string().optional(),
});

type VehicleFormValues = z.infer<typeof vehicleSchema>;
type RouteFormValues = z.infer<typeof routeSchema>;
type StopFormValues = z.infer<typeof stopSchema>;
type AssignmentFormValues = z.infer<typeof assignmentSchema>;

const vehicleStatusOptions = [
  { value: '', label: 'Select status' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'MAINTENANCE', label: 'Maintenance' },
];

const routeStatusOptions = [
  { value: '', label: 'Select status' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
];

const stopStatusOptions = [
  { value: '', label: 'Select status' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
];

const assignmentStatusOptions = [
  { value: '', label: 'Select status' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'COMPLETED', label: 'Completed' },
];

export function ManagerTransportPage() {
  const [activeTab, setActiveTab] = useState('vehicles');
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<VehicleResponse | RouteResponse | RouteStopResponse | TransportAssignmentResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [selectedRouteId, setSelectedRouteId] = useState<number | null>(null);
  const queryClient = useQueryClient();

  const vehicleForm = useForm<VehicleFormValues>({
    resolver: zodResolver(vehicleSchema),
    defaultValues: {
      registrationNumber: '',
      vehicleType: 'BUS',
      capacity: 0,
      driverId: undefined,
      driverName: '',
      driverPhone: '',
      insuranceNumber: '',
      insuranceExpiry: '',
      lastServiceDate: '',
      nextServiceDate: '',
      status: 'ACTIVE',
      remarks: '',
    },
  });

  const routeForm = useForm<RouteFormValues>({
    resolver: zodResolver(routeSchema),
    defaultValues: {
      name: '',
      code: '',
      startPoint: '',
      endPoint: '',
      distanceKm: undefined,
      estimatedDurationMinutes: undefined,
      morningDeparture: '',
      afternoonDeparture: '',
      status: 'ACTIVE',
      remarks: '',
    },
  });

  const stopForm = useForm<StopFormValues>({
    resolver: zodResolver(stopSchema),
    defaultValues: {
      name: '',
      stopOrder: 1,
      latitude: undefined,
      longitude: undefined,
      arrivalTime: '',
      departureTime: '',
      status: 'ACTIVE',
      remarks: '',
    },
  });

  const assignmentForm = useForm<AssignmentFormValues>({
    resolver: zodResolver(assignmentSchema),
    defaultValues: {
      studentId: 0,
      routeId: 0,
      stopId: 0,
      vehicleId: 0,
      pickupPoint: '',
      dropPoint: '',
      academicYearId: undefined,
      startDate: '',
      endDate: '',
      feeApplicable: false,
      monthlyFee: undefined,
      status: 'ACTIVE',
      remarks: '',
    },
  });

  const { data: vehiclesData, isLoading: vehiclesLoading } = useQuery({
    queryKey: ['vehicles', page],
    queryFn: async () => {
      const result = await getVehicles(page, 10);
      return result as PaginatedResponse<VehicleResponse>;
    },
  });

  const { data: routesData, isLoading: routesLoading } = useQuery({
    queryKey: ['routes', page],
    queryFn: async () => {
      const result = await getRoutes(page, 10);
      return result as PaginatedResponse<RouteResponse>;
    },
  });

  const { data: stopsData, isLoading: stopsLoading } = useQuery({
    queryKey: ['routeStops', selectedRouteId, page],
    queryFn: async () => {
      if (!selectedRouteId) return { content: [], totalElements: 0, totalPages: 0, page: 0, size: 10 };
      const result = await getRouteStops(selectedRouteId, page, 10);
      return result as PaginatedResponse<RouteStopResponse>;
    },
    enabled: !!selectedRouteId,
  });

  const { data: assignmentsData, isLoading: assignmentsLoading } = useQuery({
    queryKey: ['transportAssignments', page],
    queryFn: async () => {
      const result = await getTransportAssignments(page, 10);
      return result as PaginatedResponse<TransportAssignmentResponse>;
    },
  });

  const createVehicleMutation = useMutation({
    mutationFn: (data: CreateVehicleRequest) => createVehicle(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] });
      setOpen(false);
      setEditingItem(null);
      setFormError(null);
      vehicleForm.reset();
    },
  });

  const updateVehicleMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateVehicleRequest }) => updateVehicle(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] });
      setOpen(false);
      setEditingItem(null);
      setFormError(null);
      vehicleForm.reset();
    },
  });

  const deleteVehicleMutation = useMutation({
    mutationFn: (id: number) => deleteVehicle(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vehicles'] });
    },
  });

  const createRouteMutation = useMutation({
    mutationFn: (data: CreateRouteRequest) => createRoute(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes'] });
      setOpen(false);
      setEditingItem(null);
      setFormError(null);
      routeForm.reset();
    },
  });

  const updateRouteMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateRouteRequest }) => updateRoute(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes'] });
      setOpen(false);
      setEditingItem(null);
      setFormError(null);
      routeForm.reset();
    },
  });

  const deleteRouteMutation = useMutation({
    mutationFn: (id: number) => deleteRoute(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routes'] });
    },
  });

  const createStopMutation = useMutation({
    mutationFn: ({ routeId, data }: { routeId: number; data: CreateRouteStopRequest }) => createRouteStop(routeId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routeStops'] });
      setOpen(false);
      setEditingItem(null);
      setFormError(null);
      stopForm.reset();
    },
  });

  const updateStopMutation = useMutation({
    mutationFn: ({ routeId, stopId, data }: { routeId: number; stopId: number; data: CreateRouteStopRequest }) => updateRouteStop(routeId, stopId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routeStops'] });
      setOpen(false);
      setEditingItem(null);
      setFormError(null);
      stopForm.reset();
    },
  });

  const deleteStopMutation = useMutation({
    mutationFn: ({ routeId, stopId }: { routeId: number; stopId: number }) => deleteRouteStop(routeId, stopId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['routeStops'] });
    },
  });

  const createAssignmentMutation = useMutation({
    mutationFn: (data: CreateTransportAssignmentRequest) => createTransportAssignment(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transportAssignments'] });
      setOpen(false);
      setEditingItem(null);
      setFormError(null);
      assignmentForm.reset();
    },
  });

  const updateAssignmentMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateTransportAssignmentRequest }) => updateTransportAssignment(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transportAssignments'] });
      setOpen(false);
      setEditingItem(null);
      setFormError(null);
      assignmentForm.reset();
    },
  });

  const deleteAssignmentMutation = useMutation({
    mutationFn: (id: number) => deleteTransportAssignment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transportAssignments'] });
    },
  });

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (!newOpen) {
      setEditingItem(null);
      setFormError(null);
      vehicleForm.reset();
      routeForm.reset();
      stopForm.reset();
      assignmentForm.reset();
    }
  };

  const handleVehicleEdit = (vehicle: VehicleResponse) => {
    setEditingItem(vehicle);
    vehicleForm.reset({
      registrationNumber: vehicle.registrationNumber,
      vehicleType: vehicle.vehicleType,
      capacity: vehicle.capacity,
      driverId: vehicle.driverId || undefined,
      driverName: vehicle.driverName || '',
      driverPhone: vehicle.driverPhone || '',
      insuranceNumber: vehicle.insuranceNumber || '',
      insuranceExpiry: vehicle.insuranceExpiry || '',
      lastServiceDate: vehicle.lastServiceDate || '',
      nextServiceDate: vehicle.nextServiceDate || '',
      status: vehicle.status,
      remarks: vehicle.remarks || '',
    });
    setOpen(true);
  };

  const handleRouteEdit = (route: RouteResponse) => {
    setEditingItem(route);
    routeForm.reset({
      name: route.name,
      code: route.code || '',
      startPoint: route.startPoint,
      endPoint: route.endPoint,
      distanceKm: route.distanceKm || undefined,
      estimatedDurationMinutes: route.estimatedDurationMinutes || undefined,
      morningDeparture: route.morningDeparture || '',
      afternoonDeparture: route.afternoonDeparture || '',
      status: route.status,
      remarks: route.remarks || '',
    });
    setOpen(true);
  };

  const handleStopEdit = (stop: RouteStopResponse) => {
    setEditingItem(stop);
    stopForm.reset({
      name: stop.name,
      stopOrder: stop.stopOrder,
      latitude: stop.latitude || undefined,
      longitude: stop.longitude || undefined,
      arrivalTime: stop.arrivalTime || '',
      departureTime: stop.departureTime || '',
      status: stop.status,
      remarks: stop.remarks || '',
    });
    setOpen(true);
  };

  const handleAssignmentEdit = (assignment: TransportAssignmentResponse) => {
    setEditingItem(assignment);
    assignmentForm.reset({
      studentId: assignment.studentId,
      routeId: assignment.routeId,
      stopId: assignment.stopId,
      vehicleId: assignment.vehicleId,
      pickupPoint: assignment.pickupPoint || '',
      dropPoint: assignment.dropPoint || '',
      academicYearId: assignment.academicYearId || undefined,
      startDate: assignment.startDate,
      endDate: assignment.endDate || '',
      feeApplicable: assignment.feeApplicable,
      monthlyFee: assignment.monthlyFee || undefined,
      status: assignment.status,
      remarks: assignment.remarks || '',
    });
    setOpen(true);
  };

  const onSubmitVehicle = async (values: VehicleFormValues) => {
    setFormError(null);
    try {
      if (editingItem && 'registrationNumber' in editingItem) {
        await updateVehicleMutation.mutateAsync({ id: editingItem.id, data: values as CreateVehicleRequest });
      } else {
        await createVehicleMutation.mutateAsync(values as CreateVehicleRequest);
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const onSubmitRoute = async (values: RouteFormValues) => {
    setFormError(null);
    try {
      if (editingItem && 'name' in editingItem && 'startPoint' in editingItem) {
        await updateRouteMutation.mutateAsync({ id: editingItem.id, data: values as CreateRouteRequest });
      } else {
        await createRouteMutation.mutateAsync(values as CreateRouteRequest);
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const onSubmitStop = async (values: StopFormValues) => {
    setFormError(null);
    try {
      if (selectedRouteId) {
        if (editingItem && 'name' in editingItem) {
          await updateStopMutation.mutateAsync({ routeId: selectedRouteId, stopId: editingItem.id, data: values as CreateRouteStopRequest });
        } else {
          await createStopMutation.mutateAsync({ routeId: selectedRouteId, data: values as CreateRouteStopRequest });
        }
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const onSubmitAssignment = async (values: AssignmentFormValues) => {
    setFormError(null);
    try {
      if (editingItem && 'studentId' in editingItem) {
        await updateAssignmentMutation.mutateAsync({ id: editingItem.id, data: values as CreateTransportAssignmentRequest });
      } else {
        await createAssignmentMutation.mutateAsync(values as CreateTransportAssignmentRequest);
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Operation failed');
    }
  };

  const getVehicleStatusVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'INACTIVE': return 'destructive';
      case 'MAINTENANCE': return 'outline';
      default: return 'default';
    }
  };

  const getRouteStatusVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'INACTIVE': return 'destructive';
      default: return 'default';
    }
  };

  const getStopStatusVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'INACTIVE': return 'destructive';
      default: return 'default';
    }
  };

  const getAssignmentStatusVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'INACTIVE': return 'destructive';
      case 'COMPLETED': return 'default';
      default: return 'default';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Transport Management</h1>
          <p className="mt-1 text-muted-foreground">Manage vehicles, routes, stops, and student transport assignments</p>
        </div>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} defaultValue="vehicles">
        <TabsList>
          <TabsTrigger value="vehicles">Vehicles</TabsTrigger>
          <TabsTrigger value="routes">Routes</TabsTrigger>
          <TabsTrigger value="stops">Stops</TabsTrigger>
          <TabsTrigger value="assignments">Assignments</TabsTrigger>
        </TabsList>

        <TabsContent value="vehicles">
          <Card className="p-4">
            <div className="flex justify-end mb-4">
              <Button onClick={() => { setEditingItem(null); vehicleForm.reset(); setOpen(true); }}>
                <Plus className="mr-2 h-4 w-4" /> Add Vehicle
              </Button>
            </div>
            {vehiclesLoading ? (
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <Skeleton key={i} className="h-12 w-full" />
                ))}
              </div>
            ) : (
              <>
                <div className="rounded-lg border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Registration</TableHead>
                        <TableHead>Type</TableHead>
                        <TableHead>Capacity</TableHead>
                        <TableHead>Driver</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="w-12" />
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {vehiclesData?.content?.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={6} className="text-center text-muted-foreground">
                            No vehicles found
                          </TableCell>
                        </TableRow>
                      )}
                      {vehiclesData?.content?.map((vehicle: any) => (
                        <TableRow key={vehicle.id}>
                          <TableCell className="font-medium">{vehicle.registrationNumber}</TableCell>
                          <TableCell>{vehicle.vehicleType}</TableCell>
                          <TableCell>{vehicle.capacity}</TableCell>
                          <TableCell>{vehicle.driverName || '-'}</TableCell>
                          <TableCell>
                            <Badge variant={getVehicleStatusVariant(vehicle.status)}>
                              {vehicle.status}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <DropdownMenu
                              trigger={
                                <button className="rounded-full p-1 hover:bg-muted">
                                  <MoreVertical className="h-4 w-4" />
                                </button>
                              }
                            >
                              <DropdownMenuItem onClick={() => handleVehicleEdit(vehicle)}>
                                Edit
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => deleteVehicleMutation.mutate(vehicle.id)} className="text-red-600">
                                <Trash2 className="h-4 w-4 mr-2" /> Delete
                              </DropdownMenuItem>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {vehiclesData?.totalPages || 1}
                  </p>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                      Previous
                    </Button>
                    <Button variant="outline" size="sm" disabled={page >= (vehiclesData?.totalPages || 1) - 1} onClick={() => setPage(p => p + 1)}>
                      Next
                    </Button>
                  </div>
                </div>
              </>
            )}
          </Card>
        </TabsContent>

        <TabsContent value="routes">
          <Card className="p-4">
            <div className="flex justify-end mb-4">
              <Button onClick={() => { setEditingItem(null); routeForm.reset(); setOpen(true); }}>
                <Plus className="mr-2 h-4 w-4" /> Add Route
              </Button>
            </div>
            {routesLoading ? (
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <Skeleton key={i} className="h-12 w-full" />
                ))}
              </div>
            ) : (
              <>
                <div className="rounded-lg border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Name</TableHead>
                        <TableHead>Code</TableHead>
                        <TableHead>Start</TableHead>
                        <TableHead>End</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="w-12" />
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {routesData?.content?.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={6} className="text-center text-muted-foreground">
                            No routes found
                          </TableCell>
                        </TableRow>
                      )}
                      {routesData?.content?.map((route: any) => (
                        <TableRow key={route.id}>
                          <TableCell className="font-medium">{route.name}</TableCell>
                          <TableCell>{route.code || '-'}</TableCell>
                          <TableCell>{route.startPoint}</TableCell>
                          <TableCell>{route.endPoint}</TableCell>
                          <TableCell>
                            <Badge variant={getRouteStatusVariant(route.status)}>
                              {route.status}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <DropdownMenu
                              trigger={
                                <button className="rounded-full p-1 hover:bg-muted">
                                  <MoreVertical className="h-4 w-4" />
                                </button>
                              }
                            >
                              <DropdownMenuItem onClick={() => handleRouteEdit(route)}>
                                Edit
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => deleteRouteMutation.mutate(route.id)} className="text-red-600">
                                <Trash2 className="h-4 w-4 mr-2" /> Delete
                              </DropdownMenuItem>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {routesData?.totalPages || 1}
                  </p>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                      Previous
                    </Button>
                    <Button variant="outline" size="sm" disabled={page >= (routesData?.totalPages || 1) - 1} onClick={() => setPage(p => p + 1)}>
                      Next
                    </Button>
                  </div>
                </div>
              </>
            )}
          </Card>
        </TabsContent>

        <TabsContent value="stops">
          <Card className="p-4">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between mb-4">
              <div className="flex items-end gap-2">
                <div>
                  <label className="text-sm font-medium block mb-1">Select Route</label>
                  <Select
                    value={selectedRouteId?.toString() || ''}
                    onChange={(e) => {
                      const val = e.target.value ? Number(e.target.value) : null;
                      setSelectedRouteId(val);
                      setPage(0);
                    }}
                    options={[
                      { value: '', label: 'Select a route' },
                      ...(routesData?.content?.map((r: any) => ({ value: r.id.toString(), label: r.name })) || []),
                    ]}
                  />
                </div>
              </div>
              {selectedRouteId && (
                <Button onClick={() => { setEditingItem(null); stopForm.reset(); setOpen(true); }}>
                  <Plus className="mr-2 h-4 w-4" /> Add Stop
                </Button>
              )}
            </div>
            {!selectedRouteId ? (
              <p className="text-center text-muted-foreground py-8">Please select a route to view stops</p>
            ) : stopsLoading ? (
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <Skeleton key={i} className="h-12 w-full" />
                ))}
              </div>
            ) : (
              <>
                <div className="rounded-lg border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Order</TableHead>
                        <TableHead>Name</TableHead>
                        <TableHead>Arrival</TableHead>
                        <TableHead>Departure</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="w-12" />
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {stopsData?.content?.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={6} className="text-center text-muted-foreground">
                            No stops found for this route
                          </TableCell>
                        </TableRow>
                      )}
                      {stopsData?.content?.map((stop: any) => (
                        <TableRow key={stop.id}>
                          <TableCell>{stop.stopOrder}</TableCell>
                          <TableCell className="font-medium">{stop.name}</TableCell>
                          <TableCell>{stop.arrivalTime || '-'}</TableCell>
                          <TableCell>{stop.departureTime || '-'}</TableCell>
                          <TableCell>
                            <Badge variant={getStopStatusVariant(stop.status)}>
                              {stop.status}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <DropdownMenu
                              trigger={
                                <button className="rounded-full p-1 hover:bg-muted">
                                  <MoreVertical className="h-4 w-4" />
                                </button>
                              }
                            >
                              <DropdownMenuItem onClick={() => handleStopEdit(stop)}>
                                Edit
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => deleteStopMutation.mutate({ routeId: selectedRouteId, stopId: stop.id })} className="text-red-600">
                                <Trash2 className="h-4 w-4 mr-2" /> Delete
                              </DropdownMenuItem>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {stopsData?.totalPages || 1}
                  </p>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                      Previous
                    </Button>
                    <Button variant="outline" size="sm" disabled={page >= (stopsData?.totalPages || 1) - 1} onClick={() => setPage(p => p + 1)}>
                      Next
                    </Button>
                  </div>
                </div>
              </>
            )}
          </Card>
        </TabsContent>

        <TabsContent value="assignments">
          <Card className="p-4">
            <div className="flex justify-end mb-4">
              <Button onClick={() => { setEditingItem(null); assignmentForm.reset(); setOpen(true); }}>
                <Plus className="mr-2 h-4 w-4" /> Add Assignment
              </Button>
            </div>
            {assignmentsLoading ? (
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <Skeleton key={i} className="h-12 w-full" />
                ))}
              </div>
            ) : (
              <>
                <div className="rounded-lg border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Student</TableHead>
                        <TableHead>Route</TableHead>
                        <TableHead>Stop</TableHead>
                        <TableHead>Vehicle</TableHead>
                        <TableHead>Start Date</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="w-12" />
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {assignmentsData?.content?.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={7} className="text-center text-muted-foreground">
                            No transport assignments found
                          </TableCell>
                        </TableRow>
                      )}
                      {assignmentsData?.content?.map((assignment: any) => (
                        <TableRow key={assignment.id}>
                          <TableCell className="font-medium">{assignment.studentName || assignment.studentId}</TableCell>
                          <TableCell>{assignment.routeName || assignment.routeId}</TableCell>
                          <TableCell>{assignment.stopName || assignment.stopId}</TableCell>
                          <TableCell>{assignment.vehicleRegistration || assignment.vehicleId}</TableCell>
                          <TableCell>{assignment.startDate}</TableCell>
                          <TableCell>
                            <Badge variant={getAssignmentStatusVariant(assignment.status)}>
                              {assignment.status}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <DropdownMenu
                              trigger={
                                <button className="rounded-full p-1 hover:bg-muted">
                                  <MoreVertical className="h-4 w-4" />
                                </button>
                              }
                            >
                              <DropdownMenuItem onClick={() => handleAssignmentEdit(assignment)}>
                                Edit
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => deleteAssignmentMutation.mutate(assignment.id)} className="text-red-600">
                                <Trash2 className="h-4 w-4 mr-2" /> Delete
                              </DropdownMenuItem>
                            </DropdownMenu>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {assignmentsData?.totalPages || 1}
                  </p>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                      Previous
                    </Button>
                    <Button variant="outline" size="sm" disabled={page >= (assignmentsData?.totalPages || 1) - 1} onClick={() => setPage(p => p + 1)}>
                      Next
                    </Button>
                  </div>
                </div>
              </>
            )}
          </Card>
        </TabsContent>
      </Tabs>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent
          title={activeTab === 'vehicles' ? (editingItem && 'registrationNumber' in editingItem ? 'Edit Vehicle' : 'Add Vehicle') :
                 activeTab === 'routes' ? (editingItem && 'name' in editingItem && 'startPoint' in editingItem ? 'Edit Route' : 'Add Route') :
                 activeTab === 'stops' ? (editingItem && 'name' in editingItem && 'stopOrder' in editingItem ? 'Edit Stop' : 'Add Stop') :
                 (editingItem && 'studentId' in editingItem ? 'Edit Assignment' : 'Add Assignment')}
          description={activeTab === 'vehicles' ? (editingItem ? 'Update vehicle information' : 'Register a new vehicle') :
                       activeTab === 'routes' ? (editingItem ? 'Update route information' : 'Create a new route') :
                       activeTab === 'stops' ? (editingItem ? 'Update stop information' : 'Add a new stop') :
                       (editingItem ? 'Update assignment information' : 'Create a new transport assignment')}
        >
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          {activeTab === 'vehicles' && (
            <form onSubmit={vehicleForm.handleSubmit(onSubmitVehicle)} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Registration Number *</label>
                  <Input {...vehicleForm.register('registrationNumber')} />
                  {vehicleForm.formState.errors.registrationNumber && <p className="text-sm text-destructive">{vehicleForm.formState.errors.registrationNumber.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Vehicle Type *</label>
                  <Select {...vehicleForm.register('vehicleType')} options={[
                    { value: 'BUS', label: 'Bus' },
                    { value: 'VAN', label: 'Van' },
                    { value: 'CAR', label: 'Car' },
                  ]} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Capacity *</label>
                  <Input type="number" {...vehicleForm.register('capacity', { valueAsNumber: true })} />
                  {vehicleForm.formState.errors.capacity && <p className="text-sm text-destructive">{vehicleForm.formState.errors.capacity.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Driver Name</label>
                  <Input {...vehicleForm.register('driverName')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Driver Phone</label>
                  <Input {...vehicleForm.register('driverPhone')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Insurance Number</label>
                  <Input {...vehicleForm.register('insuranceNumber')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Insurance Expiry</label>
                  <Input type="date" {...vehicleForm.register('insuranceExpiry')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Last Service Date</label>
                  <Input type="date" {...vehicleForm.register('lastServiceDate')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Next Service Date</label>
                  <Input type="date" {...vehicleForm.register('nextServiceDate')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Status</label>
                  <Select {...vehicleForm.register('status')} options={vehicleStatusOptions} />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <label className="text-sm font-medium">Remarks</label>
                  <Input {...vehicleForm.register('remarks')} />
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
                <Button type="submit" disabled={createVehicleMutation.isPending || updateVehicleMutation.isPending}>
                  {(createVehicleMutation.isPending || updateVehicleMutation.isPending) && (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  )}
                  {editingItem && 'registrationNumber' in editingItem ? 'Update Vehicle' : 'Create Vehicle'}
                </Button>
              </div>
            </form>
          )}
          {activeTab === 'routes' && (
            <form onSubmit={routeForm.handleSubmit(onSubmitRoute)} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Route Name *</label>
                  <Input {...routeForm.register('name')} />
                  {routeForm.formState.errors.name && <p className="text-sm text-destructive">{routeForm.formState.errors.name.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Code</label>
                  <Input {...routeForm.register('code')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Start Point *</label>
                  <Input {...routeForm.register('startPoint')} />
                  {routeForm.formState.errors.startPoint && <p className="text-sm text-destructive">{routeForm.formState.errors.startPoint.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">End Point *</label>
                  <Input {...routeForm.register('endPoint')} />
                  {routeForm.formState.errors.endPoint && <p className="text-sm text-destructive">{routeForm.formState.errors.endPoint.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Distance (km)</label>
                  <Input type="number" step="0.1" {...routeForm.register('distanceKm', { valueAsNumber: true })} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Duration (minutes)</label>
                  <Input type="number" {...routeForm.register('estimatedDurationMinutes', { valueAsNumber: true })} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Morning Departure</label>
                  <Input type="time" {...routeForm.register('morningDeparture')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Afternoon Departure</label>
                  <Input type="time" {...routeForm.register('afternoonDeparture')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Status</label>
                  <Select {...routeForm.register('status')} options={routeStatusOptions} />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <label className="text-sm font-medium">Remarks</label>
                  <Input {...routeForm.register('remarks')} />
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
                <Button type="submit" disabled={createRouteMutation.isPending || updateRouteMutation.isPending}>
                  {(createRouteMutation.isPending || updateRouteMutation.isPending) && (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  )}
                  {editingItem && 'name' in editingItem && 'startPoint' in editingItem ? 'Update Route' : 'Create Route'}
                </Button>
              </div>
            </form>
          )}
          {activeTab === 'stops' && (
            <form onSubmit={stopForm.handleSubmit(onSubmitStop)} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Stop Name *</label>
                  <Input {...stopForm.register('name')} />
                  {stopForm.formState.errors.name && <p className="text-sm text-destructive">{stopForm.formState.errors.name.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Stop Order *</label>
                  <Input type="number" {...stopForm.register('stopOrder', { valueAsNumber: true })} />
                  {stopForm.formState.errors.stopOrder && <p className="text-sm text-destructive">{stopForm.formState.errors.stopOrder.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Latitude</label>
                  <Input type="number" step="any" {...stopForm.register('latitude', { valueAsNumber: true })} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Longitude</label>
                  <Input type="number" step="any" {...stopForm.register('longitude', { valueAsNumber: true })} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Arrival Time</label>
                  <Input type="time" {...stopForm.register('arrivalTime')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Departure Time</label>
                  <Input type="time" {...stopForm.register('departureTime')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Status</label>
                  <Select {...stopForm.register('status')} options={stopStatusOptions} />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <label className="text-sm font-medium">Remarks</label>
                  <Input {...stopForm.register('remarks')} />
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
                <Button type="submit" disabled={createStopMutation.isPending || updateStopMutation.isPending}>
                  {(createStopMutation.isPending || updateStopMutation.isPending) && (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  )}
                  {editingItem && 'name' in editingItem && 'stopOrder' in editingItem ? 'Update Stop' : 'Create Stop'}
                </Button>
              </div>
            </form>
          )}
          {activeTab === 'assignments' && (
            <form onSubmit={assignmentForm.handleSubmit(onSubmitAssignment)} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Student ID *</label>
                  <Input type="number" {...assignmentForm.register('studentId', { valueAsNumber: true })} />
                  {assignmentForm.formState.errors.studentId && <p className="text-sm text-destructive">{assignmentForm.formState.errors.studentId.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Route ID *</label>
                  <Input type="number" {...assignmentForm.register('routeId', { valueAsNumber: true })} />
                  {assignmentForm.formState.errors.routeId && <p className="text-sm text-destructive">{assignmentForm.formState.errors.routeId.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Stop ID *</label>
                  <Input type="number" {...assignmentForm.register('stopId', { valueAsNumber: true })} />
                  {assignmentForm.formState.errors.stopId && <p className="text-sm text-destructive">{assignmentForm.formState.errors.stopId.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Vehicle ID *</label>
                  <Input type="number" {...assignmentForm.register('vehicleId', { valueAsNumber: true })} />
                  {assignmentForm.formState.errors.vehicleId && <p className="text-sm text-destructive">{assignmentForm.formState.errors.vehicleId.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Pickup Point</label>
                  <Input {...assignmentForm.register('pickupPoint')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Drop Point</label>
                  <Input {...assignmentForm.register('dropPoint')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Academic Year ID</label>
                  <Input type="number" {...assignmentForm.register('academicYearId', { valueAsNumber: true })} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Start Date *</label>
                  <Input type="date" {...assignmentForm.register('startDate')} />
                  {assignmentForm.formState.errors.startDate && <p className="text-sm text-destructive">{assignmentForm.formState.errors.startDate.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">End Date</label>
                  <Input type="date" {...assignmentForm.register('endDate')} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Monthly Fee</label>
                  <Input type="number" step="0.01" {...assignmentForm.register('monthlyFee', { valueAsNumber: true })} />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Status</label>
                  <Select {...assignmentForm.register('status')} options={assignmentStatusOptions} />
                </div>
                <div className="space-y-2 md:col-span-2">
                  <label className="text-sm font-medium">Remarks</label>
                  <Input {...assignmentForm.register('remarks')} />
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>Cancel</Button>
                <Button type="submit" disabled={createAssignmentMutation.isPending || updateAssignmentMutation.isPending}>
                  {(createAssignmentMutation.isPending || updateAssignmentMutation.isPending) && (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  )}
                  {editingItem && 'studentId' in editingItem ? 'Update Assignment' : 'Create Assignment'}
                </Button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

