package com.edumanage.boarding.repository;

import com.edumanage.boarding.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Page<Room> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<Room> findBySchoolIdAndId(Long schoolId, Long id);
    List<Room> findBySchoolIdAndDormitoryId(Long schoolId, Long dormitoryId);
    boolean existsBySchoolIdAndDormitoryIdAndRoomNumber(Long schoolId, Long dormitoryId, String roomNumber);
    long countBySchoolIdAndDormitoryIdAndId(Long schoolId, Long dormitoryId, Long roomId);
}
