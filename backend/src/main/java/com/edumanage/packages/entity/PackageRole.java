package com.edumanage.packages.entity;

import com.edumanage.common.entity.BaseEntity;
import com.edumanage.packages.entity.Package;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "package_roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"package_id", "role_name"})
})
@Getter
@Setter
public class PackageRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private Package pkg;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;
}
