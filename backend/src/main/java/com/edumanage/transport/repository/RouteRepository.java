package com.edumanage.transport.repository;

import com.edumanage.transport.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    Page<Route> findBySchoolId(Long schoolId, Pageable pageable);
    List<Route> findBySchoolIdAndStatus(Long schoolId, String status);
    Optional<Route> findBySchoolIdAndName(Long schoolId, String name);
    boolean existsBySchoolIdAndNameAndIdNot(Long schoolId, String name, Long id);
}
