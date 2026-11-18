package com.edumanage.audit.aspect;

import com.edumanage.audit.Auditable;
import com.edumanage.audit.service.ActorResolver;
import com.edumanage.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ActorResolver actorResolver;

    public AuditAspect(AuditLogService auditLogService, ActorResolver actorResolver) {
        this.auditLogService = auditLogService;
        this.actorResolver = actorResolver;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String action = resolveAction(pjp, auditable);
        String entityType = auditable.entityType();
        Long entityId = extractEntityId(pjp);

        ActorResolver.Actor actor = actorResolver.currentActor();
        Long schoolId = actor.schoolId();

        HttpServletRequest request = currentRequest();
        String ipAddress = request != null ? request.getRemoteAddr() : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        String description = auditable.description();
        if (description == null || description.isBlank()) {
            description = "Executed " + pjp.getSignature().toShortString();
        }

        try {
            Object result = pjp.proceed();
            auditLogService.logWithResult(action, entityType, entityId, description,
                    ipAddress, userAgent, null, "SUCCESS", null, null, schoolId);
            return result;
        } catch (Throwable t) {
            auditLogService.logWithResult(action, entityType, entityId,
                    "Failed " + pjp.getSignature().toShortString() + ": " + sanitize(t.getMessage()),
                    ipAddress, userAgent, null, "FAILURE", null, null, schoolId);
            throw t;
        }
    }

    private String resolveAction(ProceedingJoinPoint pjp, Auditable auditable) {
        if (auditable.action() != null && !auditable.action().isBlank()) {
            return auditable.action();
        }
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private Long extractEntityId(ProceedingJoinPoint pjp) {
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof Long id) {
                return id;
            }
            if (arg != null) {
                try {
                    java.lang.reflect.Field field = arg.getClass().getDeclaredField("id");
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

    private String sanitize(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.replaceAll("(?i)(password|secret|token|api.?key|credential)", "[REDACTED]");
    }
}
