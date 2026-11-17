package com.edumanage.examinationsubjects.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.examinationsubjects.dto.CreateExaminationSubjectRequest;
import com.edumanage.examinationsubjects.entity.ExaminationSubject;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.examinations.entity.Examination;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.subjects.entity.Subject;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.streamsubjects.repository.StreamSubjectAssignmentRepository;
import com.edumanage.gradingschemes.service.GradingSchemeService;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExaminationSubjectServiceTest {

    private ExaminationSubjectRepository examinationSubjectRepository;
    private ExaminationRepository examinationRepository;
    private SubjectRepository subjectRepository;
    private ClassRepository classRepository;
    private StreamRepository streamRepository;
    private AuditLogService auditLogService;
    private StreamSubjectAssignmentRepository streamSubjectAssignmentRepository;
    private GradingSchemeService gradingSchemeService;
    private ExaminationSubjectService service;

    private final Long schoolId = 7L;
    private final Long examinationId = 200L;
    private final Long academicYearId = 10L;
    private final Long schemeId = 60L;

    @BeforeEach
    void setUp() {
        examinationSubjectRepository = mock(ExaminationSubjectRepository.class);
        examinationRepository = mock(ExaminationRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        classRepository = mock(ClassRepository.class);
        streamRepository = mock(StreamRepository.class);
        auditLogService = mock(AuditLogService.class);
        streamSubjectAssignmentRepository = mock(StreamSubjectAssignmentRepository.class);
        gradingSchemeService = mock(GradingSchemeService.class);
        service = new ExaminationSubjectService(
                examinationSubjectRepository, examinationRepository, subjectRepository,
                classRepository, streamRepository, auditLogService, streamSubjectAssignmentRepository, gradingSchemeService);
        TenantContext.setTenantId(schoolId);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private void mockCommon(Long subjectId, Long classId) {
        Examination examination = new Examination();
        examination.setId(examinationId);
        examination.setSchoolId(schoolId);
        examination.setAcademicYearId(academicYearId);
        examination.setDeletedAt(null);
        when(examinationRepository.findById(examinationId)).thenReturn(Optional.of(examination));

        Subject subject = new Subject();
        subject.setId(subjectId);
        subject.setSchoolId(schoolId);
        subject.setStatus("ACTIVE");
        subject.setDeletedAt(null);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        Class clazz = new Class();
        clazz.setId(classId);
        clazz.setSchoolId(schoolId);
        clazz.setDeletedAt(null);
        when(classRepository.findById(classId)).thenReturn(Optional.of(clazz));

        when(examinationSubjectRepository.findByExaminationIdAndSubjectIdAndClassIdAndStreamId(
                examinationId, subjectId, classId, null)).thenReturn(Optional.empty());
    }

    private Stream stream(Long streamId, Long classId) {
        Stream stream = new Stream();
        stream.setId(streamId);
        stream.setSchoolId(schoolId);
        stream.setClassId(classId);
        stream.setStatus("ACTIVE");
        stream.setDeletedAt(null);
        return stream;
    }

    private CreateExaminationSubjectRequest request(Long schemeId, Integer maximumMarks) {
        CreateExaminationSubjectRequest request = new CreateExaminationSubjectRequest();
        request.setSubjectId(50L);
        request.setClassId(30L);
        request.setMaximumMarks(maximumMarks);
        request.setPassMark(20);
        request.setGradingSchemeId(schemeId);
        return request;
    }

    private CreateExaminationSubjectRequest requestWithStream(Long schemeId, Integer maximumMarks, Long streamId) {
        CreateExaminationSubjectRequest request = request(schemeId, maximumMarks);
        request.setStreamId(streamId);
        return request;
    }

    @Test
    void createAcceptsValidSubject() {
        mockCommon(50L, 30L);
        doNothing().when(gradingSchemeService).validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("100"));
        ExaminationSubject saved = new ExaminationSubject();
        saved.setId(1L);
        when(examinationSubjectRepository.save(any(ExaminationSubject.class))).thenReturn(saved);

        assertDoesNotThrow(() -> service.create(examinationId, request(schemeId, 100)));
        verify(examinationSubjectRepository).save(any(ExaminationSubject.class));
    }

    @Test
    void createAcceptsValidSubjectWithStream() {
        mockCommon(50L, 30L);
        Stream stream = stream(40L, 30L);
        when(streamRepository.findById(40L)).thenReturn(Optional.of(stream));
        when(streamSubjectAssignmentRepository.existsBySchoolIdAndClassIdAndStreamIdAndAcademicYearIdAndSubjectId(
                schoolId, 30L, 40L, academicYearId, 50L)).thenReturn(true);
        when(examinationSubjectRepository.findByExaminationIdAndSubjectIdAndClassIdAndStreamId(
                examinationId, 50L, 30L, 40L)).thenReturn(Optional.empty());
        doNothing().when(gradingSchemeService).validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("100"));
        ExaminationSubject saved = new ExaminationSubject();
        saved.setId(1L);
        when(examinationSubjectRepository.save(any(ExaminationSubject.class))).thenReturn(saved);

        assertDoesNotThrow(() -> service.create(examinationId, requestWithStream(schemeId, 100, 40L)));
        verify(examinationSubjectRepository).save(any(ExaminationSubject.class));
    }

    @Test
    void createRejectsInvalidExaminationId() {
        when(examinationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(999L, request(schemeId, 100)));
        verify(examinationSubjectRepository, never()).save(any(ExaminationSubject.class));
    }

    @Test
    void createRejectsStreamFromAnotherClass() {
        mockCommon(50L, 30L);
        Stream stream = stream(40L, 99L);
        when(streamRepository.findById(40L)).thenReturn(Optional.of(stream));

        assertThrows(BadRequestException.class,
                () -> service.create(examinationId, requestWithStream(schemeId, 100, 40L)));
        verify(examinationSubjectRepository, never()).save(any(ExaminationSubject.class));
    }

    @Test
    void createRejectsStreamFromAnotherSchool() {
        mockCommon(50L, 30L);
        Stream stream = stream(40L, 30L);
        stream.setSchoolId(99L);
        when(streamRepository.findById(40L)).thenReturn(Optional.of(stream));

        assertThrows(ForbiddenException.class,
                () -> service.create(examinationId, requestWithStream(schemeId, 100, 40L)));
        verify(examinationSubjectRepository, never()).save(any(ExaminationSubject.class));
    }

    @Test
    void createRejectsDuplicateSubjectAssignment() {
        mockCommon(50L, 30L);
        ExaminationSubject existing = new ExaminationSubject();
        existing.setId(1L);
        existing.setExaminationId(examinationId);
        existing.setDeletedAt(null);
        when(examinationSubjectRepository.findByExaminationIdAndSubjectIdAndClassIdAndStreamId(
                examinationId, 50L, 30L, null)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> service.create(examinationId, request(schemeId, 100)));
        verify(examinationSubjectRepository, never()).save(any(ExaminationSubject.class));
    }

    @Test
    void createRejectsGradingSchemeWhoseRangeExceedsMaximumMarks() {
        mockCommon(50L, 30L);
        doThrow(new BadRequestException("maximum mark 100 exceeds the examination maximum marks 50"))
                .when(gradingSchemeService).validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50"));

        assertThrows(BadRequestException.class,
                () -> service.create(examinationId, request(schemeId, 50)));
        verify(examinationSubjectRepository, never()).save(any(ExaminationSubject.class));
    }

    @Test
    void createAcceptsCompatibleGradingScheme() {
        mockCommon(50L, 30L);
        doNothing().when(gradingSchemeService).validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50"));
        ExaminationSubject saved = new ExaminationSubject();
        saved.setId(1L);
        when(examinationSubjectRepository.save(any(ExaminationSubject.class))).thenReturn(saved);

        assertDoesNotThrow(() -> service.create(examinationId, request(schemeId, 50)));
        verify(examinationSubjectRepository).save(any(ExaminationSubject.class));
    }

    @Test
    void createRejectsGradingSchemeBelongingToOtherSchool() {
        mockCommon(50L, 30L);
        doThrow(new ForbiddenException("Grading scheme does not belong to this school"))
                .when(gradingSchemeService).validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50"));

        assertThrows(ForbiddenException.class,
                () -> service.create(examinationId, request(schemeId, 50)));
        verify(examinationSubjectRepository, never()).save(any(ExaminationSubject.class));
    }

    @Test
    void updateRejectsGradingSchemeWhoseRangeExceedsMaximumMarks() {
        mockCommon(50L, 30L);
        ExaminationSubject existing = new ExaminationSubject();
        existing.setId(1L);
        existing.setExaminationId(examinationId);
        existing.setDeletedAt(null);
        when(examinationSubjectRepository.findById(1L)).thenReturn(Optional.of(existing));
        doThrow(new BadRequestException("maximum mark 100 exceeds the examination maximum marks 50"))
                .when(gradingSchemeService).validateRangesAgainstMaximumMarks(schemeId, new BigDecimal("50"));

        assertThrows(BadRequestException.class,
                () -> service.update(1L, request(schemeId, 50)));
        verify(examinationSubjectRepository, never()).save(any(ExaminationSubject.class));
    }
}
