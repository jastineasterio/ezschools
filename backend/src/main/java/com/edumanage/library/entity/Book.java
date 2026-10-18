package com.edumanage.library.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "books")
public class Book extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "author", nullable = false, length = 255)
    private String author;

    @Column(name = "publisher", length = 255)
    private String publisher;

    @Column(name = "published_date")
    private java.time.LocalDate publishedDate;

    @Column(name = "edition", length = 100)
    private String edition;

    @Column(name = "language", length = 50)
    private String language = "ENGLISH";

    @Column(name = "pages")
    private Integer pages;

    @Column(name = "shelf_location", length = 100)
    private String shelfLocation;

    @Column(name = "total_copies", nullable = false)
    private Integer totalCopies = 1;

    @Column(name = "available_copies", nullable = false)
    private Integer availableCopies = 1;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
