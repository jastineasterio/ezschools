package com.edumanage.dashboards.dto;

import com.edumanage.library.dto.BorrowingDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Landing payload for the librarian dashboard. Every figure is scoped to the
 * library of the authenticated librarian's own school.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibrarianDashboardDto {

    private Long schoolId;
    private String schoolName;
    private long totalBooks;
    private long totalCopies;
    private long availableCopies;
    private long borrowedCopies;
    private long unavailableCopies;
    private long activeLoans;
    private long overdueLoans;
    private long returnedThisMonth;
    private long libraryMembers;
    private long pendingFines;
    private BigDecimal pendingFineAmount = BigDecimal.ZERO;
    private List<BorrowingDto> recentBorrowings = new ArrayList<>();
    private List<BorrowingDto> overdueBorrowings = new ArrayList<>();
    private List<Activity> recentActivity = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Activity {
        private Long borrowingId;
        private String type;
        private String bookTitle;
        private String barcode;
        private String memberName;
        private String membershipNumber;
        private LocalDate borrowDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private String status;
    }
}
