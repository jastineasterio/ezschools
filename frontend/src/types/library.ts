export interface BookCategory {
  id: number;
  uuid: string;
  schoolId: number;
  name: string;
  description: string;
  status: string;
}

export interface CreateBookCategoryRequest {
  name: string;
  description?: string;
  status?: string;
}

export interface Book {
  id: number;
  uuid: string;
  schoolId: number;
  categoryId: number | null;
  categoryName?: string;
  isbn?: string;
  title: string;
  author: string;
  publisher?: string;
  publishedDate?: string;
  edition?: string;
  language?: string;
  pages?: number;
  shelfLocation?: string;
  totalCopies: number;
  availableCopies: number;
  status: string;
}

export interface CreateBookRequest {
  categoryId?: number;
  title: string;
  author: string;
  publisher?: string;
  publishedDate?: string;
  edition?: string;
  language?: string;
  pages?: number;
  shelfLocation?: string;
  totalCopies?: number;
  isbn?: string;
  status?: string;
}

export interface BookCopy {
  id: number;
  uuid: string;
  schoolId: number;
  bookId: number;
  bookTitle?: string;
  barcode: string;
  status: string;
  conditionStatus: string;
  remarks?: string;
  acquiredDate?: string;
}

export interface CreateBookCopyRequest {
  bookId: number;
  barcode: string;
  conditionStatus?: string;
  remarks?: string;
  acquiredDate?: string;
  status?: string;
}

export interface LibraryMember {
  id: number;
  uuid: string;
  schoolId: number;
  memberType: string;
  memberId: number;
  memberName?: string;
  membershipNumber: string;
  joinedDate: string;
  membershipExpiry?: string;
  status: string;
}

export interface CreateLibraryMemberRequest {
  memberType: string;
  memberId: number;
  membershipNumber: string;
  joinedDate: string;
  membershipExpiry?: string;
  status?: string;
}

export interface Borrowing {
  id: number;
  uuid: string;
  schoolId: number;
  bookCopyId: number;
  bookTitle?: string;
  barcode?: string;
  memberId: number;
  memberName?: string;
  memberType?: string;
  membershipNumber?: string;
  borrowDate: string;
  dueDate: string;
  returnDate?: string;
  status: string;
  remarks?: string;
  fineAmount?: number;
}

export interface CreateBorrowingRequest {
  bookCopyId: number;
  memberId: number;
  borrowDate: string;
  dueDate: string;
  remarks?: string;
}

export interface Fine {
  id: number;
  uuid: string;
  schoolId: number;
  borrowingId: number;
  memberId: number;
  memberName?: string;
  membershipNumber?: string;
  fineAmount: number;
  paidAmount: number;
  balance: number;
  fineType: string;
  status: string;
  paidDate?: string;
  remarks?: string;
}

export interface CreateFineRequest {
  borrowingId: number;
  fineAmount: number;
  fineType?: string;
  remarks?: string;
}
