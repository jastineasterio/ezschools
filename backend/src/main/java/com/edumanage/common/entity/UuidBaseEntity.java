package com.edumanage.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class UuidBaseEntity extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @PrePersist
    protected void generateUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }
}
