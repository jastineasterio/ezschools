package com.edumanage.packages.repository;

import com.edumanage.packages.entity.PackageFeature;

import com.edumanage.packages.entity.Package;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageFeatureRepository extends JpaRepository<PackageFeature, Long> {
    List<PackageFeature> findByPkg(Package pkg);
    List<PackageFeature> findByPkgId(Long packageId);
}
