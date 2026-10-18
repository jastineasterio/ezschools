package com.edumanage.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferencesRequest {
    private Boolean inAppEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
}
