package com.edumanage.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibraryMemberDto {
    private Long id;
    private String uuid;
    private Long schoolId;
    private String memberType;
    private Long memberId;
    private String memberName;
    private String membershipNumber;
    private LocalDate joinedDate;
    private LocalDate membershipExpiry;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
