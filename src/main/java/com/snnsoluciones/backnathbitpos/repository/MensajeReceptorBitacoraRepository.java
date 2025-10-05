package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MensajeReceptorBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MensajeReceptorBitacoraRepository extends JpaRepository<MensajeReceptorBitacora, Long> {


    @Query("""
      SELECT m FROM MensajeReceptorBitacora m
      WHERE m.estado IN (:estados)
        AND (m.proximoIntento IS NULL OR m.proximoIntento <= :ahora)
        AND m.intentos < :maxIntentos
      ORDER BY m.updatedAt ASC
      """)
    List<MensajeReceptorBitacora> findPendientesParaProcesar(
        @Param("estados") List<EstadoBitacora> estados,
        @Param("ahora") LocalDateTime ahora,
        @Param("maxIntentos") int maxIntentos,
        Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
      UPDATE MensajeReceptorBitacora m
      SET m.estado = :nuevo, m.updatedAt = :ahora
      WHERE m.id = :id AND m.estado = :esperado
      """)
    int claimParaProcesar(@Param("id") Long id,
        @Param("esperado") EstadoBitacora esperado,
        @Param("nuevo") EstadoBitacora nuevo,
        @Param("ahora") LocalDateTime ahora);

    Optional<MensajeReceptorBitacora> findById(Long id);
    
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
    @Query(value = """
    SELECT m.* FROM mensaje_receptor_bitacora m 
    JOIN compras c ON c.id = m.compra_id 
    WHERE (:empresaId IS NULL OR c.empresa_id = :empresaId) 
      AND (:sucursalId IS NULL OR c.sucursal_id = :sucursalId) 
      AND (:estado IS NULL OR m.estado = CAST(:estado AS VARCHAR)) 
      AND (:fechaInicio IS NULL OR m.created_at >= CAST(:fechaInicio AS timestamp)) 
      AND (:fechaFin IS NULL OR m.created_at <= CAST(:fechaFin AS timestamp))
    ORDER BY m.created_at DESC
    """,
        countQuery = """
    SELECT COUNT(*) FROM mensaje_receptor_bitacora m 
    JOIN compras c ON c.id = m.compra_id 
    WHERE (:empresaId IS NULL OR c.empresa_id = :empresaId) 
      AND (:sucursalId IS NULL OR c.sucursal_id = :sucursalId) 
      AND (:estado IS NULL OR m.estado = CAST(:estado AS VARCHAR)) 
      AND (:fechaInicio IS NULL OR m.created_at >= CAST(:fechaInicio AS timestamp)) 
      AND (:fechaFin IS NULL OR m.created_at <= CAST(:fechaFin AS timestamp))
    """,
        nativeQuery = true)
    Page<MensajeReceptorBitacora> buscarConFiltros(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("estado") String estado, // ⚠️ Cambiar a String
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        Pageable pageable
    );

    /**
     * Buscar último consecutivo por sucursal
     */
    @Query("""
        SELECT m FROM MensajeReceptorBitacora m
        JOIN Compra c ON c.id = m.compraId
        WHERE c.sucursal.id = :sucursalId
          AND m.consecutivo IS NOT NULL
        ORDER BY m.consecutivo DESC
        LIMIT 1
        """)
    Optional<MensajeReceptorBitacora> findUltimoConsecutivoBySucursal(
        @Param("sucursalId") Long sucursalId
    );
}