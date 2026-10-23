package com.edumanage.streams.service;

import com.edumanage.audit.service.AuditLogService;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.exception.ForbiddenException;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StreamServiceTest {

    private StreamRepository streamRepository;
    private ClassRepository classRepository;
    private StreamService streamService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContext.setTenantId(7L);
        streamRepository = mock(StreamRepository.class);
        classRepository = mock(ClassRepository.class);
        streamService = new StreamService(
                streamRepository, classRepository, mock(AuditLogService.class));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void listsStreamsForTheCurrentTenantWithSearchAndPagination() {
        var pageable = PageRequest.of(1, 5);
        Stream stream = new Stream();
        stream.setId(12L);
        stream.setSchoolId(7L);
        stream.setClassId(4L);
        stream.setName("Blue");
        stream.setCode("BL");
        Class schoolClass = new Class();
        schoolClass.setName("Form 1");

        when(streamRepository.search(7L, "Blue", pageable))
                .thenReturn(new PageImpl<>(List.of(stream), pageable, 6));
        when(classRepository.findBySchoolIdAndId(7L, 4L)).thenReturn(Optional.of(schoolClass));

        var result = streamService.list(pageable, " Blue ");

        assertEquals(1, result.getContent().size());
        assertEquals(12L, result.getContent().get(0).getId());
        assertEquals("Form 1", result.getContent().get(0).getClassName());
        assertEquals(1, result.getPageNumber());
        assertEquals(5, result.getPageSize());
        assertEquals(6, result.getTotalElements());
        verify(streamRepository).search(7L, "Blue", pageable);
        verify(classRepository).findBySchoolIdAndId(7L, 4L);
    }

    @Test
    void rejectsSchoolListingWithoutAValidTenantAssignment() {
        TenantContext.clear();

        assertThrows(ForbiddenException.class,
                () -> streamService.list(PageRequest.of(0, 10), null));

        verifyNoInteractions(streamRepository, classRepository);
    }
}
