package com.edumanage.tenant;

public interface TenantContextHolder {
    Long getTenantId();
    void setTenantId(Long tenantId);
    void clear();
}
