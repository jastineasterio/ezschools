import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { getBookCategories, createBookCategory, updateBookCategory, deleteBookCategory, getBooks, createBook, updateBook, deleteBook, getBookCopies, createBookCopy, updateBookCopy, deleteBookCopy, getLibraryMembers, createLibraryMember, updateLibraryMember, deleteLibraryMember, getBorrowings, createBorrowing, returnBorrowing, detectOverdue, getFines, createFine, payFine, waiveFine, deleteFine, getActiveBookCategories, getOverdueBorrowings, getPendingFines } from '@/services/library';
import { getStudents } from '@/services/people';
import { getTeachers } from '@/services/people';
import type { BookCategory, CreateBookCategoryRequest, Book, CreateBookRequest, BookCopy, CreateBookCopyRequest, LibraryMember, CreateLibraryMemberRequest, Borrowing, Fine } from '@/types/library';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Badge } from '@/components/ui/Badge';
import { Dialog, DialogContent } from '@/components/ui/Dialog';
import { Alert } from '@/components/ui/Alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/Tabs';
import { Plus, BookOpen, Users, Clock, DollarSign, Tag, AlertTriangle } from 'lucide-react';

const categorySchema = z.object({ name: z.string().min(1, 'Name is required'), description: z.string().optional(), status: z.string().optional() });
const bookSchema = z.object({ categoryId: z.coerce.number().optional(), title: z.string().min(1, 'Title is required'), author: z.string().min(1, 'Author is required'), publisher: z.string().optional(), publishedDate: z.string().optional(), edition: z.string().optional(), language: z.string().optional(), pages: z.coerce.number().optional(), shelfLocation: z.string().optional(), totalCopies: z.coerce.number().optional(), isbn: z.string().optional(), status: z.string().optional() });
const bookCopySchema = z.object({ bookId: z.coerce.number(), barcode: z.string().min(1, 'Barcode is required'), conditionStatus: z.string().optional(), remarks: z.string().optional(), acquiredDate: z.string().optional(), status: z.string().optional() });
const memberSchema = z.object({ memberType: z.string().min(1, 'Member type is required'), memberId: z.coerce.number(), membershipNumber: z.string().min(1, 'Membership number is required'), joinedDate: z.string().min(1, 'Joined date is required'), membershipExpiry: z.string().optional(), status: z.string().optional() });
const borrowingSchema = z.object({ bookCopyId: z.coerce.number(), memberId: z.coerce.number(), borrowDate: z.string().min(1, 'Borrow date is required'), dueDate: z.string().min(1, 'Due date is required'), remarks: z.string().optional() });
const fineSchema = z.object({ borrowingId: z.coerce.number(), fineAmount: z.coerce.number().min(0, 'Fine amount must be non-negative'), fineType: z.string().optional(), remarks: z.string().optional() });

type CategoryForm = z.infer<typeof categorySchema>;
type BookForm = z.infer<typeof bookSchema>;
type BookCopyForm = z.infer<typeof bookCopySchema>;
type MemberForm = z.infer<typeof memberSchema>;
type BorrowingForm = z.infer<typeof borrowingSchema>;
type FineForm = z.infer<typeof fineSchema>;

export function ManagerLibraryPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState('categories');
  const [catOpen, setCatOpen] = useState(false);
  const [bookOpen, setBookOpen] = useState(false);
  const [copyOpen, setCopyOpen] = useState(false);
  const [memberOpen, setMemberOpen] = useState(false);
  const [borrowOpen, setBorrowOpen] = useState(false);
  const [fineOpen, setFineOpen] = useState(false);
  const [paying, setPaying] = useState<Fine | null>(null);
  const [payAmount, setPayAmount] = useState('0');

  const catForm = useForm<CategoryForm>({ resolver: zodResolver(categorySchema), defaultValues: { name: '', description: '', status: 'ACTIVE' } });
  const bookForm = useForm<BookForm>({ resolver: zodResolver(bookSchema), defaultValues: { title: '', author: '', totalCopies: 1, status: 'ACTIVE', language: 'ENGLISH' } });
  const copyForm = useForm<BookCopyForm>({ resolver: zodResolver(bookCopySchema), defaultValues: { bookId: 0, barcode: '', conditionStatus: 'GOOD', remarks: '', status: 'AVAILABLE' } });
  const memberForm = useForm<MemberForm>({ resolver: zodResolver(memberSchema), defaultValues: { memberType: 'STUDENT', membershipNumber: '', joinedDate: '', status: 'ACTIVE' } });
  const borrowForm = useForm<BorrowingForm>({ resolver: zodResolver(borrowingSchema), defaultValues: { bookCopyId: 0, memberId: 0, borrowDate: new Date().toISOString().split('T')[0], dueDate: '', remarks: '' } });
  const fineForm = useForm<FineForm>({ resolver: zodResolver(fineSchema), defaultValues: { borrowingId: 0, fineAmount: 0, fineType: 'OVERDUE', remarks: '' } });

  const { data: categoriesData } = useQuery({ queryKey: ['book-categories'], queryFn: () => getBookCategories() });
  const { data: booksData } = useQuery({ queryKey: ['books'], queryFn: () => getBooks() });
  const { data: activeCategories } = useQuery({ queryKey: ['active-categories'], queryFn: getActiveBookCategories });
  const { data: copiesData } = useQuery({ queryKey: ['book-copies'], queryFn: () => getBookCopies() });
  const { data: membersData } = useQuery({ queryKey: ['library-members'], queryFn: () => getLibraryMembers() });
  const { data: borrowingsData } = useQuery({ queryKey: ['borrowings'], queryFn: () => getBorrowings() });
  const { data: finesData } = useQuery({ queryKey: ['fines'], queryFn: () => getFines() });
  const { data: studentsData } = useQuery({ queryKey: ['students'], queryFn: () => getStudents(0, 200) });
  const { data: teachersData } = useQuery({ queryKey: ['teachers'], queryFn: () => getTeachers(0, 200) });
  const { data: overdueData } = useQuery({ queryKey: ['overdue-borrowings'], queryFn: getOverdueBorrowings });
  const { data: pendingFinesData } = useQuery({ queryKey: ['pending-fines'], queryFn: getPendingFines });

  const createCatMutation = useMutation({ mutationFn: createBookCategory, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['book-categories'] }); queryClient.invalidateQueries({ queryKey: ['active-categories'] }); setCatOpen(false); catForm.reset(); } });
  const updateCatMutation = useMutation({ mutationFn: ({ id, payload }: { id: number; payload: CreateBookCategoryRequest }) => updateBookCategory(id, payload), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['book-categories'] }); setCatOpen(false); catForm.reset(); } });
  const deleteCatMutation = useMutation({ mutationFn: deleteBookCategory, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['book-categories'] }) });
  const createBookMutation = useMutation({ mutationFn: createBook, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['books'] }); setBookOpen(false); bookForm.reset(); } });
  const updateBookMutation = useMutation({ mutationFn: ({ id, payload }: { id: number; payload: CreateBookRequest }) => updateBook(id, payload), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['books'] }); setBookOpen(false); bookForm.reset(); } });
  const deleteBookMutation = useMutation({ mutationFn: deleteBook, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['books'] }) });
  const createCopyMutation = useMutation({ mutationFn: createBookCopy, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['book-copies'] }); setCopyOpen(false); copyForm.reset(); } });
  const updateCopyMutation = useMutation({ mutationFn: ({ id, payload }: { id: number; payload: CreateBookCopyRequest }) => updateBookCopy(id, payload), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['book-copies'] }); setCopyOpen(false); copyForm.reset(); } });
  const deleteCopyMutation = useMutation({ mutationFn: deleteBookCopy, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['book-copies'] }) });
  const createMemberMutation = useMutation({ mutationFn: createLibraryMember, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['library-members'] }); setMemberOpen(false); memberForm.reset(); } });
  const updateMemberMutation = useMutation({ mutationFn: ({ id, payload }: { id: number; payload: CreateLibraryMemberRequest }) => updateLibraryMember(id, payload), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['library-members'] }); setMemberOpen(false); memberForm.reset(); } });
  const deleteMemberMutation = useMutation({ mutationFn: deleteLibraryMember, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['library-members'] }) });
  const createBorrowMutation = useMutation({ mutationFn: createBorrowing, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['borrowings'] }); queryClient.invalidateQueries({ queryKey: ['overdue-borrowings'] }); queryClient.invalidateQueries({ queryKey: ['fines'] }); setBorrowOpen(false); borrowForm.reset(); } });
  const returnBorrowMutation = useMutation({ mutationFn: returnBorrowing, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['borrowings'] }); queryClient.invalidateQueries({ queryKey: ['overdue-borrowings'] }); queryClient.invalidateQueries({ queryKey: ['fines'] }); } });
  const detectMutation = useMutation({ mutationFn: detectOverdue, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['borrowings'] }); queryClient.invalidateQueries({ queryKey: ['overdue-borrowings'] }); queryClient.invalidateQueries({ queryKey: ['fines'] }); } });
  const createFineMutation = useMutation({ mutationFn: createFine, onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['fines'] }); setFineOpen(false); fineForm.reset(); } });
  const payFineMutation = useMutation({ mutationFn: (vars: { id: number; amount: number }) => payFine(vars.id, vars.amount), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['fines'] }); setPaying(null); setPayAmount(''); } });
  const waiveFineMutation = useMutation({ mutationFn: waiveFine, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['fines'] }) });
  const deleteFineMutation = useMutation({ mutationFn: deleteFine, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['fines'] }) });

  const categoryItems = categoriesData?.content || [];
  const bookItems = booksData?.content || [];
  const copyItems = copiesData?.content || [];
  const memberItems = membersData?.content || [];
  const borrowingItems = borrowingsData?.content || [];
  const fineItems = finesData?.content || [];
  const studentOptions = studentsData?.content || [];
  const teacherOptions = teachersData?.content || [];
  const overdueItems: Borrowing[] = Array.isArray(overdueData) ? overdueData : [];
  const pendingFinesList: Fine[] = Array.isArray(pendingFinesData) ? pendingFinesData : [];

  const editingCatId = { current: 0 as number | undefined };
  const editingBookId = { current: 0 as number | undefined };
  const editingCopyId = { current: 0 as number | undefined };
  const editingMemberId = { current: 0 as number | undefined };

  const openCat = (item?: BookCategory) => {
    if (item) { catForm.reset({ name: item.name, description: item.description || '', status: item.status }); editingCatId.current = item.id; }
    else { catForm.reset({ name: '', description: '', status: 'ACTIVE' }); editingCatId.current = undefined; }
    setCatOpen(true);
  };
  const openBook = (item?: Book) => {
    if (item) { bookForm.reset({ categoryId: item.categoryId || undefined, title: item.title, author: item.author, publisher: item.publisher || '', publishedDate: item.publishedDate || '', edition: item.edition || '', language: item.language || 'ENGLISH', pages: item.pages, shelfLocation: item.shelfLocation || '', totalCopies: item.totalCopies, isbn: item.isbn || '', status: item.status }); editingBookId.current = item.id; }
    else { bookForm.reset({ title: '', author: '', totalCopies: 1, status: 'ACTIVE', language: 'ENGLISH' }); editingBookId.current = undefined; }
    setBookOpen(true);
  };
  const openCopy = (item?: BookCopy) => {
    if (item) { copyForm.reset({ bookId: item.bookId, barcode: item.barcode, conditionStatus: item.conditionStatus || 'GOOD', remarks: item.remarks || '', acquiredDate: item.acquiredDate || '', status: item.status }); editingCopyId.current = item.id; }
    else { copyForm.reset({ bookId: 0, barcode: '', conditionStatus: 'GOOD', remarks: '', status: 'AVAILABLE' }); editingCopyId.current = undefined; }
    setCopyOpen(true);
  };
  const openMember = (item?: LibraryMember) => {
    if (item) { memberForm.reset({ memberType: item.memberType, memberId: item.memberId, membershipNumber: item.membershipNumber, joinedDate: item.joinedDate, membershipExpiry: item.membershipExpiry || '', status: item.status }); editingMemberId.current = item.id; }
    else { memberForm.reset({ memberType: 'STUDENT', membershipNumber: '', joinedDate: '', status: 'ACTIVE' }); editingMemberId.current = undefined; }
    setMemberOpen(true);
  };

  const onCatSubmit = (values: CategoryForm) => { if (editingCatId.current) { updateCatMutation.mutate({ id: editingCatId.current, payload: values }); } else { createCatMutation.mutate(values); } };
  const onBookSubmit = (values: BookForm) => { if (editingBookId.current) { updateBookMutation.mutate({ id: editingBookId.current, payload: values }); } else { createBookMutation.mutate(values); } };
  const onCopySubmit = (values: BookCopyForm) => { if (editingCopyId.current) { updateCopyMutation.mutate({ id: editingCopyId.current, payload: values }); } else { createCopyMutation.mutate(values); } };
  const onMemberSubmit = (values: MemberForm) => { if (editingMemberId.current) { updateMemberMutation.mutate({ id: editingMemberId.current, payload: values }); } else { createMemberMutation.mutate(values); } };
  const onBorrowSubmit = (values: BorrowingForm) => { createBorrowMutation.mutate(values); };
  const onFineSubmit = (values: FineForm) => { createFineMutation.mutate(values); };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Library Management</h1>
      {(overdueItems.length > 0 || pendingFinesList.length > 0) && (
        <div className="flex gap-4 flex-wrap">
          {overdueItems.length > 0 && <Alert variant="destructive" className="flex-1 min-w-0"><AlertTriangle className="h-4 w-4" />{overdueItems.length} overdue borrowing(s) detected</Alert>}
          {pendingFinesList.length > 0 && <Alert variant="destructive" className="flex-1 min-w-0"><DollarSign className="h-4 w-4" />{pendingFinesList.length} pending fine(s)</Alert>}
        </div>
      )}
      <Tabs value={tab} onValueChange={setTab} defaultValue="categories">
        <TabsList>
          <TabsTrigger value="categories"><Tag className="h-4 w-4 mr-1" />Categories</TabsTrigger>
          <TabsTrigger value="books"><BookOpen className="h-4 w-4 mr-1" />Books</TabsTrigger>
          <TabsTrigger value="copies"><BookOpen className="h-4 w-4 mr-1" />Copies</TabsTrigger>
          <TabsTrigger value="members"><Users className="h-4 w-4 mr-1" />Members</TabsTrigger>
          <TabsTrigger value="borrowings"><Clock className="h-4 w-4 mr-1" />Borrowings</TabsTrigger>
          <TabsTrigger value="fines"><DollarSign className="h-4 w-4 mr-1" />Fines</TabsTrigger>
        </TabsList>

        <TabsContent value="categories">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">Book Categories</h2>
            <Button onClick={() => openCat()} className="gap-2"><Plus className="h-4 w-4" /> Add Category</Button>
          </div>
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b"><th className="text-left py-3 px-4">Name</th><th className="text-left py-3 px-4">Status</th><th className="text-right py-3 px-4">Actions</th></tr></thead>
                <tbody>
                  {categoryItems.length === 0 ? <tr><td colSpan={3} className="py-8 text-center text-muted-foreground">No categories found.</td></tr> :
                    categoryItems.map((item: BookCategory) => <tr key={item.id} className="border-b hover:bg-accent">
                      <td className="py-3 px-4 font-medium">{item.name}</td>
                      <td className="py-3 px-4"><Badge variant={item.status === 'ACTIVE' ? 'success' : 'secondary'}>{item.status}</Badge></td>
                      <td className="py-3 px-4"><div className="flex gap-2 justify-end"><Button variant="ghost" size="sm" onClick={() => openCat(item)}>Edit</Button><Button variant="ghost" size="sm" onClick={() => { if (confirm('Delete this category?')) deleteCatMutation.mutate(item.id); }}>Delete</Button></div></td>
                    </tr>)
                  }
                </tbody>
              </table>
            </div>
          </Card>
        </TabsContent>

        <TabsContent value="books">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">Books</h2>
            <Button onClick={() => openBook()} className="gap-2"><Plus className="h-4 w-4" /> Add Book</Button>
          </div>
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b"><th className="text-left py-3 px-4">Title</th><th className="text-left py-3 px-4">Author</th><th className="text-left py-3 px-4">ISBN</th><th className="text-left py-3 px-4">Available</th><th className="text-left py-3 px-4">Status</th><th className="text-right py-3 px-4">Actions</th></tr></thead>
                <tbody>
                  {bookItems.length === 0 ? <tr><td colSpan={6} className="py-8 text-center text-muted-foreground">No books found.</td></tr> :
                    bookItems.map((item: Book) => <tr key={item.id} className="border-b hover:bg-accent">
                      <td className="py-3 px-4">{item.title}</td><td className="py-3 px-4">{item.author}</td><td className="py-3 px-4">{item.isbn || '-'}</td><td className="py-3 px-4">{item.availableCopies} / {item.totalCopies}</td>
                      <td className="py-3 px-4"><Badge variant={item.status === 'ACTIVE' ? 'success' : 'secondary'}>{item.status}</Badge></td>
                      <td className="py-3 px-4"><div className="flex gap-2 justify-end"><Button variant="ghost" size="sm" onClick={() => openBook(item)}>Edit</Button><Button variant="ghost" size="sm" onClick={() => { if (confirm('Delete this book?')) deleteBookMutation.mutate(item.id); }}>Delete</Button></div></td>
                    </tr>)
                  }
                </tbody>
              </table>
            </div>
          </Card>
        </TabsContent>

        <TabsContent value="copies">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">Book Copies</h2>
            <Button onClick={() => openCopy()} className="gap-2"><Plus className="h-4 w-4" /> Add Copy</Button>
          </div>
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b"><th className="text-left py-3 px-4">Book</th><th className="text-left py-3 px-4">Barcode</th><th className="text-left py-3 px-4">Condition</th><th className="text-left py-3 px-4">Status</th><th className="text-right py-3 px-4">Actions</th></tr></thead>
                <tbody>
                  {copyItems.length === 0 ? <tr><td colSpan={5} className="py-8 text-center text-muted-foreground">No book copies found.</td></tr> :
                    copyItems.map((item: BookCopy) => <tr key={item.id} className="border-b hover:bg-accent">
                      <td className="py-3 px-4">{item.bookTitle || '-'}</td><td className="py-3 px-4">{item.barcode}</td><td className="py-3 px-4">{item.conditionStatus}</td>
                      <td className="py-3 px-4"><Badge variant={item.status === 'AVAILABLE' ? 'success' : item.status === 'BORROWED' ? 'warning' : 'secondary'}>{item.status}</Badge></td>
                      <td className="py-3 px-4"><div className="flex gap-2 justify-end"><Button variant="ghost" size="sm" onClick={() => openCopy(item)}>Edit</Button><Button variant="ghost" size="sm" onClick={() => { if (confirm('Delete this copy?')) deleteCopyMutation.mutate(item.id); }}>Delete</Button></div></td>
                    </tr>)
                  }
                </tbody>
              </table>
            </div>
          </Card>
        </TabsContent>

        <TabsContent value="members">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">Library Members</h2>
            <Button onClick={() => openMember()} className="gap-2"><Plus className="h-4 w-4" /> Add Member</Button>
          </div>
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b"><th className="text-left py-3 px-4">Membership #</th><th className="text-left py-3 px-4">Type</th><th className="text-left py-3 px-4">Joined Date</th><th className="text-left py-3 px-4">Expiry</th><th className="text-left py-3 px-4">Status</th><th className="text-right py-3 px-4">Actions</th></tr></thead>
                <tbody>
                  {memberItems.length === 0 ? <tr><td colSpan={6} className="py-8 text-center text-muted-foreground">No members found.</td></tr> :
                    memberItems.map((item: LibraryMember) => <tr key={item.id} className="border-b hover:bg-accent">
                      <td className="py-3 px-4">{item.membershipNumber}</td><td className="py-3 px-4">{item.memberType}</td><td className="py-3 px-4">{item.joinedDate}</td><td className="py-3 px-4">{item.membershipExpiry || '-'}</td>
                      <td className="py-3 px-4"><Badge variant={item.status === 'ACTIVE' ? 'success' : 'secondary'}>{item.status}</Badge></td>
                      <td className="py-3 px-4"><div className="flex gap-2 justify-end"><Button variant="ghost" size="sm" onClick={() => openMember(item)}>Edit</Button><Button variant="ghost" size="sm" onClick={() => { if (confirm('Delete this member?')) deleteMemberMutation.mutate(item.id); }}>Delete</Button></div></td>
                    </tr>)
                  }
                </tbody>
              </table>
            </div>
          </Card>
        </TabsContent>

        <TabsContent value="borrowings">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">Borrowings</h2>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => detectMutation.mutate()} disabled={detectMutation.isPending}>Detect Overdue</Button>
              <Button onClick={() => { borrowForm.reset({ bookCopyId: 0, memberId: 0, borrowDate: new Date().toISOString().split('T')[0], dueDate: '', remarks: '' }); setBorrowOpen(true); }} className="gap-2"><Plus className="h-4 w-4" /> New Borrowing</Button>
            </div>
          </div>
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b"><th className="text-left py-3 px-4">Book</th><th className="text-left py-3 px-4">Barcode</th><th className="text-left py-3 px-4">Member</th><th className="text-left py-3 px-4">Borrow Date</th><th className="text-left py-3 px-4">Due Date</th><th className="text-left py-3 px-4">Status</th><th className="text-right py-3 px-4">Actions</th></tr></thead>
                <tbody>
                  {borrowingItems.length === 0 ? <tr><td colSpan={7} className="py-8 text-center text-muted-foreground">No borrowings found.</td></tr> :
                    borrowingItems.map((item: Borrowing) => <tr key={item.id} className="border-b hover:bg-accent">
                      <td className="py-3 px-4">{item.bookTitle || '-'}</td><td className="py-3 px-4">{item.barcode || '-'}</td><td className="py-3 px-4">{item.memberName || '-'}</td><td className="py-3 px-4">{item.borrowDate}</td><td className="py-3 px-4">{item.dueDate}</td>
                      <td className="py-3 px-4"><Badge variant={item.status === 'RETURNED' ? 'success' : item.status === 'BORROWED' && item.dueDate && new Date(item.dueDate) < new Date() ? 'destructive' : item.status === 'BORROWED' ? 'warning' : 'secondary'}>{item.status}</Badge></td>
                      <td className="py-3 px-4 text-right">{item.status === 'BORROWED' && <Button size="sm" onClick={() => { if (confirm('Mark as returned?')) returnBorrowMutation.mutate(item.id); }} disabled={returnBorrowMutation.isPending}>Return</Button>}</td>
                    </tr>)
                  }
                </tbody>
              </table>
            </div>
          </Card>
        </TabsContent>

        <TabsContent value="fines">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold">Fines</h2>
            <Button onClick={() => { fineForm.reset({ borrowingId: 0, fineAmount: 0, fineType: 'OVERDUE', remarks: '' }); setFineOpen(true); }} className="gap-2"><Plus className="h-4 w-4" /> Add Fine</Button>
          </div>
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b"><th className="text-left py-3 px-4">Member</th><th className="text-left py-3 px-4">Fine Amount</th><th className="text-left py-3 px-4">Paid</th><th className="text-left py-3 px-4">Balance</th><th className="text-left py-3 px-4">Type</th><th className="text-left py-3 px-4">Status</th><th className="text-right py-3 px-4">Actions</th></tr></thead>
                <tbody>
                  {fineItems.length === 0 ? <tr><td colSpan={7} className="py-8 text-center text-muted-foreground">No fines found.</td></tr> :
                    fineItems.map((item: Fine) => <tr key={item.id} className="border-b hover:bg-accent">
                      <td className="py-3 px-4">{item.memberName || '-'}</td><td className="py-3 px-4">{item.fineAmount.toFixed(2)}</td><td className="py-3 px-4">{item.paidAmount.toFixed(2)}</td><td className="py-3 px-4">{item.balance.toFixed(2)}</td><td className="py-3 px-4">{item.fineType}</td>
                      <td className="py-3 px-4"><Badge variant={item.status === 'PAID' ? 'success' : item.status === 'WAIVED' ? 'secondary' : 'warning'}>{item.status}</Badge></td>
                      <td className="py-3 px-4"><div className="flex gap-2 justify-end">
                        {item.status === 'PENDING' && <><Button size="sm" variant="outline" onClick={() => { setPaying(item); setPayAmount(String(item.balance)); }}>Pay</Button><Button size="sm" variant="ghost" onClick={() => { if (confirm('Waive this fine?')) waiveFineMutation.mutate(item.id); }}>Waive</Button></>}
                        <Button variant="ghost" size="icon" onClick={() => { if (confirm('Delete this fine?')) deleteFineMutation.mutate(item.id); }}><DollarSign className="h-4 w-4 text-red-500" /></Button>
                      </div></td>
                    </tr>)
                  }
                </tbody>
              </table>
            </div>
          </Card>
        </TabsContent>
      </Tabs>

      <Dialog open={catOpen} onOpenChange={(o) => { if (!o) { setCatOpen(false); catForm.reset({ name: '', description: '', status: 'ACTIVE' }); } }}>
        <DialogContent title="Add Category">
          <form onSubmit={(e) => { e.preventDefault(); catForm.handleSubmit(onCatSubmit)(e); }} className="space-y-4">
            <div><label className="block text-sm font-medium mb-1">Name</label><Input {...catForm.register('name')} /></div>
            {catForm.formState.errors.name && <p className="text-sm text-red-600">{catForm.formState.errors.name.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Description</label><Input {...catForm.register('description')} /></div>
            <div><label className="block text-sm font-medium mb-1">Status</label><Controller name="status" control={catForm.control} render={({ field }) => (<Select options={[{ value: 'ACTIVE', label: 'Active' }, { value: 'INACTIVE', label: 'Inactive' }]} {...field} />)} /></div>
            <div className="flex justify-end gap-2 pt-2"><Button type="button" variant="secondary" onClick={() => { setCatOpen(false); catForm.reset({ name: '', description: '', status: 'ACTIVE' }); }}>Cancel</Button><Button type="submit" disabled={createCatMutation.isPending || updateCatMutation.isPending}>Save</Button></div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={bookOpen} onOpenChange={(o) => { if (!o) { setBookOpen(false); bookForm.reset({ title: '', author: '', totalCopies: 1, status: 'ACTIVE', language: 'ENGLISH' }); } }}>
        <DialogContent title="Add Book">
          <form onSubmit={(e) => { e.preventDefault(); bookForm.handleSubmit(onBookSubmit)(e); }} className="space-y-4">
            <div><label className="block text-sm font-medium mb-1">Title</label><Input {...bookForm.register('title')} /></div>
            {bookForm.formState.errors.title && <p className="text-sm text-red-600">{bookForm.formState.errors.title.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Author</label><Input {...bookForm.register('author')} /></div>
            {bookForm.formState.errors.author && <p className="text-sm text-red-600">{bookForm.formState.errors.author.message}</p>}
            <div><label className="block text-sm font-medium mb-1">ISBN</label><Input {...bookForm.register('isbn')} /></div>
            <div><label className="block text-sm font-medium mb-1">Category</label><Controller name="categoryId" control={bookForm.control} render={({ field }) => (<Select options={[{ value: '', label: 'None' }, ...(activeCategories || []).map((c: BookCategory) => ({ value: String(c.id), label: c.name }))]} {...field} />)} /></div>
            <div><label className="block text-sm font-medium mb-1">Publisher</label><Input {...bookForm.register('publisher')} /></div>
            <div><label className="block text-sm font-medium mb-1">Published Date</label><Input type="date" {...bookForm.register('publishedDate')} /></div>
            <div><label className="block text-sm font-medium mb-1">Edition</label><Input {...bookForm.register('edition')} /></div>
            <div><label className="block text-sm font-medium mb-1">Language</label><Input {...bookForm.register('language')} /></div>
            <div><label className="block text-sm font-medium mb-1">Pages</label><Input type="number" {...bookForm.register('pages')} /></div>
            <div><label className="block text-sm font-medium mb-1">Shelf Location</label><Input {...bookForm.register('shelfLocation')} /></div>
            <div><label className="block text-sm font-medium mb-1">Total Copies</label><Input type="number" {...bookForm.register('totalCopies')} /></div>
            <div><label className="block text-sm font-medium mb-1">Status</label><Controller name="status" control={bookForm.control} render={({ field }) => (<Select options={[{ value: 'ACTIVE', label: 'Active' }, { value: 'INACTIVE', label: 'Inactive' }]} {...field} />)} /></div>
            <div className="flex justify-end gap-2 pt-2"><Button type="button" variant="secondary" onClick={() => { setBookOpen(false); bookForm.reset({ title: '', author: '', totalCopies: 1, status: 'ACTIVE', language: 'ENGLISH' }); }}>Cancel</Button><Button type="submit" disabled={createBookMutation.isPending || updateBookMutation.isPending}>Save</Button></div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={copyOpen} onOpenChange={(o) => { if (!o) { setCopyOpen(false); copyForm.reset({ bookId: 0, barcode: '', conditionStatus: 'GOOD', remarks: '', status: 'AVAILABLE' }); } }}>
        <DialogContent title="Add Book Copy">
          <form onSubmit={(e) => { e.preventDefault(); copyForm.handleSubmit(onCopySubmit)(e); }} className="space-y-4">
            <div><label className="block text-sm font-medium mb-1">Book</label><Controller name="bookId" control={copyForm.control} render={({ field }) => (<Select options={bookItems.map((b: Book) => ({ value: String(b.id), label: b.title }))} {...field} />)} /></div>
            {copyForm.formState.errors.bookId && <p className="text-sm text-red-600">{copyForm.formState.errors.bookId.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Barcode</label><Input {...copyForm.register('barcode')} /></div>
            {copyForm.formState.errors.barcode && <p className="text-sm text-red-600">{copyForm.formState.errors.barcode.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Condition</label><Controller name="conditionStatus" control={copyForm.control} render={({ field }) => (<Select options={[{ value: 'GOOD', label: 'Good' }, { value: 'FAIR', label: 'Fair' }, { value: 'POOR', label: 'Poor' }]} {...field} />)} /></div>
            <div><label className="block text-sm font-medium mb-1">Remarks</label><Input {...copyForm.register('remarks')} /></div>
            <div><label className="block text-sm font-medium mb-1">Acquired Date</label><Input type="date" {...copyForm.register('acquiredDate')} /></div>
            <div><label className="block text-sm font-medium mb-1">Status</label><Controller name="status" control={copyForm.control} render={({ field }) => (<Select options={[{ value: 'AVAILABLE', label: 'Available' }, { value: 'BORROWED', label: 'Borrowed' }, { value: 'LOST', label: 'Lost' }]} {...field} />)} /></div>
            <div className="flex justify-end gap-2 pt-2"><Button type="button" variant="secondary" onClick={() => { setCopyOpen(false); copyForm.reset({ bookId: 0, barcode: '', conditionStatus: 'GOOD', remarks: '', status: 'AVAILABLE' }); }}>Cancel</Button><Button type="submit" disabled={createCopyMutation.isPending || updateCopyMutation.isPending}>Save</Button></div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={memberOpen} onOpenChange={(o) => { if (!o) { setMemberOpen(false); memberForm.reset({ memberType: 'STUDENT', membershipNumber: '', joinedDate: '', status: 'ACTIVE' }); } }}>
        <DialogContent title="Add Library Member">
          <form onSubmit={(e) => { e.preventDefault(); memberForm.handleSubmit(onMemberSubmit)(e); }} className="space-y-4">
            <div><label className="block text-sm font-medium mb-1">Member Type</label><Controller name="memberType" control={memberForm.control} render={({ field }) => (<Select options={[{ value: 'STUDENT', label: 'Student' }, { value: 'TEACHER', label: 'Teacher' }]} {...field} />)} /></div>
            <div><label className="block text-sm font-medium mb-1">Member</label><Controller name="memberId" control={memberForm.control} render={({ field }) => (<Select options={[...studentOptions.map((s: any) => ({ value: String(s.id), label: `${s.firstName} ${s.lastName} (${s.admissionNumber})` })), ...teacherOptions.map((t: any) => ({ value: String(t.id), label: `${(t as any).firstName || 'Teacher'} ${(t as any).lastName || ''}`.trim() }))]} {...field} />)} /></div>
            <div><label className="block text-sm font-medium mb-1">Membership Number</label><Input {...memberForm.register('membershipNumber')} /></div>
            {memberForm.formState.errors.membershipNumber && <p className="text-sm text-red-600">{memberForm.formState.errors.membershipNumber.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Joined Date</label><Input type="date" {...memberForm.register('joinedDate')} /></div>
            {memberForm.formState.errors.joinedDate && <p className="text-sm text-red-600">{memberForm.formState.errors.joinedDate.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Membership Expiry</label><Input type="date" {...memberForm.register('membershipExpiry')} /></div>
            <div><label className="block text-sm font-medium mb-1">Status</label><Controller name="status" control={memberForm.control} render={({ field }) => (<Select options={[{ value: 'ACTIVE', label: 'Active' }, { value: 'INACTIVE', label: 'Inactive' }]} {...field} />)} /></div>
            <div className="flex justify-end gap-2 pt-2"><Button type="button" variant="secondary" onClick={() => { setMemberOpen(false); memberForm.reset({ memberType: 'STUDENT', membershipNumber: '', joinedDate: '', status: 'ACTIVE' }); }}>Cancel</Button><Button type="submit" disabled={createMemberMutation.isPending || updateMemberMutation.isPending}>Save</Button></div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={borrowOpen} onOpenChange={(o) => { if (!o) { setBorrowOpen(false); borrowForm.reset({ bookCopyId: 0, memberId: 0, borrowDate: new Date().toISOString().split('T')[0], dueDate: '', remarks: '' }); } }}>
        <DialogContent title="New Borrowing">
          <form onSubmit={(e) => { e.preventDefault(); borrowForm.handleSubmit(onBorrowSubmit)(e); }} className="space-y-4">
            <div><label className="block text-sm font-medium mb-1">Book Copy</label><Controller name="bookCopyId" control={borrowForm.control} render={({ field }) => (<Select options={copyItems.filter((c: BookCopy) => c.status === 'AVAILABLE').map((c: BookCopy) => ({ value: String(c.id), label: `${c.bookTitle || 'Unknown'} (${c.barcode})` }))} {...field} />)} /></div>
            {borrowForm.formState.errors.bookCopyId && <p className="text-sm text-red-600">{borrowForm.formState.errors.bookCopyId.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Member</label><Controller name="memberId" control={borrowForm.control} render={({ field }) => (<Select options={memberItems.map((m: LibraryMember) => ({ value: String(m.id), label: `${m.membershipNumber} (${m.memberType})` }))} {...field} />)} /></div>
            {borrowForm.formState.errors.memberId && <p className="text-sm text-red-600">{borrowForm.formState.errors.memberId.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Borrow Date</label><Input type="date" {...borrowForm.register('borrowDate')} /></div>
            {borrowForm.formState.errors.borrowDate && <p className="text-sm text-red-600">{borrowForm.formState.errors.borrowDate.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Due Date</label><Input type="date" {...borrowForm.register('dueDate')} /></div>
            {borrowForm.formState.errors.dueDate && <p className="text-sm text-red-600">{borrowForm.formState.errors.dueDate.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Remarks</label><Input {...borrowForm.register('remarks')} /></div>
            <div className="flex justify-end gap-2 pt-2"><Button type="button" variant="secondary" onClick={() => { setBorrowOpen(false); borrowForm.reset({ bookCopyId: 0, memberId: 0, borrowDate: new Date().toISOString().split('T')[0], dueDate: '', remarks: '' }); }}>Cancel</Button><Button type="submit" disabled={createBorrowMutation.isPending}>Borrow</Button></div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={fineOpen} onOpenChange={(o) => { if (!o) { setFineOpen(false); fineForm.reset({ borrowingId: 0, fineAmount: 0, fineType: 'OVERDUE', remarks: '' }); } }}>
        <DialogContent title="Add Fine">
          <form onSubmit={(e) => { e.preventDefault(); fineForm.handleSubmit(onFineSubmit)(e); }} className="space-y-4">
            <div><label className="block text-sm font-medium mb-1">Borrowing ID</label><Input type="number" {...fineForm.register('borrowingId')} /></div>
            {fineForm.formState.errors.borrowingId && <p className="text-sm text-red-600">{fineForm.formState.errors.borrowingId.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Fine Amount</label><Input type="number" step="0.01" {...fineForm.register('fineAmount')} /></div>
            {fineForm.formState.errors.fineAmount && <p className="text-sm text-red-600">{fineForm.formState.errors.fineAmount.message}</p>}
            <div><label className="block text-sm font-medium mb-1">Fine Type</label><Controller name="fineType" control={fineForm.control} render={({ field }) => (<Select options={[{ value: 'OVERDUE', label: 'Overdue' }, { value: 'DAMAGE', label: 'Damage' }, { value: 'LOST', label: 'Lost' }]} {...field} />)} /></div>
            <div><label className="block text-sm font-medium mb-1">Remarks</label><Input {...fineForm.register('remarks')} /></div>
            <div className="flex justify-end gap-2 pt-2"><Button type="button" variant="secondary" onClick={() => { setFineOpen(false); fineForm.reset({ borrowingId: 0, fineAmount: 0, fineType: 'OVERDUE', remarks: '' }); }}>Cancel</Button><Button type="submit" disabled={createFineMutation.isPending}>Save</Button></div>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!paying} onOpenChange={(o) => { if (!o) { setPaying(null); setPayAmount('0'); } }}>
        <DialogContent title="Pay Fine">
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">Paying fine for {paying?.memberName || '-'}</p>
            <p className="text-sm">Balance: <strong>${paying?.balance.toFixed(2)}</strong></p>
            <div><label className="block text-sm font-medium mb-1">Amount</label><Input type="number" step="0.01" value={payAmount} onChange={(e) => setPayAmount(e.target.value)} /></div>
            <div className="flex justify-end gap-2 pt-2"><Button variant="secondary" onClick={() => { setPaying(null); setPayAmount('0'); }}>Cancel</Button><Button onClick={() => { if (paying && payAmount) payFineMutation.mutate({ id: paying.id, amount: parseFloat(payAmount) }); }} disabled={payFineMutation.isPending}>Pay</Button></div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

