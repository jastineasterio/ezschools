package com.edumanage.transport.repository;

import com.edumanage.transport.entity.RouteStop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    List<RouteStop> findByRouteIdAndStatus(Long routeId, String status);
    Page<RouteStop> findByRouteId(Long routeId, Pageable pageable);
    List<RouteStop> findByRouteIdAndSchoolId(Long routeId, Long schoolId);
    long countByRouteIdAndStatus(Long routeId, String status);
    boolean existsByRouteIdAndNameAndIdNot(Long routeId, String name, Long id);
}
