package com.edumanage.studentparents.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student_parents", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "parent_id", "relationship_type"})
})
@Getter
@Setter
public class StudentParent extends SoftDeletableEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(name = "relationship_type", nullable = false, length = 50)
    private String relationshipType;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Column(name = "receives_sms", nullable = false)
    private boolean receivesSms = true;

    @Column(name = "receives_notifications", nullable = false)
    private boolean receivesNotifications = true;
}
