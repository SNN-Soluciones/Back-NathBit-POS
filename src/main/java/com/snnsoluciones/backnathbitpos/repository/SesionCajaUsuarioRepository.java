package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.SesionCajaUsuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SesionCajaUsuarioRepository
    extends JpaRepository<SesionCajaUsuario, Long> {

  List<SesionCajaUsuario> findBySesionCajaIdAndEstadoOrderByFechaHoraFinDesc(
      Long sesionCajaId, String estado);

  @Query("""
    SELECT CASE WHEN COUNT(s) = 0 THEN true ELSE false END
    FROM SesionCajaUsuario s
    WHERE s.sesionCaja.id = :sesionCajaId
    AND s.estado IN ('ACTIVA', 'CONTEO')
    """)
  boolean todosLosTurnosCerrados(@Param("sesionCajaId") Long sesionCajaId);

  @Query("""
    SELECT s
    FROM SesionCajaUsuario s
    WHERE s.usuario.id = :usuarioId
    AND s.estado = 'ACTIVA'
    ORDER BY s.fechaHoraInicio DESC
    LIMIT 1
    """)
  Optional<SesionCajaUsuario> findTurnoActivoUsuario(
      @Param("usuarioId") Long usuarioId);

  @Query("""
    SELECT COALESCE(SUM(s.ventasEfectivo), 0)
    FROM SesionCajaUsuario s
    WHERE s.sesionCaja.id = :sesionCajaId
    AND s.fechaHoraInicio <= :hasta
    """)
  BigDecimal sumVentasEfectivoSesionHasta(
      @Param("sesionCajaId") Long sesionCajaId,
      @Param("hasta") LocalDateTime hasta);

  @Query("""
      SELECT s
      FROM SesionCajaUsuario s
      WHERE s.usuario.id = :usuarioId
      AND s.sesionCaja.id = :sesionCajaId
      AND s.estado = 'ACTIVA'
      """)
  Optional<SesionCajaUsuario> findTurnoActivoUsuarioEnSesion(
      @Param("usuarioId") Long usuarioId,
      @Param("sesionCajaId") Long sesionCajaId);

  List<SesionCajaUsuario> findBySesionCajaId(Long sesionCajaId);

  @Query("""
      SELECT s
      FROM SesionCajaUsuario s
      WHERE s.sesionCaja.id = :sesionCajaId
      AND s.estado = 'ACTIVA'
      """)
  List<SesionCajaUsuario> findTurnosActivosSesion(Long sesionCajaId);

  @Query("""
      SELECT s
      FROM SesionCajaUsuario s
      WHERE s.sesionCaja.id = :sesionCajaId
      AND s.estado = 'CERRADA'
      """)
  List<SesionCajaUsuario> findTurnosCerradosSesion(Long sesionCajaId);
}