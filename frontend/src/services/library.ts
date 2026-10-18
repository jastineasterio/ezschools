import { api } from './api';
import type { PaginatedResponse } from '@/types/api';
import type {
  BookCategory, CreateBookCategoryRequest,
  Book, CreateBookRequest,
  BookCopy, CreateBookCopyRequest,
  LibraryMember, CreateLibraryMemberRequest,
  Borrowing, CreateBorrowingRequest,
  Fine, CreateFineRequest,
} from '@/types/library';

export async function getBookCategories(page = 0, size = 20): Promise<PaginatedResponse<BookCategory>> {
  const { data } = await api.get(`/school/library/categories?page=${page}&size=${size}`);
  return data.data;
}

export async function getActiveBookCategories(): Promise<BookCategory[]> {
  const { data } = await api.get('/school/library/categories/active');
  return data.data;
}

export async function getBookCategory(id: number) {
  const { data } = await api.get(`/school/library/categories/${id}`);
  return data.data;
}

export async function createBookCategory(payload: CreateBookCategoryRequest) {
  const { data } = await api.post('/school/library/categories', payload);
  return data.data;
}

export async function updateBookCategory(id: number, payload: CreateBookCategoryRequest) {
  const { data } = await api.put(`/school/library/categories/${id}`, payload);
  return data.data;
}

export async function deleteBookCategory(id: number) {
  const { data } = await api.delete(`/school/library/categories/${id}`);
  return data.data;
}

export async function getBooks(page = 0, size = 20): Promise<PaginatedResponse<Book>> {
  const { data } = await api.get(`/school/library/books?page=${page}&size=${size}`);
  return data.data;
}

export async function getBook(id: number) {
  const { data } = await api.get(`/school/library/books/${id}`);
  return data.data;
}

export async function createBook(payload: CreateBookRequest) {
  const { data } = await api.post('/school/library/books', payload);
  return data.data;
}

export async function updateBook(id: number, payload: CreateBookRequest) {
  const { data } = await api.put(`/school/library/books/${id}`, payload);
  return data.data;
}

export async function deleteBook(id: number) {
  const { data } = await api.delete(`/school/library/books/${id}`);
  return data.data;
}

export async function getBookCopies(page = 0, size = 20): Promise<PaginatedResponse<BookCopy>> {
  const { data } = await api.get(`/school/library/copies?page=${page}&size=${size}`);
  return data.data;
}

export async function getBookCopiesByBook(bookId: number): Promise<BookCopy[]> {
  const { data } = await api.get(`/school/library/copies/book/${bookId}`);
  return data.data;
}

export async function createBookCopy(payload: CreateBookCopyRequest) {
  const { data } = await api.post('/school/library/copies', payload);
  return data.data;
}

export async function updateBookCopy(id: number, payload: CreateBookCopyRequest) {
  const { data } = await api.put(`/school/library/copies/${id}`, payload);
  return data.data;
}

export async function deleteBookCopy(id: number) {
  const { data } = await api.delete(`/school/library/copies/${id}`);
  return data.data;
}

export async function getLibraryMembers(page = 0, size = 20): Promise<PaginatedResponse<LibraryMember>> {
  const { data } = await api.get(`/school/library/members?page=${page}&size=${size}`);
  return data.data;
}

export async function getLibraryMembersByType(memberType: string): Promise<LibraryMember[]> {
  const { data } = await api.get(`/school/library/members/type/${memberType}`);
  return data.data;
}

export async function createLibraryMember(payload: CreateLibraryMemberRequest) {
  const { data } = await api.post('/school/library/members', payload);
  return data.data;
}

export async function updateLibraryMember(id: number, payload: CreateLibraryMemberRequest) {
  const { data } = await api.put(`/school/library/members/${id}`, payload);
  return data.data;
}

export async function deleteLibraryMember(id: number) {
  const { data } = await api.delete(`/school/library/members/${id}`);
  return data.data;
}

export async function getBorrowings(page = 0, size = 20): Promise<PaginatedResponse<Borrowing>> {
  const { data } = await api.get(`/school/library/borrowings?page=${page}&size=${size}`);
  return data.data;
}

export async function getOverdueBorrowings(): Promise<Borrowing[]> {
  const { data } = await api.get('/school/library/borrowings/overdue');
  return data.data;
}

export async function getMemberBorrowings(memberId: number): Promise<Borrowing[]> {
  const { data } = await api.get(`/school/library/borrowings/member/${memberId}`);
  return data.data;
}

export async function createBorrowing(payload: CreateBorrowingRequest) {
  const { data } = await api.post('/school/library/borrowings', payload);
  return data.data;
}

export async function returnBorrowing(id: number) {
  const { data } = await api.post(`/school/library/borrowings/${id}/return`);
  return data.data;
}

export async function detectOverdue() {
  const { data } = await api.post('/school/library/borrowings/detect-overdue');
  return data.data;
}

export async function getFines(page = 0, size = 20): Promise<PaginatedResponse<Fine>> {
  const { data } = await api.get(`/school/library/fines?page=${page}&size=${size}`);
  return data.data;
}

export async function getPendingFines(): Promise<Fine[]> {
  const { data } = await api.get('/school/library/fines/pending');
  return data.data;
}

export async function createFine(payload: CreateFineRequest) {
  const { data } = await api.post('/school/library/fines', payload);
  return data.data;
}

export async function payFine(id: number, amount: number) {
  const { data } = await api.post(`/school/library/fines/${id}/pay`, null, { params: { amount } });
  return data.data;
}

export async function waiveFine(id: number) {
  const { data } = await api.post(`/school/library/fines/${id}/waive`);
  return data.data;
}

export async function deleteFine(id: number) {
  const { data } = await api.delete(`/school/library/fines/${id}`);
  return data.data;
}
