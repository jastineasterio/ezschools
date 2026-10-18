package com.edumanage.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private Long categoryId;
    private String categoryName;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishedDate;
    private String edition;
    private String language;
    private Integer pages;
    private String shelfLocation;
    private Integer totalCopies;
    private Integer availableCopies;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
