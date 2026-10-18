export interface FeeStructure {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  description: string | null;
  amount: number;
  dueDate: string | null;
  academicYearId: number | null;
  classId: number | null;
  feeType: string;
  isMandatory: boolean;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFeeStructureRequest {
  name: string;
  description?: string;
  amount: number;
  dueDate?: string;
  academicYearId?: number;
  classId?: number;
  feeType: string;
  isMandatory?: boolean;
  status?: string;
}

export interface FeeAssignment {
  id: number;
  uuid: string;
  schoolId: number;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  feeStructureId: number;
  feeStructureName: string;
  amountDue: number;
  amountPaid: number;
  balance: number;
  dueDate: string | null;
  status: string;
  assignedAt: string;
}

export interface CreateFeeAssignmentRequest {
  studentId: number;
  feeStructureId: number;
  dueDate?: string;
}

export interface Payment {
  id: number;
  uuid: string;
  schoolId: number;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  feeAssignmentId: number | null;
  amount: number;
  paymentMethod: string;
  paymentDate: string;
  referenceNumber: string | null;
  receiptNumber: string | null;
  remarks: string | null;
  status: string;
}

export interface CreatePaymentRequest {
  studentId: number;
  feeAssignmentId?: number;
  amount: number;
  paymentMethod: string;
  paymentDate: string;
  referenceNumber?: string;
  receiptNumber?: string;
  remarks?: string;
}
