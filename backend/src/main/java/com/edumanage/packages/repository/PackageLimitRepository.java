package com.edumanage.packages.repository;

import com.edumanage.packages.entity.PackageLimit;

import com.edumanage.packages.entity.Package;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageLimitRepository extends JpaRepository<PackageLimit, Long> {
    List<PackageLimit> findByPkg(Package pkg);
    List<PackageLimit> findByPkgId(Long packageId);
}
