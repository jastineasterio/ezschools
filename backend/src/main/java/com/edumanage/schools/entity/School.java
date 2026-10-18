package com.edumanage.schools.entity;

import com.edumanage.common.entity.UuidBaseEntity;
import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "schools")
@Getter
@Setter
public class School extends UuidBaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "registration_number", unique = true, length = 100)
    private String registrationNumber;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_suspended", nullable = false)
    private boolean isSuspended = false;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "suspended_reason")
    private String suspendedReason;

    @ManyToMany
    @JoinTable(name = "school_users",
            joinColumns = @JoinColumn(name = "school_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> users = new HashSet<>();
}
