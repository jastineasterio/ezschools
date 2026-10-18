package com.edumanage.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLibraryMemberRequest {
    @NotBlank(message = "Member type is required")
    @Pattern(regexp = "STUDENT|TEACHER", message = "Member type must be STUDENT or TEACHER")
    private String memberType;

    @NotNull(message = "Member ID is required")
    private Long memberId;

    @NotBlank(message = "Membership number is required")
    @Size(max = 100, message = "Membership number must not exceed 100 characters")
    private String membershipNumber;

    @NotNull(message = "Joined date is required")
    private LocalDate joinedDate;

    private LocalDate membershipExpiry;

    private String status = "ACTIVE";
}
