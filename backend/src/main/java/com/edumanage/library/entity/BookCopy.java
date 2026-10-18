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
@Table(name = "book_copies")
public class BookCopy extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "barcode", nullable = false, length = 100)
    private String barcode;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "AVAILABLE";

    @Column(name = "condition_status", length = 20)
    private String conditionStatus = "GOOD";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "acquired_date")
    private java.time.LocalDate acquiredDate;
}
