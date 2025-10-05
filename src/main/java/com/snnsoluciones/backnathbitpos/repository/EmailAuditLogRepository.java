package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.EmailAuditLog;
import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailAuditLogRepository extends JpaRepository<EmailAuditLog, Long> {

    /**
     * Buscar emails pendientes de reintento
     */
    List<EmailAuditLog> findByEstadoInAndIntentosLessThan(List<EstadoEmail> estados, Integer maxIntentos);

//    Optional<EmailAuditLog> findByFacturaIdAndEstado(Long facturaId, EstadoEmail estado);

    Optional<EmailAuditLog> findFirstByFacturaIdAndEstadoOrderByCreatedAtDesc(Long facturaId, EstadoEmail estado);

}