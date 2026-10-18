package com.edumanage.boarding.repository;

import com.edumanage.boarding.entity.Bed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {
    Page<Bed> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Bed> findBySchoolIdAndId(Long schoolId, Long id);
    List<Bed> findBySchoolIdAndRoomId(Long schoolId, Long roomId);
    List<Bed> findBySchoolIdAndStudentId(Long schoolId, Long studentId);
    boolean existsBySchoolIdAndRoomIdAndBedNumber(Long schoolId, Long roomId, String bedNumber);
    boolean existsBySchoolIdAndStudentIdAndStatus(Long schoolId, Long studentId, String status);
}
