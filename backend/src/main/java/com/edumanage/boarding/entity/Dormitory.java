package com.edumanage.boarding.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dormitories")
@Getter
@Setter
public class Dormitory extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 0;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "warden_id")
    private Long wardenId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warden_id", insertable = false, updatable = false)
    private User warden;
}
