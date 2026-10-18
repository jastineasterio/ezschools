package com.edumanage.permissions.entity;

import com.edumanage.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "category", length = 50)
    private String category;
}
