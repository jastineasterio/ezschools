package com.edumanage.library.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBorrowingRequest {
    @NotNull(message = "Book copy ID is required")
    private Long bookCopyId;

    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotNull(message = "Borrow date is required")
    private LocalDate borrowDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String remarks;
}
