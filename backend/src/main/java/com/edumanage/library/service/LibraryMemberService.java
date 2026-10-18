package com.edumanage.library.service;

import com.edumanage.exception.ForbiddenException;
import com.edumanage.exception.ResourceNotFoundException;
import com.edumanage.library.dto.CreateLibraryMemberRequest;
import com.edumanage.library.dto.LibraryMemberDto;
import com.edumanage.library.entity.LibraryMember;
import com.edumanage.library.repository.LibraryMemberRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.tenant.TenantContext;
import com.edumanage.library.util.LibraryFeatureGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LibraryMemberService {

    private final LibraryMemberRepository libraryMemberRepository;
    private final AuditLogService auditLogService;

    public LibraryMemberService(LibraryMemberRepository libraryMemberRepository, AuditLogService auditLogService) {
        this.libraryMemberRepository = libraryMemberRepository;
        this.auditLogService = auditLogService;
    }

    public PageResponse<LibraryMemberDto> list(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        Page<LibraryMember> page = libraryMemberRepository.findBySchoolId(tenantId, pageable);
        return PageResponse.from(page.map(this::mapToDto));
    }

    public List<LibraryMemberDto> listByMemberType(String memberType) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        return libraryMemberRepository.findBySchoolIdAndMemberType(tenantId, memberType).stream()
                .map(this::mapToDto)
                .toList();
    }

    public LibraryMemberDto get(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);
        LibraryMember member = libraryMemberRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Library member not found"));
        return mapToDto(member);
    }

    @Transactional
    public LibraryMemberDto create(CreateLibraryMemberRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        if (libraryMemberRepository.existsBySchoolIdAndMembershipNumber(tenantId, request.getMembershipNumber())) {
            throw new com.edumanage.exception.ConflictException("Membership number already exists");
        }

        LibraryMember member = new LibraryMember();
        member.setSchoolId(tenantId);
        member.setMemberType(request.getMemberType());
        member.setMemberId(request.getMemberId());
        member.setMembershipNumber(request.getMembershipNumber());
        member.setJoinedDate(request.getJoinedDate());
        member.setMembershipExpiry(request.getMembershipExpiry());
        member.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        libraryMemberRepository.save(member);
        auditLogService.log("LIBRARY_MEMBER_CREATE", "LibraryMember", member.getId(),
                "Created library member: " + request.getMembershipNumber(), null, null, null);
        return mapToDto(member);
    }

    @Transactional
    public LibraryMemberDto update(Long id, CreateLibraryMemberRequest request) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        LibraryMember member = libraryMemberRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Library member not found"));

        if (libraryMemberRepository.existsBySchoolIdAndMembershipNumberAndIdNot(tenantId, request.getMembershipNumber(), id)) {
            throw new com.edumanage.exception.ConflictException("Membership number already exists");
        }

        member.setMemberType(request.getMemberType());
        member.setMemberId(request.getMemberId());
        member.setMembershipNumber(request.getMembershipNumber());
        member.setJoinedDate(request.getJoinedDate());
        member.setMembershipExpiry(request.getMembershipExpiry());
        if (request.getStatus() != null) {
            member.setStatus(request.getStatus());
        }

        libraryMemberRepository.save(member);
        auditLogService.log("LIBRARY_MEMBER_UPDATE", "LibraryMember", member.getId(),
                "Updated library member: " + request.getMembershipNumber(), null, null, null);
        return mapToDto(member);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        LibraryFeatureGuard.checkLibraryFeature(tenantId);

        LibraryMember member = libraryMemberRepository.findBySchoolIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Library member not found"));
        member.setDeletedAt(java.time.LocalDateTime.now());
        libraryMemberRepository.save(member);
        auditLogService.log("LIBRARY_MEMBER_DELETE", "LibraryMember", member.getId(),
                "Deleted library member: " + member.getMembershipNumber(), null, null, null);
    }

    private LibraryMemberDto mapToDto(LibraryMember entity) {
        LibraryMemberDto dto = new LibraryMemberDto();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid() != null ? entity.getUuid().toString() : null);
        dto.setSchoolId(entity.getSchoolId());
        dto.setMemberType(entity.getMemberType());
        dto.setMemberId(entity.getMemberId());
        dto.setMembershipNumber(entity.getMembershipNumber());
        dto.setJoinedDate(entity.getJoinedDate());
        dto.setMembershipExpiry(entity.getMembershipExpiry());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
