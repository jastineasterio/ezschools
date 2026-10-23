import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  getDormitories,
  createDormitory,
  updateDormitory,
  deleteDormitory,
  createRoom,
  updateRoom,
  deleteRoom,
  createBed,
  updateBed,
  deleteBed,
  getBoardingStudents,
  allocateBoardingStudent,
  releaseBoardingStudent,
  createBoardingAttendance,
  updateBoardingAttendance,
  deleteBoardingAttendance,
  getRoomsByDormitory,
  getBedsByRoom,
  getActiveBoardingStudents,
  getBoardingAttendanceByDormitoryAndDate,
} from '@/services/boarding';
import type {
  DormitoryResponse,
  CreateDormitoryRequest,
  RoomResponse,
  CreateRoomRequest,
  BedResponse,
  CreateBedRequest,
  BoardingStudentResponse,
  BoardingAttendanceResponse,
  CreateBoardingAttendanceRequest,
} from '@/types/boarding';
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
import { getApiErrorMessage } from '@/services/api';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/Tabs';
import {
  Plus,
  MoreVertical,
  Trash2,
  UserPlus,
  UserX,
  Loader2,
} from 'lucide-react';

const dormitorySchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
  capacity: z.coerce.number().min(0).optional(),
  gender: z.string().optional(),
  wardenId: z.coerce.number().optional(),
  status: z.string().optional(),
});

const roomSchema = z.object({
  dormitoryId: z.coerce.number().min(1, 'Dormitory is required'),
  roomNumber: z.string().min(1, 'Room number is required'),
  capacity: z.coerce.number().min(1).optional(),
  status: z.string().optional(),
});

const bedSchema = z.object({
  roomId: z.coerce.number().min(1, 'Room is required'),
  bedNumber: z.string().min(1, 'Bed number is required'),
  status: z.string().optional(),
});

const allocateSchema = z.object({
  studentId: z.coerce.number().min(1, 'Student ID is required'),
  dormitoryId: z.coerce.number().min(1, 'Dormitory is required'),
  roomId: z.coerce.number().min(1, 'Room is required'),
  bedId: z.coerce.number().min(1, 'Bed is required'),
  status: z.string().optional(),
});

const attendanceSchema = z.object({
  studentId: z.coerce.number().min(1, 'Student ID is required'),
  dormitoryId: z.coerce.number().min(1, 'Dormitory is required'),
  attendanceDate: z.string().min(1, 'Date is required'),
  status: z.string().min(1, 'Status is required'),
  remarks: z.string().optional(),
});

type DormitoryFormValues = z.infer<typeof dormitorySchema>;
type RoomFormValues = z.infer<typeof roomSchema>;
type BedFormValues = z.infer<typeof bedSchema>;
type AllocateFormValues = z.infer<typeof allocateSchema>;
type AttendanceFormValues = z.infer<typeof attendanceSchema>;

const genderOptions = [
  { value: '', label: 'Any' },
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
];

const attendanceStatusOptions = [
  { value: 'PRESENT', label: 'Present' },
  { value: 'ABSENT', label: 'Absent' },
  { value: 'LATE', label: 'Late' },
  { value: 'EXCUSED', label: 'Excused' },
];

export function ManagerBoardingPage() {
  const [activeTab, setActiveTab] = useState('dormitories');
  const queryClient = useQueryClient();
  const [dormOpen, setDormOpen] = useState(false);
  const [roomOpen, setRoomOpen] = useState(false);
  const [bedOpen, setBedOpen] = useState(false);
  const [allocateOpen, setAllocateOpen] = useState(false);
  const [attendanceOpen, setAttendanceOpen] = useState(false);
  const [editingDorm, setEditingDorm] = useState<DormitoryResponse | null>(null);
  const [editingRoom, setEditingRoom] = useState<RoomResponse | null>(null);
  const [editingBed, setEditingBed] = useState<BedResponse | null>(null);
  const [editingAttendance, setEditingAttendance] = useState<BoardingAttendanceResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [selectedDormitory, setSelectedDormitory] = useState<number | null>(null);
  const [selectedRoom, setSelectedRoom] = useState<number | null>(null);
  const [attendanceDate, setAttendanceDate] = useState<string>('');
  const [boardingSearch, setBoardingSearch] = useState('');
  const handleRequestError = (error: unknown) => {
    setFormError(getApiErrorMessage(error, 'Boarding request failed'));
  };

  const { data: dormitories, isLoading: dormLoading, error: dormError } = useQuery({
    queryKey: ['boarding-dormitories'],
    queryFn: async () => {
      const result = await getDormitories(0, 50);
      return result.data;
    },
  });

  const { data: rooms, isLoading: roomLoading, error: roomError } = useQuery({
    queryKey: ['boarding-rooms', selectedDormitory],
    queryFn: async () => {
      if (!selectedDormitory) return [];
      return getRoomsByDormitory(selectedDormitory);
    },
    enabled: !!selectedDormitory,
  });

  const { data: beds, isLoading: bedLoading, error: bedError } = useQuery({
    queryKey: ['boarding-beds', selectedRoom],
    queryFn: async () => {
      if (!selectedRoom) return [];
      return getBedsByRoom(selectedRoom);
    },
    enabled: !!selectedRoom,
  });

  const { data: boardingStudents, isLoading: studentsLoading, error: studentsError } = useQuery({
    queryKey: ['boarding-students'],
    queryFn: async () => {
      const result = await getBoardingStudents(0, 50);
      return result.data;
    },
  });

  const { error: activeStudentsError } = useQuery({
    queryKey: ['active-boarding-students'],
    queryFn: async () => {
      return getActiveBoardingStudents();
    },
  });

  const { data: attendance, isLoading: attendanceLoading, error: attendanceError } = useQuery({
    queryKey: ['boarding-attendance', selectedDormitory, attendanceDate],
    queryFn: async () => {
      if (!selectedDormitory || !attendanceDate) return [];
      return getBoardingAttendanceByDormitoryAndDate(selectedDormitory, attendanceDate);
    },
    enabled: !!selectedDormitory && !!attendanceDate,
  });

  const createDormMutation = useMutation({
    mutationFn: createDormitory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-dormitories'] });
      setFormError(null);
      setDormOpen(false);
      setEditingDorm(null);
    },
    onError: handleRequestError,
  });

  const updateDormMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateDormitoryRequest }) => updateDormitory(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-dormitories'] });
      setFormError(null);
      setDormOpen(false);
      setEditingDorm(null);
      setFormError(null);
    },
    onError: handleRequestError,
  });

  const deleteDormMutation = useMutation({
    mutationFn: deleteDormitory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-dormitories'] });
    },
    onError: handleRequestError,
  });

  const createRoomMutation = useMutation({
    mutationFn: createRoom,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-rooms'] });
      setFormError(null);
      setRoomOpen(false);
      setEditingRoom(null);
    },
    onError: handleRequestError,
  });

  const updateRoomMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateRoomRequest }) => updateRoom(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-rooms'] });
      setFormError(null);
      setRoomOpen(false);
      setEditingRoom(null);
      setFormError(null);
    },
    onError: handleRequestError,
  });

  const deleteRoomMutation = useMutation({
    mutationFn: deleteRoom,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-rooms'] });
    },
    onError: handleRequestError,
  });

  const createBedMutation = useMutation({
    mutationFn: createBed,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-beds'] });
      setFormError(null);
      setBedOpen(false);
      setEditingBed(null);
    },
    onError: handleRequestError,
  });

  const updateBedMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateBedRequest }) => updateBed(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-beds'] });
      setFormError(null);
      setBedOpen(false);
      setEditingBed(null);
      setFormError(null);
    },
    onError: handleRequestError,
  });

  const deleteBedMutation = useMutation({
    mutationFn: deleteBed,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-beds'] });
    },
    onError: handleRequestError,
  });

  const allocateMutation = useMutation({
    mutationFn: allocateBoardingStudent,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-students'] });
      queryClient.invalidateQueries({ queryKey: ['active-boarding-students'] });
      queryClient.invalidateQueries({ queryKey: ['boarding-beds'] });
      queryClient.invalidateQueries({ queryKey: ['boarding-rooms'] });
      setFormError(null);
      setAllocateOpen(false);
    },
    onError: handleRequestError,
  });

  const releaseMutation = useMutation({
    mutationFn: releaseBoardingStudent,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-students'] });
      queryClient.invalidateQueries({ queryKey: ['active-boarding-students'] });
      queryClient.invalidateQueries({ queryKey: ['boarding-beds'] });
      queryClient.invalidateQueries({ queryKey: ['boarding-rooms'] });
      setFormError(null);
    },
    onError: handleRequestError,
  });

  const createAttendanceMutation = useMutation({
    mutationFn: createBoardingAttendance,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-attendance'] });
      setFormError(null);
      setAttendanceOpen(false);
      setEditingAttendance(null);
    },
    onError: handleRequestError,
  });

  const updateAttendanceMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateBoardingAttendanceRequest }) => updateBoardingAttendance(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-attendance'] });
      setFormError(null);
      setAttendanceOpen(false);
      setEditingAttendance(null);
      setFormError(null);
    },
    onError: handleRequestError,
  });

  const deleteAttendanceMutation = useMutation({
    mutationFn: deleteBoardingAttendance,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boarding-attendance'] });
    },
    onError: handleRequestError,
  });

  const {
    register: registerDorm,
    handleSubmit: handleSubmitDorm,
    reset: resetDorm,
    formState: { errors: dormErrors },
  } = useForm<DormitoryFormValues>({
    resolver: zodResolver(dormitorySchema),
    defaultValues: { name: '', description: '', capacity: 0, gender: '', wardenId: undefined, status: 'ACTIVE' },
  });

  const {
    register: registerRoom,
    handleSubmit: handleSubmitRoom,
    reset: resetRoom,
    formState: { errors: roomErrors },
  } = useForm<RoomFormValues>({
    resolver: zodResolver(roomSchema),
    defaultValues: { dormitoryId: 0, roomNumber: '', capacity: 1, status: 'ACTIVE' },
  });

  const {
    register: registerBed,
    handleSubmit: handleSubmitBed,
    reset: resetBed,
    formState: { errors: bedErrors },
  } = useForm<BedFormValues>({
    resolver: zodResolver(bedSchema),
    defaultValues: { roomId: 0, bedNumber: '', status: 'AVAILABLE' },
  });

  const {
    register: registerAllocate,
    handleSubmit: handleSubmitAllocate,
    reset: resetAllocate,
    formState: { errors: allocateErrors },
  } = useForm<AllocateFormValues>({
    resolver: zodResolver(allocateSchema),
    defaultValues: { studentId: 0, dormitoryId: 0, roomId: 0, bedId: 0, status: 'ACTIVE' },
  });

  const {
    register: registerAttendance,
    handleSubmit: handleSubmitAttendance,
    reset: resetAttendance,
    formState: { errors: attendanceErrors },
  } = useForm<AttendanceFormValues>({
    resolver: zodResolver(attendanceSchema),
    defaultValues: { studentId: 0, dormitoryId: 0, attendanceDate: '', status: 'PRESENT', remarks: '' },
  });

  const handleDormSubmit = (values: DormitoryFormValues) => {
    setFormError(null);
    if (editingDorm) {
      updateDormMutation.mutate({ id: editingDorm.id, data: values });
    } else {
      createDormMutation.mutate(values);
    }
  };

  const handleRoomSubmit = (values: RoomFormValues) => {
    setFormError(null);
    if (editingRoom) {
      updateRoomMutation.mutate({ id: editingRoom.id, data: values });
    } else {
      createRoomMutation.mutate(values);
    }
  };

  const handleBedSubmit = (values: BedFormValues) => {
    setFormError(null);
    if (editingBed) {
      updateBedMutation.mutate({ id: editingBed.id, data: values });
    } else {
      createBedMutation.mutate(values);
    }
  };

  const handleAllocateSubmit = (values: AllocateFormValues) => {
    setFormError(null);
    allocateMutation.mutate(values);
  };

  const handleAttendanceSubmit = (values: AttendanceFormValues) => {
    setFormError(null);
    if (editingAttendance) {
      updateAttendanceMutation.mutate({ id: editingAttendance.id, data: values });
    } else {
      createAttendanceMutation.mutate(values);
    }
  };

  const openDormDialog = (dorm?: DormitoryResponse) => {
    if (dorm) {
      setEditingDorm(dorm);
      resetDorm({
        name: dorm.name,
        description: dorm.description || '',
        capacity: dorm.capacity,
        gender: dorm.gender || '',
        wardenId: dorm.wardenId || undefined,
        status: dorm.status,
      });
    } else {
      setEditingDorm(null);
      resetDorm({ name: '', description: '', capacity: 0, gender: '', wardenId: undefined, status: 'ACTIVE' });
    }
    setFormError(null);
    setDormOpen(true);
  };

  const openRoomDialog = (room?: RoomResponse) => {
    if (room) {
      setEditingRoom(room);
      resetRoom({
        dormitoryId: room.dormitoryId,
        roomNumber: room.roomNumber,
        capacity: room.capacity,
        status: room.status,
      });
    } else {
      setEditingRoom(null);
      resetRoom({ dormitoryId: selectedDormitory || 0, roomNumber: '', capacity: 1, status: 'ACTIVE' });
    }
    setFormError(null);
    setRoomOpen(true);
  };

  const openBedDialog = (bed?: BedResponse) => {
    if (bed) {
      setEditingBed(bed);
      resetBed({ roomId: bed.roomId, bedNumber: bed.bedNumber, status: bed.status });
    } else {
      setEditingBed(null);
      resetBed({ roomId: selectedRoom || 0, bedNumber: '', status: 'AVAILABLE' });
    }
    setFormError(null);
    setBedOpen(true);
  };

  const openAllocateDialog = () => {
    resetAllocate({ studentId: 0, dormitoryId: selectedDormitory || 0, roomId: selectedRoom || 0, bedId: 0, status: 'ACTIVE' });
    setFormError(null);
    setAllocateOpen(true);
  };

  const openAttendanceDialog = (record?: BoardingAttendanceResponse) => {
    if (record) {
      setEditingAttendance(record);
      resetAttendance({
        studentId: record.studentId,
        dormitoryId: record.dormitoryId,
        attendanceDate: record.attendanceDate,
        status: record.status,
        remarks: record.remarks || '',
      });
    } else {
      setEditingAttendance(null);
      resetAttendance({
        studentId: 0,
        dormitoryId: selectedDormitory || 0,
        attendanceDate: attendanceDate || new Date().toISOString().split('T')[0],
        status: 'PRESENT',
        remarks: '',
      });
    }
    setFormError(null);
    setAttendanceOpen(true);
  };

  const getStatusVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'OCCUPIED':
        return 'success';
      case 'AVAILABLE':
        return 'outline';
      case 'PRESENT':
        return 'success';
      case 'ABSENT':
        return 'destructive';
      case 'LATE':
        return 'warning';
      case 'INACTIVE':
        return 'outline';
      default:
        return 'default';
    }
  };

  const matchesBoardingSearch = (...values: unknown[]) => {
    const query = boardingSearch.trim().toLowerCase();
    return !query || values.some((value) => String(value ?? '').toLowerCase().includes(query));
  };
  const filteredDormitories = (dormitories || []).filter((dorm) =>
    matchesBoardingSearch(dorm.name, dorm.gender, dorm.status)
  );
  const filteredRooms = (rooms || []).filter((room) =>
    matchesBoardingSearch(room.roomNumber, room.dormitoryName, room.status)
  );
  const filteredBeds = (beds || []).filter((bed) =>
    matchesBoardingSearch(bed.bedNumber, bed.roomNumber, bed.studentName, bed.status)
  );
  const filteredBoardingStudents = (boardingStudents || []).filter((record) =>
    matchesBoardingSearch(record.studentName, record.admissionNumber, record.dormitoryName, record.roomNumber, record.status)
  );
  const filteredAttendance = (attendance || []).filter((record) =>
    matchesBoardingSearch(record.studentName, record.admissionNumber, record.dormitoryName, record.attendanceDate, record.status, record.remarks)
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold">Boarding Management</h1>
          <p className="mt-1 text-muted-foreground">Manage dormitories, rooms, beds, and boarding attendance</p>
        </div>
      </div>

      {[dormError, roomError, bedError, studentsError, activeStudentsError, attendanceError].some(Boolean) && (
        <Alert variant="destructive">
          <AlertTitle>Unable to load boarding data</AlertTitle>
          <AlertDescription>
            {getApiErrorMessage(
              [dormError, roomError, bedError, studentsError, activeStudentsError, attendanceError].find(Boolean),
              'Please retry the request.'
            )}
          </AlertDescription>
        </Alert>
      )}
      {formError && !dormOpen && !roomOpen && !bedOpen && !allocateOpen && !attendanceOpen && (
        <Alert variant="destructive">
          <AlertTitle>Request failed</AlertTitle>
          <AlertDescription>{formError}</AlertDescription>
        </Alert>
      )}

      <Input
        placeholder="Search boarding records..."
        value={boardingSearch}
        onChange={(event) => setBoardingSearch(event.target.value)}
        className="max-w-md"
      />

      <Tabs value={activeTab} onValueChange={setActiveTab} defaultValue="dormitories">
        <TabsList>
          <TabsTrigger value="dormitories">Dormitories</TabsTrigger>
          <TabsTrigger value="rooms">Rooms</TabsTrigger>
          <TabsTrigger value="beds">Beds</TabsTrigger>
          <TabsTrigger value="students">Students</TabsTrigger>
          <TabsTrigger value="attendance">Attendance</TabsTrigger>
        </TabsList>

        <TabsContent value="dormitories">
          <Card className="p-4">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold">Dormitories</h2>
              <Button onClick={() => openDormDialog()}>
                <Plus className="mr-2 h-4 w-4" /> Add Dormitory
              </Button>
            </div>
            {dormLoading ? (
              <div className="space-y-3">
                {[...Array(3)].map((_, i) => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            ) : (
              <div className="rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Name</TableHead>
                      <TableHead>Gender</TableHead>
                      <TableHead>Capacity</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="w-12" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredDormitories.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-muted-foreground">
                          No dormitories found
                        </TableCell>
                      </TableRow>
                    )}
                    {filteredDormitories.map((dorm: DormitoryResponse) => (
                      <TableRow key={dorm.id}>
                        <TableCell className="font-medium">{dorm.name}</TableCell>
                        <TableCell>{dorm.gender || '-'}</TableCell>
                        <TableCell>{dorm.capacity}</TableCell>
                        <TableCell>
                          <Badge variant={getStatusVariant(dorm.status)}>{dorm.status}</Badge>
                        </TableCell>
                        <TableCell>
                          <DropdownMenu
                            trigger={
                              <button className="rounded-full p-1 hover:bg-muted">
                                <MoreVertical className="h-4 w-4" />
                              </button>
                            }
                          >
                            <DropdownMenuItem onClick={() => openDormDialog(dorm)}>Edit</DropdownMenuItem>
                            <DropdownMenuItem onClick={() => deleteDormMutation.mutate(dorm.id)}>
                              <Trash2 className="mr-2 h-4 w-4" /> Delete
                            </DropdownMenuItem>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </Card>
        </TabsContent>

        <TabsContent value="rooms">
          <Card className="p-4">
            <div className="mb-4 flex items-center gap-4">
              <Select
                {...registerRoom('dormitoryId', { valueAsNumber: true })}
                options={[
                  { value: '0', label: 'Select dormitory' },
                  ...(dormitories || []).map((d: DormitoryResponse) => ({ value: String(d.id), label: d.name })),
                ]}
                onChange={(e) => setSelectedDormitory(e.target.value ? Number(e.target.value) : null)}
              />
              {selectedDormitory && (
                <Button onClick={() => openRoomDialog()}>
                  <Plus className="mr-2 h-4 w-4" /> Add Room
                </Button>
              )}
            </div>
            {roomLoading ? (
              <div className="space-y-3">
                {[...Array(3)].map((_, i) => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            ) : (
              <div className="rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Room Number</TableHead>
                      <TableHead>Dormitory</TableHead>
                      <TableHead>Occupancy</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="w-12" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {!selectedDormitory && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-muted-foreground">
                          Select a dormitory to view rooms
                        </TableCell>
                      </TableRow>
                    )}
                    {selectedDormitory && filteredRooms.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-muted-foreground">
                          No rooms found
                        </TableCell>
                      </TableRow>
                    )}
                    {filteredRooms.map((room: RoomResponse) => (
                      <TableRow key={room.id}>
                        <TableCell className="font-medium">{room.roomNumber}</TableCell>
                        <TableCell>{room.dormitoryName || '-'}</TableCell>
                        <TableCell>{room.currentOccupancy}/{room.capacity}</TableCell>
                        <TableCell>
                          <Badge variant={getStatusVariant(room.status)}>{room.status}</Badge>
                        </TableCell>
                        <TableCell>
                          <DropdownMenu
                            trigger={
                              <button className="rounded-full p-1 hover:bg-muted">
                                <MoreVertical className="h-4 w-4" />
                              </button>
                            }
                          >
                            <DropdownMenuItem onClick={() => openRoomDialog(room)}>Edit</DropdownMenuItem>
                            <DropdownMenuItem onClick={() => deleteRoomMutation.mutate(room.id)}>
                              <Trash2 className="mr-2 h-4 w-4" /> Delete
                            </DropdownMenuItem>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </Card>
        </TabsContent>

        <TabsContent value="beds">
          <Card className="p-4">
            <div className="mb-4 flex items-center gap-4">
              <Select
                value={selectedDormitory ? String(selectedDormitory) : ''}
                onChange={(e) => { setSelectedDormitory(e.target.value ? Number(e.target.value) : null); setSelectedRoom(null); }}
                options={[
                  { value: '', label: 'Select dormitory' },
                  ...(dormitories || []).map((d: DormitoryResponse) => ({ value: String(d.id), label: d.name })),
                ]}
              />
              <Select
                value={selectedRoom ? String(selectedRoom) : ''}
                onChange={(e) => setSelectedRoom(e.target.value ? Number(e.target.value) : null)}
                options={[
                  { value: '', label: 'Select room' },
                  ...(rooms || []).map((r: RoomResponse) => ({ value: String(r.id), label: r.roomNumber })),
                ]}
                disabled={!selectedDormitory}
              />
              {selectedRoom && (
                <Button onClick={() => openBedDialog()}>
                  <Plus className="mr-2 h-4 w-4" /> Add Bed
                </Button>
              )}
            </div>
            {bedLoading ? (
              <div className="space-y-3">
                {[...Array(3)].map((_, i) => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            ) : (
              <div className="rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Bed Number</TableHead>
                      <TableHead>Room</TableHead>
                      <TableHead>Student</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="w-12" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {!selectedRoom && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-muted-foreground">
                          Select a room to view beds
                        </TableCell>
                      </TableRow>
                    )}
                    {selectedRoom && filteredBeds.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-muted-foreground">
                          No beds found
                        </TableCell>
                      </TableRow>
                    )}
                    {filteredBeds.map((bed: BedResponse) => (
                      <TableRow key={bed.id}>
                        <TableCell className="font-medium">{bed.bedNumber}</TableCell>
                        <TableCell>{bed.roomNumber || '-'}</TableCell>
                        <TableCell>{bed.studentName || '-'}</TableCell>
                        <TableCell>
                          <Badge variant={getStatusVariant(bed.status)}>{bed.status}</Badge>
                        </TableCell>
                        <TableCell>
                          <DropdownMenu
                            trigger={
                              <button className="rounded-full p-1 hover:bg-muted">
                                <MoreVertical className="h-4 w-4" />
                              </button>
                            }
                          >
                            <DropdownMenuItem onClick={() => openBedDialog(bed)}>Edit</DropdownMenuItem>
                            <DropdownMenuItem onClick={() => deleteBedMutation.mutate(bed.id)}>
                              <Trash2 className="mr-2 h-4 w-4" /> Delete
                            </DropdownMenuItem>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </Card>
        </TabsContent>

        <TabsContent value="students">
          <Card className="p-4">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold">Boarding Students</h2>
              <Button onClick={() => openAllocateDialog()}>
                <UserPlus className="mr-2 h-4 w-4" /> Allocate Student
              </Button>
            </div>
            {studentsLoading ? (
              <div className="space-y-3">
                {[...Array(3)].map((_, i) => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            ) : (
              <div className="rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Student</TableHead>
                      <TableHead>Dormitory</TableHead>
                      <TableHead>Room</TableHead>
                      <TableHead>Bed</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="w-12" />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredBoardingStudents.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={6} className="text-center text-muted-foreground">
                          No boarding students found
                        </TableCell>
                      </TableRow>
                    )}
                    {filteredBoardingStudents.map((record: BoardingStudentResponse) => (
                      <TableRow key={record.id}>
                        <TableCell className="font-medium">{record.studentName || '-'}</TableCell>
                        <TableCell>{record.dormitoryName || '-'}</TableCell>
                        <TableCell>{record.roomNumber || '-'}</TableCell>
                        <TableCell>{record.bedNumber || '-'}</TableCell>
                        <TableCell>
                          <Badge variant={getStatusVariant(record.status)}>{record.status}</Badge>
                        </TableCell>
                        <TableCell>
                          <DropdownMenu
                            trigger={
                              <button className="rounded-full p-1 hover:bg-muted">
                                <MoreVertical className="h-4 w-4" />
                              </button>
                            }
                          >
                            <DropdownMenuItem onClick={() => releaseMutation.mutate(record.id)}>
                              <UserX className="mr-2 h-4 w-4" /> Release
                            </DropdownMenuItem>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </Card>
        </TabsContent>

        <TabsContent value="attendance">
          <Card className="p-4">
            <div className="mb-4 flex items-center gap-4">
              <Select
                value={selectedDormitory ? String(selectedDormitory) : ''}
                onChange={(e) => { setSelectedDormitory(e.target.value ? Number(e.target.value) : null); setAttendanceDate(''); }}
                options={[
                  { value: '', label: 'Select dormitory' },
                  ...(dormitories || []).map((d: DormitoryResponse) => ({ value: String(d.id), label: d.name })),
                ]}
              />
              <Input
                label="Date"
                type="date"
                value={attendanceDate}
                onChange={(e) => setAttendanceDate(e.target.value)}
                className="sm:w-48"
              />
              {selectedDormitory && attendanceDate && (
                <Button onClick={() => openAttendanceDialog()}>
                  <Plus className="mr-2 h-4 w-4" /> Add Attendance
                </Button>
              )}
            </div>
            {attendanceLoading ? (
              <div className="space-y-3">
                {[...Array(3)].map((_, i) => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            ) : (
              <div className="rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Student</TableHead>
                      <TableHead>Date</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Remarks</TableHead>
                      <TableHead className="w-24">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {!selectedDormitory || !attendanceDate && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-muted-foreground">
                          Select a dormitory and date to view attendance
                        </TableCell>
                      </TableRow>
                    )}
                    {selectedDormitory && attendanceDate && filteredAttendance.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5} className="text-center text-muted-foreground">
                          No attendance records found
                        </TableCell>
                      </TableRow>
                    )}
                    {filteredAttendance.map((record: BoardingAttendanceResponse) => (
                      <TableRow key={record.id}>
                        <TableCell className="font-medium">{record.studentName || '-'}</TableCell>
                        <TableCell>{record.attendanceDate}</TableCell>
                        <TableCell>
                          <Badge variant={getStatusVariant(record.status)}>{record.status}</Badge>
                        </TableCell>
                        <TableCell>{record.remarks || '-'}</TableCell>
                        <TableCell>
                          <DropdownMenu
                            trigger={
                              <button className="rounded-full p-1 hover:bg-muted">
                                <MoreVertical className="h-4 w-4" />
                              </button>
                            }
                          >
                            <DropdownMenuItem onClick={() => openAttendanceDialog(record)}>Edit</DropdownMenuItem>
                            <DropdownMenuItem onClick={() => deleteAttendanceMutation.mutate(record.id)}>
                              <Trash2 className="mr-2 h-4 w-4" /> Delete
                            </DropdownMenuItem>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </Card>
        </TabsContent>
      </Tabs>

      <Dialog open={dormOpen} onOpenChange={setDormOpen}>
        <DialogContent title={editingDorm ? 'Edit Dormitory' : 'Add Dormitory'} description={editingDorm ? 'Update dormitory information' : 'Create a new dormitory'}>
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmitDorm(handleDormSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Name *</label>
              <Input {...registerDorm('name')} />
              {dormErrors.name && <p className="text-sm text-destructive">{dormErrors.name.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Description</label>
              <Input {...registerDorm('description')} />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Capacity</label>
                <Input type="number" {...registerDorm('capacity', { valueAsNumber: true })} />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Gender</label>
                <Select {...registerDorm('gender')} options={genderOptions} />
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setDormOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createDormMutation.isPending || updateDormMutation.isPending}>
                {(createDormMutation.isPending || updateDormMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {editingDorm ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={roomOpen} onOpenChange={setRoomOpen}>
        <DialogContent title={editingRoom ? 'Edit Room' : 'Add Room'} description={editingRoom ? 'Update room information' : 'Create a new room'}>
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmitRoom(handleRoomSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Dormitory *</label>
              <Select
                {...registerRoom('dormitoryId', { valueAsNumber: true })}
                options={[
                  { value: '0', label: 'Select dormitory' },
                  ...(dormitories || []).map((d: DormitoryResponse) => ({ value: String(d.id), label: d.name })),
                ]}
              />
              {roomErrors.dormitoryId && <p className="text-sm text-destructive">{roomErrors.dormitoryId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Room Number *</label>
              <Input {...registerRoom('roomNumber')} />
              {roomErrors.roomNumber && <p className="text-sm text-destructive">{roomErrors.roomNumber.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Capacity</label>
              <Input type="number" {...registerRoom('capacity', { valueAsNumber: true })} />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setRoomOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createRoomMutation.isPending || updateRoomMutation.isPending}>
                {(createRoomMutation.isPending || updateRoomMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {editingRoom ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={bedOpen} onOpenChange={setBedOpen}>
        <DialogContent title={editingBed ? 'Edit Bed' : 'Add Bed'} description={editingBed ? 'Update bed information' : 'Create a new bed'}>
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmitBed(handleBedSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Room *</label>
              <Select
                {...registerBed('roomId', { valueAsNumber: true })}
                options={[
                  { value: '0', label: 'Select room' },
                  ...(rooms || []).map((r: RoomResponse) => ({ value: String(r.id), label: r.roomNumber })),
                ]}
              />
              {bedErrors.roomId && <p className="text-sm text-destructive">{bedErrors.roomId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Bed Number *</label>
              <Input {...registerBed('bedNumber')} />
              {bedErrors.bedNumber && <p className="text-sm text-destructive">{bedErrors.bedNumber.message}</p>}
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setBedOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createBedMutation.isPending || updateBedMutation.isPending}>
                {(createBedMutation.isPending || updateBedMutation.isPending) && (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                )}
                {editingBed ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={allocateOpen} onOpenChange={setAllocateOpen}>
        <DialogContent title="Allocate Student" description="Assign a student to a bed">
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmitAllocate(handleAllocateSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Student ID *</label>
              <Input type="number" {...registerAllocate('studentId', { valueAsNumber: true })} />
              {allocateErrors.studentId && <p className="text-sm text-destructive">{allocateErrors.studentId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Dormitory *</label>
              <Select
                {...registerAllocate('dormitoryId', { valueAsNumber: true })}
                options={[
                  { value: '0', label: 'Select dormitory' },
                  ...(dormitories || []).map((d: DormitoryResponse) => ({ value: String(d.id), label: d.name })),
                ]}
              />
              {allocateErrors.dormitoryId && <p className="text-sm text-destructive">{allocateErrors.dormitoryId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Room *</label>
              <Select
                {...registerAllocate('roomId', { valueAsNumber: true })}
                options={[
                  { value: '0', label: 'Select room' },
                  ...(rooms || []).map((r: RoomResponse) => ({ value: String(r.id), label: r.roomNumber })),
                ]}
              />
              {allocateErrors.roomId && <p className="text-sm text-destructive">{allocateErrors.roomId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Bed *</label>
              <Select
                {...registerAllocate('bedId', { valueAsNumber: true })}
                options={[
                  { value: '0', label: 'Select bed' },
                  ...(beds || []).map((b: BedResponse) => ({ value: String(b.id), label: b.bedNumber })),
                ]}
              />
              {allocateErrors.bedId && <p className="text-sm text-destructive">{allocateErrors.bedId.message}</p>}
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setAllocateOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={allocateMutation.isPending}>
                {allocateMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                Allocate
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={attendanceOpen} onOpenChange={setAttendanceOpen}>
        <DialogContent title={editingAttendance ? 'Edit Attendance' : 'Add Attendance'} description={editingAttendance ? 'Update attendance record' : 'Record new attendance'}>
          {formError && (
            <Alert variant="destructive">
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          )}
          <form onSubmit={handleSubmitAttendance(handleAttendanceSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Student ID *</label>
              <Input type="number" {...registerAttendance('studentId', { valueAsNumber: true })} />
              {attendanceErrors.studentId && <p className="text-sm text-destructive">{attendanceErrors.studentId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Dormitory *</label>
              <Select
                {...registerAttendance('dormitoryId', { valueAsNumber: true })}
                options={[
                  { value: '0', label: 'Select dormitory' },
                  ...(dormitories || []).map((d: DormitoryResponse) => ({ value: String(d.id), label: d.name })),
                ]}
              />
              {attendanceErrors.dormitoryId && <p className="text-sm text-destructive">{attendanceErrors.dormitoryId.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Date *</label>
              <Input type="date" {...registerAttendance('attendanceDate')} />
              {attendanceErrors.attendanceDate && <p className="text-sm text-destructive">{attendanceErrors.attendanceDate.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Status *</label>
              <Select {...registerAttendance('status')} options={attendanceStatusOptions} />
              {attendanceErrors.status && <p className="text-sm text-destructive">{attendanceErrors.status.message}</p>}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Remarks</label>
              <Input {...registerAttendance('remarks')} />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setAttendanceOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={createAttendanceMutation.isPending || updateAttendanceMutation.isPending}>
                {createAttendanceMutation.isPending || updateAttendanceMutation.isPending ? 'Saving...' : editingAttendance ? 'Update' : 'Create'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
