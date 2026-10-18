package com.edumanage.resultapprovals.entity;

import com.edumanage.common.entity.SoftDeletableEntity;
import com.edumanage.results.entity.Result;
import com.edumanage.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "result_approvals")
@Getter
@Setter
public class ResultApproval extends SoftDeletableEntity {

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", insertable = false, updatable = false)
    private Result result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", insertable = false, updatable = false)
    private User actorUser;
}
