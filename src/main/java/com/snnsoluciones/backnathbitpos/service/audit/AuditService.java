package com.snnsoluciones.backnathbitpos.service.audit;

import com.snnsoluciones.backnathbitpos.entity.security.AuditEvent;
import com.snnsoluciones.backnathbitpos.repository.AuditEventRepository;
import com.snnsoluciones.backnathbitpos.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Servicio para el registro de eventos de auditoría.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    /**
     * Registra un evento de auditoría exitoso.
     */
    @Transactional
    public void logEvent(String eventType, String details) {
        logEvent(eventType, details, true, null);
    }

    /**
     * Registra un evento de auditoría con estado específico.
     */
    @Transactional
    public void logEvent(String eventType, String details, boolean success, String errorMessage) {
        try {
            AuditEvent event = new AuditEvent();
            event.setUsername(getCurrentUsername());
            event.setEventType(eventType);
            event.setEventDate(LocalDateTime.now());
            event.setIpAddress(getClientIpAddress());
            event.setUserAgent(getUserAgent());
            event.setDetails(details);
            event.setSuccess(success);
            event.setErrorMessage(errorMessage);
            event.setRequestMethod(getRequestMethod());
            event.setRequestUrl(getRequestUrl());

            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Error al registrar evento de auditoría: {}", e.getMessage());
        }
    }

    /**
     * Registra un evento de auditoría con información de recurso.
     */
    @Transactional
    public void logResourceEvent(String eventType, String resourceType, String resourceId,
        String details, boolean success) {
        try {
            AuditEvent event = new AuditEvent();
            event.setUsername(getCurrentUsername());
            event.setEventType(eventType);
            event.setEventDate(LocalDateTime.now());
            event.setIpAddress(getClientIpAddress());
            event.setUserAgent(getUserAgent());
            event.setDetails(details);
            event.setSuccess(success);
            event.setResourceType(resourceType);
            event.setResourceId(resourceId);
            event.setRequestMethod(getRequestMethod());
            event.setRequestUrl(getRequestUrl());

            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Error al registrar evento de auditoría de recurso: {}", e.getMessage());
        }
    }

    /**
     * Registra un evento de auditoría con cambios de valores.
     */
    @Transactional
    public void logUpdateEvent(String resourceType, String resourceId,
        String oldValue, String newValue, String details) {
        try {
            AuditEvent event = new AuditEvent();
            event.setUsername(getCurrentUsername());
            event.setEventType("UPDATE");
            event.setEventDate(LocalDateTime.now());
            event.setIpAddress(getClientIpAddress());
            event.setUserAgent(getUserAgent());
            event.setDetails(details);
            event.setSuccess(true);
            event.setResourceType(resourceType);
            event.setResourceId(resourceId);
            event.setOldValue(oldValue);
            event.setNewValue(newValue);
            event.setRequestMethod(getRequestMethod());
            event.setRequestUrl(getRequestUrl());

            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Error al registrar evento de actualización: {}", e.getMessage());
        }
    }

    /**
     * Registra un intento de acceso no autorizado.
     */
    @Transactional
    public void logUnauthorizedAccess(String resource, String details) {
        logEvent("UNAUTHORIZED_ACCESS",
            String.format("Intento de acceso no autorizado a: %s. %s", resource, details),
            false,
            "Acceso denegado");
    }

    /**
     * Registra una operación con su tiempo de ejecución.
     */
    @Transactional
    public void logTimedEvent(String eventType, String details, long executionTimeMs,
        boolean success, Integer responseStatus) {
        try {
            AuditEvent event = new AuditEvent();
            event.setUsername(getCurrentUsername());
            event.setEventType(eventType);
            event.setEventDate(LocalDateTime.now());
            event.setIpAddress(getClientIpAddress());
            event.setUserAgent(getUserAgent());
            event.setDetails(details);
            event.setSuccess(success);
            event.setExecutionTimeMs(executionTimeMs);
            event.setResponseStatus(responseStatus);
            event.setRequestMethod(getRequestMethod());
            event.setRequestUrl(getRequestUrl());

            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Error al registrar evento con tiempo: {}", e.getMessage());
        }
    }

    // Métodos auxiliares privados

    private String getCurrentUsername() {
        return SecurityUtils.getCurrentUsername().orElse("anonymous");
    }

    private String getClientIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }
        return "unknown";
    }

    private String getUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getHeader("User-Agent") : "unknown";
    }

    private String getRequestMethod() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getMethod() : null;
    }

    private String getRequestUrl() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            StringBuffer url = request.getRequestURL();
            String queryString = request.getQueryString();
            if (queryString != null) {
                url.append("?").append(queryString);
            }
            return url.toString();
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }
}