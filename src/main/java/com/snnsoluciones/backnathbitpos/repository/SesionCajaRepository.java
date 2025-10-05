package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
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
public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {

  // Buscar sesión abierta por usuario
  @Query("""
      SELECT sc\s
      FROM SesionCaja sc
      JOIN FETCH sc.terminal t
      JOIN FETCH t.sucursal s
      JOIN FETCH sc.usuario u
      WHERE sc.usuario.id = :usuarioId
        AND sc.estado = 'ABIERTA'
     \s""")
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

  /**
   * Busca una sesión de caja activa para un usuario en una sucursal específica
   *
   * @param usuarioId  ID del usuario
   * @param sucursalId ID de la sucursal
   * @return Sesión activa si existe
   */
  @Query("SELECT sc FROM SesionCaja sc " +
      "WHERE sc.usuario.id = :usuarioId " +
      "AND sc.terminal.sucursal.id = :sucursalId " +
      "AND sc.estado = 'ABIERTA' " +
      "AND sc.fechaHoraCierre IS NULL")
  Optional<SesionCaja> findSesionActivaByUsuarioAndSucursal(
      @Param("usuarioId") Long usuarioId,
      @Param("sucursalId") Long sucursalId
  );

  Optional<SesionCaja> findByTerminalIdAndEstado(Long terminalId, EstadoSesion estado);

  List<SesionCaja> findByEstado(EstadoSesion estado);

  Page<SesionCaja> findByTerminalSucursalId(Long sucursalId, Pageable pageable);
}