package com.edumanage.transport.repository;

import com.edumanage.transport.entity.TransportAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportAssignmentRepository extends JpaRepository<TransportAssignment, Long> {
    Page<TransportAssignment> findBySchoolId(Long schoolId, Pageable pageable);
    List<TransportAssignment> findBySchoolIdAndStatus(Long schoolId, String status);
    List<TransportAssignment> findBySchoolIdAndStudentId(Long schoolId, Long studentId);
    Page<TransportAssignment> findBySchoolIdAndStudentId(Long schoolId, Long studentId, Pageable pageable);
    List<TransportAssignment> findBySchoolIdAndRouteId(Long schoolId, Long routeId);
    List<TransportAssignment> findBySchoolIdAndVehicleId(Long schoolId, Long vehicleId);
    Optional<TransportAssignment> findBySchoolIdAndStudentIdAndRouteIdAndStatus(Long schoolId, Long studentId, Long routeId, String status);
    boolean existsBySchoolIdAndStudentIdAndRouteIdAndStatus(Long schoolId, Long studentId, Long routeId, String status);
    long countBySchoolIdAndVehicleIdAndStatus(Long schoolId, Long vehicleId, String status);
    List<TransportAssignment> findBySchoolIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long schoolId, java.time.LocalDate date, java.time.LocalDate date2);
}
