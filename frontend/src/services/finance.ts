import { api } from './api';
import type { PaginatedResponse } from '@/types/api';
import type { FinancialCategory, CreateFinancialCategoryRequest, FinancialTransaction, CreateFinancialTransactionRequest } from '@/types/finance';

export async function getFinancialCategories(page = 0, size = 20): Promise<PaginatedResponse<FinancialCategory>> {
  const { data } = await api.get(`/school/finance/categories?page=${page}&size=${size}`);
  return data.data;
}

export async function getFinancialCategoriesByType(categoryType: string): Promise<FinancialCategory[]> {
  const { data } = await api.get(`/school/finance/categories/type/${categoryType}`);
  return data.data;
}

export async function getFinancialCategory(id: number) {
  const { data } = await api.get(`/school/finance/categories/${id}`);
  return data.data;
}

export async function createFinancialCategory(payload: CreateFinancialCategoryRequest) {
  const { data } = await api.post('/school/finance/categories', payload);
  return data.data;
}

export async function updateFinancialCategory(id: number, payload: CreateFinancialCategoryRequest) {
  const { data } = await api.put(`/school/finance/categories/${id}`, payload);
  return data.data;
}

export async function getFinancialTransactions(page = 0, size = 20): Promise<PaginatedResponse<FinancialTransaction>> {
  const { data } = await api.get(`/school/finance/transactions?page=${page}&size=${size}`);
  return data.data;
}

export async function createFinancialTransaction(payload: CreateFinancialTransactionRequest) {
  const { data } = await api.post('/school/finance/transactions', payload);
  return data.data;
}
