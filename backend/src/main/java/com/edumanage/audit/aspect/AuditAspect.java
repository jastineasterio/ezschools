package com.edumanage.audit.aspect;

import com.edumanage.audit.Auditable;
import com.edumanage.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String action = auditable.action();
        String entityType = auditable.entityType();
        Long entityId = extractEntityId(pjp);

        HttpServletRequest request = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            request = attrs.getRequest();
        }

        String ipAddress = request != null ? request.getRemoteAddr() : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        Object result;
        try {
            result = pjp.proceed();
            auditLogService.log(action, entityType, entityId,
                    "Executed " + pjp.getSignature().toShortString(), ipAddress, userAgent, null);
            return result;
        } catch (Throwable t) {
            auditLogService.log(action, entityType, entityId,
                    "Failed " + pjp.getSignature().toShortString() + ": " + t.getMessage(), ipAddress, userAgent, null);
            throw t;
        }
    }

    private Long extractEntityId(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof Long id) {
                return id;
            }
            if (arg != null) {
                try {
                    Field field = arg.getClass().getDeclaredField("id");
                    field.setAccessible(true);
                    Object value = field.get(arg);
                    if (value instanceof Long id) {
                        return id;
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // ignore
                }
            }
        }
        return null;
    }
}
