package com.edumanage.sms.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.notifications.service.DarsmsProviderService;
import com.edumanage.sms.dto.CreateSmsConfigurationRequest;
import com.edumanage.sms.entity.SmsConfiguration;
import com.edumanage.sms.repository.SmsConfigurationRepository;
import com.edumanage.sms.service.SmsEncryptionService;
import com.edumanage.schools.entity.School;
import com.edumanage.schools.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmsConfigurationServiceTest {

    private SmsConfigurationRepository repo;
    private SmsEncryptionService encryption;
    private AuditLogService audit;
    private SchoolRepository schoolRepo;
    private DarsmsProviderService darsms;
    private SmsConfigurationService service;

    @BeforeEach
    void setUp() {
        repo = mock(SmsConfigurationRepository.class);
        encryption = mock(SmsEncryptionService.class);
        audit = mock(AuditLogService.class);
        schoolRepo = mock(SchoolRepository.class);
        darsms = mock(DarsmsProviderService.class);
        service = new SmsConfigurationService(repo, encryption, audit, schoolRepo, darsms);
    }

    @Test
    void createConfigurationRequiresApiKey() {
        CreateSmsConfigurationRequest req = new CreateSmsConfigurationRequest();
        req.setSchoolId(1L);
        req.setProvider("DARSMS");
        req.setApiKey("   ");

        School school = new School();
        school.setId(1L);
        school.setName("Test School");
        when(schoolRepo.findById(1L)).thenReturn(Optional.of(school));

        assertThrows(BadRequestException.class, () -> service.createConfiguration(req));
    }

    @Test
    void createConfigurationRejectsUnknownSchool() {
        CreateSmsConfigurationRequest req = new CreateSmsConfigurationRequest();
        req.setSchoolId(1L);
        req.setProvider("DARSMS");
        req.setApiKey("valid-key");
        when(schoolRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createConfiguration(req));
    }

    @Test
    void createConfigurationAcceptsOptionalApiSecret() {
        CreateSmsConfigurationRequest req = new CreateSmsConfigurationRequest();
        req.setSchoolId(1L);
        req.setProvider("DARSMS");
        req.setApiKey("valid-key");
        req.setApiSecret(null);
        School school = new School();
        school.setId(1L);
        school.setName("Test School");
        when(schoolRepo.findById(1L)).thenReturn(Optional.of(school));
        when(repo.existsBySchoolIdAndProviderAndDeletedAtIsNull(1L, "DARSMS")).thenReturn(false);
        when(encryption.encrypt("valid-key")).thenReturn("encrypted-key");
        SmsConfiguration saved = new SmsConfiguration();
        saved.setId(100L);
        saved.setSchoolId(1L);
        when(repo.save(any(SmsConfiguration.class))).thenReturn(saved);

        var dto = service.createConfiguration(req);

        assertNotNull(dto);
        assertEquals(1L, dto.getSchoolId());
        verify(encryption, times(1)).encrypt("valid-key");
        verify(encryption, never()).encrypt(null);
    }
}
