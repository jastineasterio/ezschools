package com.edumanage.fees.service;

import com.edumanage.fees.dto.BulkFeeAssignmentPreviewDto;
import com.edumanage.fees.dto.BulkFeeAssignmentRequest;
import com.edumanage.fees.dto.BulkFeeAssignmentResultDto;
import com.edumanage.fees.entity.FeeAssignment;
import com.edumanage.fees.entity.FeeStructure;
import com.edumanage.fees.repository.FeeAssignmentRepository;
import com.edumanage.fees.repository.FeeStructureRepository;
import com.edumanage.academicyears.entity.AcademicYear;
import com.edumanage.academicyears.repository.AcademicYearRepository;
import com.edumanage.classes.entity.Class;
import com.edumanage.classes.repository.ClassRepository;
import com.edumanage.studentprofiles.entity.StudentProfile;
import com.edumanage.studentprofiles.repository.StudentProfileRepository;
import com.edumanage.streams.entity.Stream;
import com.edumanage.streams.repository.StreamRepository;
import com.edumanage.enrollments.entity.StudentEnrollment;
import com.edumanage.enrollments.repository.StudentEnrollmentRepository;
import com.edumanage.audit.service.AuditLogService;
import com.edumanage.security.AccessGuard;
import com.edumanage.tenant.TenantContext;
import com.edumanage.exception.BadRequestException;
import com.edumanage.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FeeAssignmentServiceTest {

    private FeeAssignmentRepository feeAssignmentRepository;
    private FeeStructureRepository feeStructureRepository;
    private StudentProfileRepository studentProfileRepository;
    private StudentEnrollmentRepository studentEnrollmentRepository;
    private ClassRepository classRepository;
    private StreamRepository streamRepository;
    private AcademicYearRepository academicYearRepository;
    private AuditLogService auditLogService;
    private AccessGuard accessGuard;
    private FeeAssignmentService service;

    @BeforeEach
    void setUp() {
        feeAssignmentRepository = mock(FeeAssignmentRepository.class);
        feeStructureRepository = mock(FeeStructureRepository.class);
        studentProfileRepository = mock(StudentProfileRepository.class);
        studentEnrollmentRepository = mock(StudentEnrollmentRepository.class);
        classRepository = mock(ClassRepository.class);
        streamRepository = mock(StreamRepository.class);
        academicYearRepository = mock(AcademicYearRepository.class);
        auditLogService = mock(AuditLogService.class);
        accessGuard = mock(AccessGuard.class);
        service = new FeeAssignmentService(
                feeAssignmentRepository, feeStructureRepository, studentProfileRepository,
                studentEnrollmentRepository, classRepository, streamRepository,
                academicYearRepository, auditLogService, accessGuard);
        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private BulkFeeAssignmentRequest request(Long classId, Long streamId, Long academicYearId, List<Long> feeStructureIds, String scope) {
        BulkFeeAssignmentRequest r = new BulkFeeAssignmentRequest();
        r.setScope(scope);
        r.setClassId(classId);
        r.setStreamId(streamId);
        r.setAcademicYearId(academicYearId);
        r.setFeeStructureIds(feeStructureIds);
        r.setDueDate(null);
        return r;
    }

    private StudentProfile student(Long id, String admission, String first, String last) {
        StudentProfile s = new StudentProfile();
        s.setId(id);
        s.setSchoolId(1L);
        s.setAdmissionNumber(admission);
        s.setFirstName(first);
        s.setLastName(last);
        s.setStatus("ACTIVE");
        return s;
    }

    private StudentEnrollment enrol(Long studentId, Long classId, Long academicYearId, Long streamId) {
        StudentEnrollment e = new StudentEnrollment();
        e.setStudentId(studentId);
        e.setSchoolId(1L);
        e.setClassId(classId);
        e.setAcademicYearId(academicYearId);
        e.setStreamId(streamId);
        e.setStatus("ENROLLED");
        return e;
    }

    private Class clazz(Long id) {
        Class c = new Class();
        c.setId(id);
        c.setSchoolId(1L);
        return c;
    }

    private Stream stream(Long id, Long classId) {
        Stream s = new Stream();
        s.setId(id);
        s.setSchoolId(1L);
        s.setClassId(classId);
        return s;
    }

    private AcademicYear year(Long id) {
        AcademicYear y = new AcademicYear();
        y.setId(id);
        y.setSchoolId(1L);
        return y;
    }

    private FeeStructure structure(Long id, BigDecimal amount) {
        FeeStructure f = new FeeStructure();
        f.setId(id);
        f.setSchoolId(1L);
        f.setName("Tuition");
        f.setAmount(amount);
        return f;
    }

    private void givenClassYearStructure() {
        when(classRepository.findBySchoolIdAndId(1L, 10L)).thenReturn(Optional.of(clazz(10L)));
        when(academicYearRepository.findById(100L)).thenReturn(Optional.of(year(100L)));
        when(feeStructureRepository.findBySchoolIdAndId(1L, 50L)).thenReturn(Optional.of(structure(50L, new BigDecimal("100.00"))));
    }

    @Test
    void testPreviewClassScopeAllStreams() {
        givenClassYearStructure();
        List<StudentProfile> students = List.of(student(1L, "A001", "A", "One"), student(2L, "A002", "B", "Two"), student(3L, "A003", "C", "Three"));
        List<StudentEnrollment> enrols = students.stream().map(s -> enrol(s.getId(), 10L, 100L, null)).toList();
        when(studentEnrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(1L, 10L, 100L, "ENROLLED")).thenReturn(enrols);
        when(studentEnrollmentRepository.findBySchoolIdAndClassId(1L, 10L)).thenReturn(enrols);
        when(studentProfileRepository.findBySchoolIdAndIdIn(any(), anyList())).thenReturn(students);

        BulkFeeAssignmentPreviewDto preview = service.previewBulkAssignment(request(10L, null, 100L, List.of(50L), "CLASS"));

        assertEquals(3, preview.getEligibleStudentCount());
        assertEquals(3, preview.getTotalAssignments());
        assertEquals(new BigDecimal("300.00"), preview.getTotalAmount());
        assertEquals(3, preview.getStudents().size());
        assertEquals("A001", preview.getStudents().get(0).getAdmissionNumber());
        assertEquals("A One", preview.getStudents().get(0).getName());
    }

    @Test
    void testPreviewSpecificStreamFiltersStudents() {
        givenClassYearStructure();
        when(streamRepository.findBySchoolIdAndId(1L, 20L)).thenReturn(Optional.of(stream(20L, 10L)));
        StudentProfile s1 = student(1L, "A001", "A", "One");
        StudentProfile s2 = student(2L, "A002", "B", "Two");
        List<StudentEnrollment> enrols = List.of(enrol(1L, 10L, 100L, 20L), enrol(2L, 10L, 100L, 20L));
        when(studentEnrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(1L, 10L, 100L, "ENROLLED")).thenReturn(enrols);
        when(studentProfileRepository.findBySchoolIdAndIdIn(any(), anyList())).thenReturn(List.of(s1, s2));

        BulkFeeAssignmentPreviewDto preview = service.previewBulkAssignment(request(10L, 20L, 100L, List.of(50L), "STREAM"));

        assertEquals(2, preview.getEligibleStudentCount());
        assertEquals(2, preview.getTotalAssignments());
        verify(streamRepository).findBySchoolIdAndId(1L, 20L);
    }

    @Test
    void testPreviewAcademicYearFiltersEnrollments() {
        givenClassYearStructure();
        List<StudentProfile> students = List.of(student(1L, "A001", "A", "One"));
        List<StudentEnrollment> enrols = List.of(enrol(1L, 10L, 100L, null));
        when(studentEnrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(1L, 10L, 100L, "ENROLLED")).thenReturn(enrols);
        when(studentProfileRepository.findBySchoolIdAndIdIn(any(), anyList())).thenReturn(students);

        BulkFeeAssignmentPreviewDto preview = service.previewBulkAssignment(request(10L, null, 100L, List.of(50L), "CLASS"));

        assertEquals(1, preview.getEligibleStudentCount());
        assertEquals(1, preview.getTotalAssignments());
        verify(studentEnrollmentRepository).findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(1L, 10L, 100L, "ENROLLED");
    }

    @Test
    void testPreviewNoEligibleStudents() {
        givenClassYearStructure();
        when(studentEnrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(1L, 10L, 100L, "ENROLLED")).thenReturn(List.of());
        when(studentEnrollmentRepository.findBySchoolIdAndClassId(1L, 10L)).thenReturn(List.of());

        BulkFeeAssignmentPreviewDto preview = service.previewBulkAssignment(request(10L, null, 100L, List.of(50L), "CLASS"));

        assertEquals(0, preview.getEligibleStudentCount());
        assertEquals(0, preview.getTotalAssignments());
        assertTrue(preview.getStudents().isEmpty());
    }

    @Test
    void testBulkAssignSkipsDuplicates() {
        givenClassYearStructure();
        List<StudentProfile> students = List.of(student(1L, "A001", "A", "One"), student(2L, "A002", "B", "Two"), student(3L, "A003", "C", "Three"));
        List<StudentEnrollment> enrols = students.stream().map(s -> enrol(s.getId(), 10L, 100L, null)).toList();
        when(studentEnrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(1L, 10L, 100L, "ENROLLED")).thenReturn(enrols);
        when(studentProfileRepository.findBySchoolIdAndIdIn(any(), anyList())).thenReturn(students);
        FeeAssignment existing = new FeeAssignment();
        existing.setStudentId(1L);
        existing.setFeeStructureId(50L);
        when(feeAssignmentRepository.findBySchoolIdAndStudentIdInAndFeeStructureIdIn(eq(1L), anyList(), anyList())).thenReturn(List.of(existing));
        when(feeAssignmentRepository.saveAll(anyList())).thenReturn(List.of());

        BulkFeeAssignmentResultDto result = service.bulkAssign(request(10L, null, 100L, List.of(50L), "CLASS"));

        assertEquals(2, result.getAssigned());
        assertEquals(1, result.getSkipped());
        assertEquals(0, result.getFailed());
        verify(feeAssignmentRepository).saveAll(anyList());
    }

    @Test
    void testSuccessfulPreviewAndSubmissionPreservesBalances() {
        givenClassYearStructure();
        List<StudentProfile> students = List.of(student(1L, "A001", "A", "One"), student(2L, "A002", "B", "Two"));
        List<StudentEnrollment> enrols = students.stream().map(s -> enrol(s.getId(), 10L, 100L, null)).toList();
        when(studentEnrollmentRepository.findBySchoolIdAndClassIdAndAcademicYearIdAndStatusAndDeletedAtIsNull(1L, 10L, 100L, "ENROLLED")).thenReturn(enrols);
        when(studentProfileRepository.findBySchoolIdAndIdIn(any(), anyList())).thenReturn(students);
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        when(feeAssignmentRepository.saveAll(captor.capture())).thenReturn(List.of());

        BulkFeeAssignmentResultDto result = service.bulkAssign(request(10L, null, 100L, List.of(50L), "CLASS"));

        assertEquals(2, result.getAssigned());
        assertEquals(0, result.getSkipped());
        assertEquals(0, result.getFailed());
        List<FeeAssignment> saved = captor.getValue();
        assertEquals(2, saved.size());
        for (FeeAssignment a : saved) {
            assertEquals(1L, a.getSchoolId());
            assertEquals(new BigDecimal("100.00"), a.getAmountDue());
            assertEquals(BigDecimal.ZERO, a.getAmountPaid());
            assertEquals(new BigDecimal("100.00"), a.getBalance());
            assertEquals("PENDING", a.getStatus());
        }
    }

    @Test
    void testFeeStructureNotInSchoolThrows() {
        when(classRepository.findBySchoolIdAndId(1L, 10L)).thenReturn(Optional.of(clazz(10L)));
        when(feeStructureRepository.findBySchoolIdAndId(1L, 50L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.previewBulkAssignment(request(10L, null, 100L, List.of(50L), "CLASS")));
    }

    @Test
    void testClassNotInSchoolThrows() {
        when(classRepository.findBySchoolIdAndId(1L, 10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.previewBulkAssignment(request(10L, null, 100L, List.of(50L), "CLASS")));
    }

    @Test
    void testStreamScopeRequiresStream() {
        assertThrows(BadRequestException.class,
                () -> service.previewBulkAssignment(request(10L, null, 100L, List.of(50L), "STREAM")));
    }

    @Test
    void testInvalidScopeThrows() {
        assertThrows(BadRequestException.class,
                () -> service.previewBulkAssignment(request(10L, null, 100L, List.of(50L), "INVALID")));
    }

    @Test
    void testStreamNotBelongingToClassThrows() {
        givenClassYearStructure();
        when(streamRepository.findBySchoolIdAndId(1L, 20L)).thenReturn(Optional.of(stream(20L, 99L)));
        assertThrows(BadRequestException.class,
                () -> service.previewBulkAssignment(request(10L, 20L, 100L, List.of(50L), "STREAM")));
    }
}
