package com.edumanage.audit.entity;

import com.edumanage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "result", nullable = false, length = 20)
    private String result = "SUCCESS";

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_changes", columnDefinition = "jsonb")
    private String beforeChanges;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_changes", columnDefinition = "jsonb")
    private String afterChanges;
}
