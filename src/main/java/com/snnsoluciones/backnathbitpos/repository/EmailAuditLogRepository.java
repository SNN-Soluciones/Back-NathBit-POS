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
     * Buscar por clave de factura
     */
    Optional<EmailAuditLog> findByClave(String clave);

    /**
     * Buscar todos los logs de una factura
     */
    List<EmailAuditLog> findByFacturaIdOrderByCreatedAtDesc(Long facturaId);

    /**
     * Buscar por estado
     */
    List<EmailAuditLog> findByEstado(EstadoEmail estado);

    /**
     * Contar emails enviados por factura
     */
    long countByFacturaIdAndEstado(Long facturaId, EstadoEmail estado);

    /**
     * Buscar emails pendientes de reintento
     */
    List<EmailAuditLog> findByEstadoInAndIntentosLessThan(List<EstadoEmail> estados, Integer maxIntentos);

//    Optional<EmailAuditLog> findByFacturaIdAndEstado(Long facturaId, EstadoEmail estado);

    Optional<EmailAuditLog> findFirstByFacturaIdAndEstadoOrderByCreatedAtDesc(Long facturaId, EstadoEmail estado);

    // O si necesitas todos los registros:
    List<EmailAuditLog> findAllByFacturaIdAndEstadoOrderByCreatedAtDesc(Long facturaId, EstadoEmail estado);

}