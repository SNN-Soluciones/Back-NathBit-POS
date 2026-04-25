// src/main/java/com/snnsoluciones/backnathbitpos/repository/V2SesionCajaRepository.java

package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.V2SesionCaja;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface V2SesionCajaRepository
    extends JpaRepository<V2SesionCaja, Long>,
    JpaSpecificationExecutor<V2SesionCaja> {

    // Sesiones abiertas de una sucursal SOLO tipo PDV (para el cajero)
    @Query("SELECT s FROM V2SesionCaja s WHERE s.sucursal.id = :sucursalId AND s.estado = 'ABIERTA' AND s.terminal.tipo = 'PDV'")
    List<V2SesionCaja> findAbiertasPDVBySucursalId(@Param("sucursalId") Long sucursalId);

    // Sesión activa de una terminal
    @Query("SELECT s FROM V2SesionCaja s WHERE s.terminal.id = :terminalId AND s.estado = 'ABIERTA'")
    Optional<V2SesionCaja> findAbiertaByTerminalId(@Param("terminalId") Long terminalId);

    // Todas las sesiones abiertas de una sucursal
    @Query("SELECT s FROM V2SesionCaja s WHERE s.sucursal.id = :sucursalId AND s.estado = 'ABIERTA'")
    List<V2SesionCaja> findAbiertasBySucursalId(@Param("sucursalId") Long sucursalId);

    // Última sesión cerrada de una terminal (para sugerir fondo inicial)
    @Query("""
        SELECT s FROM V2SesionCaja s
        WHERE s.terminal.id = :terminalId
          AND s.estado = 'CERRADA'
        ORDER BY s.fechaCierre DESC
        LIMIT 1
        """)
    Optional<V2SesionCaja> findUltimaCerradaByTerminalId(@Param("terminalId") Long terminalId);

    @Query("""
    SELECT s FROM V2SesionCaja s
    WHERE (:sucursalId IS NULL OR s.sucursal.id = :sucursalId)
      AND (:terminalId IS NULL OR s.terminal.id = :terminalId)
      AND (:estado     IS NULL OR s.estado = :estado)
      AND (:modoGaveta IS NULL OR s.modoGaveta = :modoGaveta)
      AND (:fechaDesde IS NULL OR s.fechaApertura >= :fechaDesde)
      AND (:fechaHasta IS NULL OR s.fechaApertura <= :fechaHasta)
    ORDER BY s.fechaApertura DESC
    """)
    Page<V2SesionCaja> buscarConFiltros(
        @Param("sucursalId") Long sucursalId,
        @Param("terminalId") Long terminalId,
        @Param("estado")     String estado,
        @Param("modoGaveta") String modoGaveta,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta,
        Pageable pageable
    );

    // Para filtrar por cajero que tuvo turno en la sesión
    @Query("""
    SELECT DISTINCT s FROM V2SesionCaja s
    JOIN V2TurnoCajero t ON t.sesion.id = s.id
    WHERE t.usuario.id = :usuarioId
      AND (:sucursalId IS NULL OR s.sucursal.id = :sucursalId)
      AND (:terminalId IS NULL OR s.terminal.id = :terminalId)
      AND (:estado     IS NULL OR s.estado = :estado)
      AND (:fechaDesde IS NULL OR s.fechaApertura >= :fechaDesde)
      AND (:fechaHasta IS NULL OR s.fechaApertura <= :fechaHasta)
    ORDER BY s.fechaApertura DESC
    """)
    Page<V2SesionCaja> buscarConFiltrosPorCajero(
        @Param("usuarioId")  Long usuarioId,
        @Param("sucursalId") Long sucursalId,
        @Param("terminalId") Long terminalId,
        @Param("estado")     String estado,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta,
        Pageable pageable
    );
}