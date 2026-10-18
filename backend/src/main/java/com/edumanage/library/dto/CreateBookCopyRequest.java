package com.edumanage.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookCopyRequest {
    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotBlank(message = "Barcode is required")
    @Size(max = 100, message = "Barcode must not exceed 100 characters")
    private String barcode;

    private String conditionStatus = "GOOD";

    private String remarks;

    private LocalDate acquiredDate;

    private String status = "AVAILABLE";
}
