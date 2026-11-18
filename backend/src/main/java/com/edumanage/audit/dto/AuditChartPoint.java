package com.edumanage.audit.dto;

/**
 * Single day of the activity trend aggregation.
 */
public record AuditChartPoint(String period, long count) {
}
