package com.edumanage.streams.entity;

import com.edumanage.classes.entity.Class;
import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "streams", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"class_id", "name"}),
        @UniqueConstraint(columnNames = {"school_id", "class_id", "code"})
})
@Getter
@Setter
public class Stream extends SoftDeletableEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private Class clazz;
}
