package com.edumanage.transport.repository;

import com.edumanage.transport.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Page<Vehicle> findBySchoolId(Long schoolId, Pageable pageable);
    List<Vehicle> findBySchoolIdAndStatus(Long schoolId, String status);
    Optional<Vehicle> findBySchoolIdAndRegistrationNumber(Long schoolId, String registrationNumber);
    boolean existsBySchoolIdAndRegistrationNumberAndIdNot(Long schoolId, String registrationNumber, Long id);
}
