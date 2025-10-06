package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.EmailAuditLog;
import com.snnsoluciones.backnathbitpos.enums.EstadoEmail;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    boolean existsByFacturaIdAndEstado(Long facturaId, EstadoEmail estado);

    @Query("""
      select e.facturaId
      from EmailAuditLog e
      where e.estado = com.snnsoluciones.backnathbitpos.enums.EstadoEmail.ENVIADO
        and e.facturaId in :facturaIds
    """)
    Set<Long> findFacturaIdsEnviados(Collection<Long> facturaIds);

}