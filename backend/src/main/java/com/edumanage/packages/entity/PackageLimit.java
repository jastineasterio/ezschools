package com.edumanage.packages.entity;

import com.edumanage.common.entity.BaseEntity;
import com.edumanage.packages.entity.Package;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "package_limits", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"package_id", "limit_key"})
})
@Getter
@Setter
public class PackageLimit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private Package pkg;

    @Column(name = "limit_key", nullable = false, length = 100)
    private String limitKey;

    @Column(name = "limit_name", nullable = false, length = 150)
    private String limitName;

    @Column(name = "max_value", nullable = false)
    private long maxValue = 0;

    @Column(name = "current_value", nullable = false)
    private long currentValue = 0;

    @Column(name = "unit", length = 50)
    private String unit;
}
