package com.edumanage.parents.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "parents")
@Getter
@Setter
public class Parent extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
