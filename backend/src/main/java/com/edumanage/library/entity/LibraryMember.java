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
@Table(name = "library_members")
public class LibraryMember extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "member_type", nullable = false, length = 20)
    private String memberType;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "membership_number", nullable = false, length = 100)
    private String membershipNumber;

    @Column(name = "joined_date", nullable = false)
    private java.time.LocalDate joinedDate;

    @Column(name = "membership_expiry")
    private java.time.LocalDate membershipExpiry;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
