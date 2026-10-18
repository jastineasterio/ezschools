export interface FinancialCategory {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  categoryType: string;
  description: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFinancialCategoryRequest {
  name: string;
  categoryType: string;
  description?: string;
  status?: string;
}

export interface FinancialTransaction {
  id: number;
  uuid: string;
  schoolId: number;
  categoryId: number;
  categoryName: string;
  amount: number;
  transactionType: string;
  transactionDate: string;
  referenceNumber: string | null;
  description: string | null;
  paymentId: number | null;
  feeAssignmentId: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFinancialTransactionRequest {
  categoryId: number;
  amount: number;
  transactionType: string;
  transactionDate: string;
  referenceNumber?: string;
  description?: string;
  paymentId?: number;
  feeAssignmentId?: number;
}
