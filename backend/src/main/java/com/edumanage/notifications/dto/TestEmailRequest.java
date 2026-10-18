package com.edumanage.notifications.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TestEmailRequest(@NotBlank @Email String recipient, boolean confirmSend) {
}
