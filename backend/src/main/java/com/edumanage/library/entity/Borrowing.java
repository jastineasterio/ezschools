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
@Table(name = "borrowings")
public class Borrowing extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "book_copy_id", nullable = false)
    private Long bookCopyId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "borrow_date", nullable = false)
    private java.time.LocalDate borrowDate;

    @Column(name = "due_date", nullable = false)
    private java.time.LocalDate dueDate;

    @Column(name = "return_date")
    private java.time.LocalDate returnDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "BORROWED";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
