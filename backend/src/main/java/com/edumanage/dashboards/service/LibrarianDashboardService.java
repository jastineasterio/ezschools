package com.edumanage.dashboards.service;

import com.edumanage.dashboards.dto.LibrarianDashboardDto;
import com.edumanage.library.dto.BorrowingDto;
import com.edumanage.library.entity.Book;
import com.edumanage.library.entity.BookCopy;
import com.edumanage.library.entity.Borrowing;
import com.edumanage.library.entity.Fine;
import com.edumanage.library.entity.LibraryMember;
import com.edumanage.library.repository.BookCopyRepository;
import com.edumanage.library.repository.BookRepository;
import com.edumanage.library.repository.BorrowingRepository;
import com.edumanage.library.repository.FineRepository;
import com.edumanage.library.repository.LibraryMemberRepository;
import com.edumanage.library.util.LibraryFeatureGuard;
import com.edumanage.schools.repository.SchoolRepository;
import com.edumanage.security.AccessGuard;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.users.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the librarian dashboard. All figures come from the library records of
 * the authenticated librarian's own school and never from another tenant.
 */
@Service
public class LibrarianDashboardService {

    private static final int RECENT_LIMIT = 10;

    private final AccessGuard accessGuard;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowingRepository borrowingRepository;
    private final LibraryMemberRepository libraryMemberRepository;
    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SchoolRepository schoolRepository;

    public LibrarianDashboardService(AccessGuard accessGuard,
                                     BookRepository bookRepository,
                                     BookCopyRepository bookCopyRepository,
                                     BorrowingRepository borrowingRepository,
                                     LibraryMemberRepository libraryMemberRepository,
                                     FineRepository fineRepository,
                                     UserRepository userRepository,
                                     StudentProfileRepository studentProfileRepository,
                                     SchoolRepository schoolRepository) {
        this.accessGuard = accessGuard;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowingRepository = borrowingRepository;
        this.libraryMemberRepository = libraryMemberRepository;
        this.fineRepository = fineRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.schoolRepository = schoolRepository;
    }

    @Transactional(readOnly = true)
    public LibrarianDashboardDto getDashboard() {
        accessGuard.assertLibraryAccess();

        Long schoolId = com.edumanage.tenant.TenantContext.requireTenantId();
        LibraryFeatureGuard.checkLibraryFeature(schoolId);

        LibrarianDashboardDto dto = new LibrarianDashboardDto();
        dto.setSchoolId(schoolId);
        dto.setSchoolName(schoolRepository.findById(schoolId).map(school -> school.getName()).orElse(null));

        dto.setTotalBooks(bookRepository.findBySchoolId(schoolId, PageRequest.of(0, 1)).getTotalElements());
        dto.setTotalCopies(bookCopyRepository.findBySchoolId(schoolId, PageRequest.of(0, 1)).getTotalElements());

        long available = bookCopyRepository.findBySchoolIdAndStatus(schoolId, "AVAILABLE").size();
        long borrowed = bookCopyRepository.findBySchoolIdAndStatus(schoolId, "BORROWED").size();
        dto.setAvailableCopies(available);
        dto.setBorrowedCopies(borrowed);
        dto.setUnavailableCopies(Math.max(0, dto.getTotalCopies() - available - borrowed));

        dto.setActiveLoans(borrowingRepository.countBySchoolIdAndStatus(schoolId, "BORROWED"));

        LocalDate today = LocalDate.now();
        List<Borrowing> overdue = borrowingRepository.findBySchoolIdAndDueDateBeforeAndStatus(schoolId, today, "BORROWED");
        dto.setOverdueLoans(overdue.size());

        dto.setReturnedThisMonth(borrowingRepository.countBySchoolIdAndStatusAndReturnDateBetween(
                schoolId, "RETURNED", today.withDayOfMonth(1), today));

        dto.setLibraryMembers(libraryMemberRepository.findBySchoolId(schoolId, PageRequest.of(0, 1)).getTotalElements());

        List<Fine> pendingFines = fineRepository.findBySchoolIdAndStatus(schoolId, "PENDING");
        dto.setPendingFines(pendingFines.size());
        dto.setPendingFineAmount(pendingFines.stream()
                .map(fine -> fine.getBalance() == null ? BigDecimal.ZERO : fine.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<Borrowing> recent = borrowingRepository.findBySchoolId(schoolId,
                        PageRequest.of(0, RECENT_LIMIT, Sort.by(Sort.Direction.DESC, "borrowDate")))
                .getContent().stream()
                .filter(borrowing -> !borrowing.isDeleted())
                .toList();
        dto.setRecentBorrowings(recent.stream().map(this::mapBorrowing).toList());
        dto.setOverdueBorrowings(overdue.stream().filter(borrowing -> !borrowing.isDeleted()).map(this::mapBorrowing).toList());
        dto.setRecentActivity(buildActivity(recent));

        return dto;
    }

    private List<LibrarianDashboardDto.Activity> buildActivity(List<Borrowing> borrowings) {
        List<LibrarianDashboardDto.Activity> activity = new ArrayList<>();
        for (Borrowing borrowing : borrowings) {
            BookCopy copy = borrowing.getBookCopyId() == null ? null
                    : bookCopyRepository.findById(borrowing.getBookCopyId()).orElse(null);
            Book book = copy == null || copy.getBookId() == null ? null
                    : bookRepository.findById(copy.getBookId()).orElse(null);
            LibraryMember member = borrowing.getMemberId() == null ? null
                    : libraryMemberRepository.findById(borrowing.getMemberId()).orElse(null);

            LibrarianDashboardDto.Activity entry = new LibrarianDashboardDto.Activity();
            entry.setBorrowingId(borrowing.getId());
            entry.setType("RETURNED".equals(borrowing.getStatus()) ? "RETURN" : "BORROW");
            entry.setBookTitle(book == null ? null : book.getTitle());
            entry.setBarcode(copy == null ? null : copy.getBarcode());
            entry.setMemberName(resolveMemberName(member));
            entry.setMembershipNumber(member == null ? null : member.getMembershipNumber());
            entry.setBorrowDate(borrowing.getBorrowDate());
            entry.setDueDate(borrowing.getDueDate());
            entry.setReturnDate(borrowing.getReturnDate());
            entry.setStatus(borrowing.getStatus());
            activity.add(entry);
        }
        activity.sort(Comparator.comparing(
                (LibrarianDashboardDto.Activity entry) -> entry.getReturnDate() != null ? entry.getReturnDate() : entry.getBorrowDate(),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return activity;
    }

    private BorrowingDto mapBorrowing(Borrowing borrowing) {
        BorrowingDto dto = new BorrowingDto();
        dto.setId(borrowing.getId());
        dto.setUuid(borrowing.getUuid() != null ? borrowing.getUuid().toString() : null);
        dto.setSchoolId(borrowing.getSchoolId());
        dto.setBookCopyId(borrowing.getBookCopyId());
        dto.setMemberId(borrowing.getMemberId());
        dto.setBorrowDate(borrowing.getBorrowDate());
        dto.setDueDate(borrowing.getDueDate());
        dto.setReturnDate(borrowing.getReturnDate());
        dto.setStatus(borrowing.getStatus());
        dto.setRemarks(borrowing.getRemarks());
        dto.setCreatedAt(borrowing.getCreatedAt());
        dto.setUpdatedAt(borrowing.getUpdatedAt());

        if (borrowing.getBookCopyId() != null) {
            bookCopyRepository.findById(borrowing.getBookCopyId()).ifPresent(copy -> {
                dto.setBarcode(copy.getBarcode());
                if (copy.getBookId() != null) {
                    bookRepository.findById(copy.getBookId()).ifPresent(book -> dto.setBookTitle(book.getTitle()));
                }
            });
        }
        if (borrowing.getMemberId() != null) {
            libraryMemberRepository.findById(borrowing.getMemberId()).ifPresent(member -> {
                dto.setMembershipNumber(member.getMembershipNumber());
                dto.setMemberType(member.getMemberType());
                dto.setMemberName(resolveMemberName(member));
            });
        }
        return dto;
    }

    private String resolveMemberName(LibraryMember member) {
        if (member == null || member.getMemberId() == null) {
            return null;
        }
        if ("STUDENT".equalsIgnoreCase(member.getMemberType())) {
            return studentProfileRepository.findById(member.getMemberId())
                    .filter(student -> !student.isDeleted())
                    .map(student -> student.getFirstName() + " " + student.getLastName())
                    .orElse(null);
        }
        return userRepository.findById(member.getMemberId())
                .filter(user -> !user.isDeleted())
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse(null);
    }
}
