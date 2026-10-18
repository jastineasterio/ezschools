package com.edumanage.library.repository;

import com.edumanage.library.entity.LibraryMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryMemberRepository extends JpaRepository<LibraryMember, Long> {
    Page<LibraryMember> findBySchoolId(Long schoolId, Pageable pageable);
    List<LibraryMember> findBySchoolIdAndMemberType(Long schoolId, String memberType);
    List<LibraryMember> findBySchoolIdAndStatus(Long schoolId, String status);
    Optional<LibraryMember> findBySchoolIdAndId(Long schoolId, Long id);
    Optional<LibraryMember> findBySchoolIdAndMemberTypeAndMemberId(Long schoolId, String memberType, Long memberId);
    boolean existsBySchoolIdAndMembershipNumber(Long schoolId, String membershipNumber);
    boolean existsBySchoolIdAndMembershipNumberAndIdNot(Long schoolId, String membershipNumber, Long id);
}
