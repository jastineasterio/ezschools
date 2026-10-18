package com.edumanage.streams.repository;

import com.edumanage.streams.entity.Stream;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StreamRepository extends JpaRepository<Stream, Long> {
    Page<Stream> findBySchoolId(Long schoolId, Pageable pageable);
    List<Stream> findByClassIdAndSchoolId(Long classId, Long schoolId);
    Optional<Stream> findBySchoolIdAndId(Long schoolId, Long id);
    boolean existsByClassIdAndName(Long classId, String name);
    boolean existsBySchoolIdAndClassIdAndCode(Long schoolId, Long classId, String code);
}
