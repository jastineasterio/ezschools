package com.edumanage.packages.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "packages")
@Getter
@Setter
public class Package extends UuidBaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "billing_period", nullable = false, length = 20)
    private String billingPeriod = "MONTHLY";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PackageRole> roles = new HashSet<>();

    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PackageFeature> features = new HashSet<>();

    @OneToMany(mappedBy = "pkg", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PackageLimit> limits = new HashSet<>();
}
