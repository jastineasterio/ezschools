package com.edumanage.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateCampaignRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 1600)
    private String message;

    @NotBlank
    private String category;

    @NotBlank
    private String channel;

    @NotBlank
    private String recipientGroup;

    @Size(max = 100)
    private List<Long> recipientUserIds;

    @Pattern(regexp = "^[A-Za-z0-9._:-]{16,100}$")
    private String idempotencyKey;
}
