package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MensajeReceptorBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MensajeReceptorBitacoraRepository extends JpaRepository<MensajeReceptorBitacora, Long> {
    
    // Buscar por compra
    Optional<MensajeReceptorBitacora> findByCompraId(Long compraId);
    
    // Buscar por clave
    Optional<MensajeReceptorBitacora> findByClave(String clave);
    
    // Buscar pendientes para procesar
    @Query("SELECT m FROM MensajeReceptorBitacora m " +
           "WHERE m.estado = 'PENDIENTE' " +
           "AND (m.proximoIntento IS NULL OR m.proximoIntento <= :ahora) " +
           "ORDER BY m.createdAt ASC")
    List<MensajeReceptorBitacora> findMensajesPendientesProcesar(
        @Param("ahora") LocalDateTime ahora, 
        Pageable pageable
    );
    
    // Contar por estado
    long countByEstado(EstadoBitacora estado);
    
    // Buscar con filtros para el frontend
    @Query("SELECT m FROM MensajeReceptorBitacora m " +
           "JOIN Compra c ON c.id = m.compraId " +
           "WHERE (:empresaId IS NULL OR c.empresa.id = :empresaId) " +
           "AND (:sucursalId IS NULL OR c.sucursal.id = :sucursalId) " +
           "AND (:estado IS NULL OR m.estado = :estado) " +
           "AND (:fechaInicio IS NULL OR m.createdAt >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR m.createdAt <= :fechaFin)")
    Page<MensajeReceptorBitacora> buscarConFiltros(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("estado") EstadoBitacora estado,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        Pageable pageable
    );
}