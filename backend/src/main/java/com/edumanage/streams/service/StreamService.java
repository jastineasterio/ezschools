package com.edumanage.streams.service;

import com.edumanage.exception.ConflictException;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.streams.dto.CreateStreamRequest;
import com.edumanage.streams.dto.StreamDto;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.classes.repository.ClassRepository;
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
public class StreamService {

    private final StreamRepository streamRepository;
    private final ClassRepository classRepository;
    private final AuditLogService auditLogService;

    public StreamService(StreamRepository streamRepository, ClassRepository classRepository,
                         AuditLogService auditLogService) {
        this.streamRepository = streamRepository;
        this.classRepository = classRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<StreamDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<Stream> page;
        if (tenantId != null) {
            page = streamRepository.findBySchoolId(tenantId, pageable);
        } else {
            page = streamRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(stream -> mapToDto(stream, null)));
    }

    public List<StreamDto> listByClass(Long classId) {
        var clazz = classRepository.findById(classId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return streamRepository.findByClassIdAndSchoolId(classId, clazz.getSchoolId()).stream()
                .map(stream -> mapToDto(stream, clazz.getName()))
                .collect(Collectors.toList());
    }

    public StreamDto getById(Long id) {
        var stream = streamRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(stream.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        return mapToDto(stream, null);
    }

    @Transactional
    public StreamDto create(CreateStreamRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Access denied");
        }

        var clazz = classRepository.findById(request.getClassId())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        if (!tenantId.equals(clazz.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (streamRepository.existsByClassIdAndName(request.getClassId(), request.getName())) {
            throw new ConflictException("Stream name already exists for this class");
        }
        if (request.getCode() != null && streamRepository.existsBySchoolIdAndClassIdAndCode(tenantId, request.getClassId(), request.getCode())) {
            throw new ConflictException("Stream code already exists for this class");
        }

        Stream stream = new Stream();
        stream.setSchoolId(tenantId);
        stream.setClassId(request.getClassId());
        stream.setName(request.getName());
        stream.setCode(request.getCode());
        stream.setCapacity(request.getCapacity());
        stream.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        streamRepository.save(stream);

        auditLogService.log("STREAM_CREATE", "Stream", stream.getId(),
                "Created stream " + stream.getName(), null, null, null);

        return mapToDto(stream, clazz.getName());
    }

    @Transactional
    public StreamDto update(Long id, CreateStreamRequest request) {
        var stream = streamRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(stream.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        if (!stream.getClassId().equals(request.getClassId())) {
            var clazz = classRepository.findById(request.getClassId())
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
            if (!tenantId.equals(clazz.getSchoolId())) {
                throw new ForbiddenException("Access denied");
            }
        }

        if (!stream.getName().equals(request.getName()) &&
                streamRepository.existsByClassIdAndName(request.getClassId(), request.getName())) {
            throw new ConflictException("Stream name already exists for this class");
        }
        if (request.getCode() != null && !request.getCode().equals(stream.getCode()) &&
                streamRepository.existsBySchoolIdAndClassIdAndCode(stream.getSchoolId(), request.getClassId(), request.getCode())) {
            throw new ConflictException("Stream code already exists for this class");
        }

        stream.setClassId(request.getClassId());
        stream.setName(request.getName());
        if (request.getCode() != null) {
            stream.setCode(request.getCode());
        }
        stream.setCapacity(request.getCapacity());
        if (request.getStatus() != null) {
            stream.setStatus(request.getStatus());
        }

        streamRepository.save(stream);

        auditLogService.log("STREAM_UPDATE", "Stream", stream.getId(),
                "Updated stream " + stream.getName(), null, null, null);

        return mapToDto(stream, null);
    }

    @Transactional
    public StreamDto updateStatus(Long id, String status) {
        var stream = streamRepository.findById(id)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found"));

        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(stream.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        stream.setStatus(status);
        streamRepository.save(stream);

        auditLogService.log("STREAM_UPDATE_STATUS", "Stream", stream.getId(),
                "Updated stream " + stream.getName() + " status to " + status, null, null, null);

        return mapToDto(stream, null);
    }

    private StreamDto mapToDto(Stream entity, String className) {
        StreamDto dto = new StreamDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setClassId(entity.getClassId());
        dto.setClassName(className);
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setCapacity(entity.getCapacity());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
