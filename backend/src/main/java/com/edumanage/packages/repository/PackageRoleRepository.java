package com.edumanage.packages.repository;

import com.edumanage.packages.entity.PackageRole;

import com.edumanage.packages.entity.Package;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageRoleRepository extends JpaRepository<PackageRole, Long> {
    List<PackageRole> findByPkg(Package pkg);
    List<PackageRole> findByPkgId(Long packageId);
}
