package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {
    
    // Buscar sesión abierta por usuario
    @Query("""
    SELECT sc FROM SesionCaja sc
    WHERE sc.usuario.id = :usuarioId
    AND sc.estado = 'ABIERTA'
    """)
    Optional<SesionCaja> findSesionAbiertaByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    // Buscar sesión abierta por terminal
    @Query("""
    SELECT sc FROM SesionCaja sc
    WHERE sc.terminal.id = :terminalId
    AND sc.estado = 'ABIERTA'
    """)
    Optional<SesionCaja> findSesionAbiertaByTerminalId(@Param("terminalId") Long terminalId);
    
    // Buscar sesiones por fecha
    @Query("""
    SELECT sc FROM SesionCaja sc
    WHERE sc.fechaHoraApertura >= :fechaInicio
    AND sc.fechaHoraApertura <= :fechaFin
    ORDER BY sc.fechaHoraApertura DESC
    """)
    List<SesionCaja> findByFechaRango(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    // Buscar sesiones por terminal y fecha
    @Query("""
    SELECT sc FROM SesionCaja sc
    WHERE sc.terminal.id = :terminalId
    AND sc.fechaHoraApertura >= :fechaInicio
    AND sc.fechaHoraApertura <= :fechaFin
    ORDER BY sc.fechaHoraApertura DESC
    """)
    List<SesionCaja> findByTerminalIdAndFechaRango(
        @Param("terminalId") Long terminalId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    // Verificar si usuario tiene sesión abierta
    @Query("""
    SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END
    FROM SesionCaja sc
    WHERE sc.usuario.id = :usuarioId
    AND sc.estado = 'ABIERTA'
    """)
    boolean existsSesionAbiertaByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    // Buscar última sesión cerrada por terminal
    @Query("""
    SELECT sc FROM SesionCaja sc
    WHERE sc.terminal.id = :terminalId
    AND sc.estado = 'CERRADA'
    ORDER BY sc.fechaHoraCierre DESC
    LIMIT 1
    """)
    Optional<SesionCaja> findUltimaSesionCerradaByTerminalId(@Param("terminalId") Long terminalId);

    @Query("SELECT s FROM SesionCaja s WHERE s.usuario.id = :usuarioId AND s.estado = 'ABIERTA' AND s.fechaHoraCierre IS NULL")
    Optional<SesionCaja> findActivaByUsuarioId(@Param("usuarioId") Long usuarioId);
}