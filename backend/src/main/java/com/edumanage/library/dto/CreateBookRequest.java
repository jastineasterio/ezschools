package com.edumanage.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequest {
    private Long categoryId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    private String author;

    @Size(max = 255, message = "Publisher must not exceed 255 characters")
    private String publisher;

    private LocalDate publishedDate;

    @Size(max = 100, message = "Edition must not exceed 100 characters")
    private String edition;

    private String language = "ENGLISH";

    private Integer pages;

    @Size(max = 100, message = "Shelf location must not exceed 100 characters")
    private String shelfLocation;

    private Integer totalCopies = 1;

    @Size(max = 20, message = "ISBN must not exceed 20 characters")
    private String isbn;

    private String status = "ACTIVE";
}
