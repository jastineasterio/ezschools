package com.edumanage.examinationsubjects.service;

import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.examinationsubjects.dto.CreateExaminationSubjectRequest;
import com.edumanage.examinationsubjects.dto.ExaminationSubjectDto;
import com.edumanage.examinationsubjects.entity.ExaminationSubject;
import com.edumanage.examinationsubjects.repository.ExaminationSubjectRepository;
import com.edumanage.examinations.repository.ExaminationRepository;
import com.edumanage.subjects.repository.SubjectRepository;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExaminationSubjectService {

    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final ExaminationRepository examinationRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;
    private final StreamRepository streamRepository;
    private final AuditLogService auditLogService;

    public ExaminationSubjectService(ExaminationSubjectRepository examinationSubjectRepository,
                                     ExaminationRepository examinationRepository,
                                     SubjectRepository subjectRepository,
                                     ClassRepository classRepository,
                                     StreamRepository streamRepository,
                                     AuditLogService auditLogService) {
        this.examinationSubjectRepository = examinationSubjectRepository;
        this.examinationRepository = examinationRepository;
        this.subjectRepository = subjectRepository;
        this.classRepository = classRepository;
        this.streamRepository = streamRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<ExaminationSubjectDto> listByExamination(Long examinationId, Pageable pageable) {
        var examination = examinationRepository.findById(examinationId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(examination.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        Page<ExaminationSubject> page = examinationSubjectRepository.findByExaminationId(examinationId, pageable);
        return PageResponse.from(page.map(subject -> mapToDto(subject, examination.getName(), null, null, null)));
    }

    public List<ExaminationSubjectDto> listAllByExamination(Long examinationId) {
        var examination = examinationRepository.findById(examinationId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(examination.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return examinationSubjectRepository.findByExaminationId(examinationId).stream()
                .map(subject -> mapToDto(subject, examination.getName(), null, null, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public ExaminationSubjectDto create(Long examinationId, CreateExaminationSubjectRequest request) {
        var examination = examinationRepository.findById(examinationId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found"));

        Long tenantId = examination.getSchoolId();
        if (TenantContext.getTenantId() != null && !TenantContext.getTenantId().equals(tenantId)) {
            throw new ForbiddenException("Access denied");
        }

        var subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        if (!tenantId.equals(subject.getSchoolId())) {
            throw new ForbiddenException("Subject does not belong to this school");
        }

        var clazz = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!tenantId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Class does not belong to this school");
        }

        if (request.getStreamId() != null) {
            var stream = streamRepository.findById(request.getStreamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (!tenantId.equals(stream.getSchoolId())) {
                throw new ForbiddenException("Stream does not belong to this school");
            }
        }

        if (examinationSubjectRepository.findByExaminationIdAndSubjectIdAndClassIdAndStreamId(
                examinationId, request.getSubjectId(), request.getClassId(), request.getStreamId()).isPresent()) {
            throw new ConflictException("Examination subject already exists");
        }

        ExaminationSubject examinationSubject = new ExaminationSubject();
        examinationSubject.setExaminationId(examinationId);
        examinationSubject.setSubjectId(request.getSubjectId());
        examinationSubject.setClassId(request.getClassId());
        examinationSubject.setStreamId(request.getStreamId());
        examinationSubject.setMaximumMarks(new java.math.BigDecimal(request.getMaximumMarks()));
        examinationSubject.setPassMark(new java.math.BigDecimal(request.getPassMark()));
        examinationSubject.setGradingSchemeId(request.getGradingSchemeId());

        examinationSubjectRepository.save(examinationSubject);

        auditLogService.log("EXAMINATION_SUBJECT_CREATE", "ExaminationSubject", examinationSubject.getId(),
                "Created examination subject", null, null, null);

        return mapToDto(examinationSubject, examination.getName(), subject.getName(), clazz.getName(), null);
    }

    @Transactional
    public ExaminationSubjectDto update(Long id, CreateExaminationSubjectRequest request) {
        var examinationSubject = examinationSubjectRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination subject not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            var examination = examinationRepository.findById(examinationSubject.getExaminationId()).orElse(null);
            if (examination == null || !tenantId.equals(examination.getSchoolId())) {
                throw new ForbiddenException("Access denied");
            }
        }

        var subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        var examination = examinationRepository.findById(examinationSubject.getExaminationId()).orElse(null);
        if (examination != null && !examination.getSchoolId().equals(subject.getSchoolId())) {
            throw new ForbiddenException("Subject does not belong to this school");
        }

        var clazz = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (examination != null && !examination.getSchoolId().equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Class does not belong to this school");
        }

        if (request.getStreamId() != null) {
            var stream = streamRepository.findById(request.getStreamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));
            if (examination != null && !examination.getSchoolId().equals(stream.getSchoolId())) {
                throw new ForbiddenException("Stream does not belong to this school");
            }
        }

        examinationSubject.setSubjectId(request.getSubjectId());
        examinationSubject.setClassId(request.getClassId());
        examinationSubject.setStreamId(request.getStreamId());
        examinationSubject.setMaximumMarks(new java.math.BigDecimal(request.getMaximumMarks()));
        examinationSubject.setPassMark(new java.math.BigDecimal(request.getPassMark()));
        examinationSubject.setGradingSchemeId(request.getGradingSchemeId());

        examinationSubjectRepository.save(examinationSubject);

        auditLogService.log("EXAMINATION_SUBJECT_UPDATE", "ExaminationSubject", examinationSubject.getId(),
                "Updated examination subject", null, null, null);

        return mapToDto(examinationSubject, examination != null ? examination.getName() : null, subject.getName(), clazz.getName(), null);
    }

    @Transactional
    public void delete(Long id) {
        var examinationSubject = examinationSubjectRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Examination subject not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            var examination = examinationRepository.findById(examinationSubject.getExaminationId()).orElse(null);
            if (examination == null || !tenantId.equals(examination.getSchoolId())) {
                throw new ForbiddenException("Access denied");
            }
        }

        examinationSubject.setDeletedAt(java.time.LocalDateTime.now());
        examinationSubjectRepository.save(examinationSubject);

        auditLogService.log("EXAMINATION_SUBJECT_DELETE", "ExaminationSubject", examinationSubject.getId(),
                "Deleted examination subject", null, null, null);
    }

    private ExaminationSubjectDto mapToDto(ExaminationSubject entity, String examinationName,
                                           String subjectName, String className, String streamName) {
        ExaminationSubjectDto dto = new ExaminationSubjectDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setExaminationId(entity.getExaminationId());
        dto.setExaminationName(examinationName);
        dto.setSubjectId(entity.getSubjectId());
        dto.setSubjectName(subjectName);
        dto.setClassId(entity.getClassId());
        dto.setClassName(className);
        dto.setStreamId(entity.getStreamId());
        dto.setStreamName(streamName);
        dto.setMaximumMarks(entity.getMaximumMarks() != null ? entity.getMaximumMarks().intValue() : null);
        dto.setPassMark(entity.getPassMark() != null ? entity.getPassMark().intValue() : null);
        dto.setGradingSchemeId(entity.getGradingSchemeId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
