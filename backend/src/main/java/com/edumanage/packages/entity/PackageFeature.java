package com.edumanage.packages.entity;

import com.edumanage.common.entity.BaseEntity;
import com.edumanage.packages.entity.Package;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "package_features", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"package_id", "feature_key"})
})
@Getter
@Setter
public class PackageFeature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private Package pkg;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(name = "feature_name", nullable = false, length = 150)
    private String featureName;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = false;
}
