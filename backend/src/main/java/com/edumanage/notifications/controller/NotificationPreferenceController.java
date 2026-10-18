package com.edumanage.notifications.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.notifications.dto.NotificationPreferenceDto;
import com.edumanage.notifications.dto.UpdateNotificationPreferencesRequest;
import com.edumanage.notifications.entity.NotificationPreference;
import com.edumanage.notifications.repository.NotificationPreferenceRepository;
import com.edumanage.security.CustomUserDetails;
import com.edumanage.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/school/notifications/preferences")
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN','MANAGER')")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceRepository preferenceRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceDto>> getPreferences() {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        Optional<NotificationPreference> pref = preferenceRepository.findBySchoolIdAndUserId(tenantId, userId);
        NotificationPreferenceDto dto;
        if (pref.isEmpty()) {
            dto = new NotificationPreferenceDto(null, tenantId, userId, true, false, false);
        } else {
            dto = map(pref.get());
        }

        return ResponseEntity.ok(new ApiResponse<>(true, "Preferences fetched", dto, null,
                LocalDateTime.now(), "/api/school/notifications/preferences"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceDto>> updatePreferences(
            @RequestBody UpdateNotificationPreferencesRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long userId = getCurrentUserId();

        NotificationPreference pref = preferenceRepository.findBySchoolIdAndUserId(tenantId, userId)
                .orElseGet(() -> {
                    NotificationPreference p = new NotificationPreference();
                    p.setSchoolId(tenantId);
                    p.setUserId(userId);
                    return p;
                });

        if (request.getInAppEnabled() != null) pref.setInAppEnabled(request.getInAppEnabled());
        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getSmsEnabled() != null) pref.setSmsEnabled(request.getSmsEnabled());

        preferenceRepository.save(pref);

        return ResponseEntity.ok(new ApiResponse<>(true, "Preferences updated", map(pref), null,
                LocalDateTime.now(), "/api/school/notifications/preferences"));
    }

    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).id();
        }
        throw new com.edumanage.exception.ForbiddenException("User not authenticated");
    }

    private NotificationPreferenceDto map(NotificationPreference pref) {
        return new NotificationPreferenceDto(pref.getId(), pref.getSchoolId(), pref.getUserId(),
                pref.getInAppEnabled(), pref.getEmailEnabled(), pref.getSmsEnabled());
    }
}
