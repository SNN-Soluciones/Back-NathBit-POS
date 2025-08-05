package com.snnsoluciones.backnathbitpos.entity.security;

import com.snnsoluciones.backnathbitpos.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad para registrar eventos de auditoría del sistema.
 * Almacena todos los eventos importantes como login, logout, cambios en datos críticos, etc.
 */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_url", columnDefinition = "TEXT")
    private String requestUrl;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;
}