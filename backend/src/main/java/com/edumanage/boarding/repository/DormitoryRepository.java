package com.edumanage.boarding.repository;

import com.edumanage.boarding.entity.Dormitory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DormitoryRepository extends JpaRepository<Dormitory, Long> {
    Page<Dormitory> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Dormitory> findBySchoolIdAndId(Long schoolId, Long id);
    List<Dormitory> findBySchoolIdAndStatus(Long schoolId, String status);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
}
