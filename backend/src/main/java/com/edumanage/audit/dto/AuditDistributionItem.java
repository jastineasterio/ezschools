package com.edumanage.audit.dto;

/**
 * Single bucket of a distribution aggregation (action or result).
 */
public record AuditDistributionItem(String name, long count) {
}
