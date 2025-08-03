package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.security.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la gestión de eventos de auditoría.
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    /**
     * Busca eventos de auditoría por nombre de usuario.
     */
    Page<AuditEvent> findByUsername(String username, Pageable pageable);

    /**
     * Busca eventos de auditoría por tipo de evento.
     */
    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);

    /**
     * Busca eventos de auditoría por nombre de usuario y tipo de evento.
     */
    Page<AuditEvent> findByUsernameAndEventType(String username, String eventType, Pageable pageable);


    Page<AuditEvent> findByUsernameAndEventTypeIn(String username, List<String> eventTypes, Pageable pageable);

    /**
     * Busca eventos de auditoría en un rango de fechas.
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.eventDate BETWEEN :startDate AND :endDate ORDER BY a.eventDate DESC")
    Page<AuditEvent> findByDateRange(@Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    /**
     * Busca eventos de auditoría por usuario en un rango de fechas.
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.username = :username AND a.eventDate BETWEEN :startDate AND :endDate ORDER BY a.eventDate DESC")
    Page<AuditEvent> findByUsernameAndDateRange(@Param("username") String username,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    /**
     * Busca eventos de auditoría fallidos.
     */
    Page<AuditEvent> findBySuccessFalse(Pageable pageable);

    /**
     * Busca eventos de auditoría exitosos.
     */
    Page<AuditEvent> findBySuccessTrue(Pageable pageable);

    /**
     * Busca eventos de auditoría por tipo de recurso.
     */
    Page<AuditEvent> findByResourceType(String resourceType, Pageable pageable);

    /**
     * Busca eventos de auditoría por tipo de recurso e ID.
     */
    Page<AuditEvent> findByResourceTypeAndResourceId(String resourceType, String resourceId, Pageable pageable);

    /**
     * Cuenta eventos de auditoría por tipo en un rango de fechas.
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditEvent a WHERE a.eventDate BETWEEN :startDate AND :endDate GROUP BY a.eventType")
    List<Object[]> countByEventTypeAndDateRange(@Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Obtiene los últimos eventos de auditoría.
     */
    List<AuditEvent> findTop10ByOrderByEventDateDesc();

    /**
     * Obtiene los últimos eventos de auditoría de un usuario.
     */
    List<AuditEvent> findTop10ByUsernameOrderByEventDateDesc(String username);

    /**
     * Elimina eventos de auditoría antiguos.
     */
    @Query("DELETE FROM AuditEvent a WHERE a.eventDate < :cutoffDate")
    void deleteOldEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca intentos de login fallidos recientes de un usuario.
     */
    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username = :username AND a.eventType = 'LOGIN_FAILED' AND a.eventDate > :since")
    Long countRecentFailedLogins(@Param("username") String username, @Param("since") LocalDateTime since);
}