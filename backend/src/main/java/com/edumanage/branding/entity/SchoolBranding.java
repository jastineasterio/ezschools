package com.edumanage.branding.entity;

import com.edumanage.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "school_branding")
@Getter
@Setter
public class SchoolBranding extends BaseEntity {

    @Column(name = "school_id", nullable = false, unique = true)
    private Long schoolId;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor = "#3b82f6";

    @Column(name = "secondary_color", nullable = false, length = 7)
    private String secondaryColor = "#64748b";

    @Column(name = "accent_color", nullable = false, length = 7)
    private String accentColor = "#f59e0b";

    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor = "#ffffff";

    @Column(name = "text_color", nullable = false, length = 7)
    private String textColor = "#0f172a";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;
}
