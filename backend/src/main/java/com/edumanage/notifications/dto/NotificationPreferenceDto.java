package com.edumanage.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceDto {
    private Long id;
    private Long schoolId;
    private Long userId;
    private Boolean inAppEnabled;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
}
