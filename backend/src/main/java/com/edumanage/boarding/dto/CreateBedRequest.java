package com.edumanage.boarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBedRequest {
    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotBlank(message = "Bed number is required")
    @Size(max = 50)
    private String bedNumber;

    private String status;
}
