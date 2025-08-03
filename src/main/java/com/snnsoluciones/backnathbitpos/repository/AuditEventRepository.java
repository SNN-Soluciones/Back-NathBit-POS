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

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByUsername(String username, Pageable pageable);

    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);

    Page<AuditEvent> findByEventDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE a.username = :username AND a.eventType = :eventType ORDER BY a.eventDate DESC")
    List<AuditEvent> findRecentByUsernameAndType(@Param("username") String username, 
                                                  @Param("eventType") String eventType, 
                                                  Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username = :username AND a.eventType = 'LOGIN_FAILED' AND a.eventDate > :since")
    long countFailedLoginAttempts(@Param("username") String username, @Param("since") LocalDateTime since);
}