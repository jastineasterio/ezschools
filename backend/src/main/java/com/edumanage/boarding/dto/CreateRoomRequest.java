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
public class CreateRoomRequest {
    @NotNull(message = "Dormitory ID is required")
    private Long dormitoryId;

    @NotBlank(message = "Room number is required")
    @Size(max = 50)
    private String roomNumber;

    private Integer capacity;

    private String status;
}
