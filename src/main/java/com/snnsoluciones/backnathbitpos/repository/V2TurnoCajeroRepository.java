// src/main/java/com/snnsoluciones/backnathbitpos/repository/V2TurnoCajeroRepository.java

package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.V2TurnoCajero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface V2TurnoCajeroRepository extends JpaRepository<V2TurnoCajero, Long> {

    // Turno activo de un usuario (en cualquier sesión)
    @Query("""
        SELECT t FROM V2TurnoCajero t
        WHERE t.usuario.id = :usuarioId
          AND t.estado = 'ACTIVO'
        """)
    Optional<V2TurnoCajero> findActivoByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Turno activo de un usuario en una sesión específica
    @Query("""
        SELECT t FROM V2TurnoCajero t
        WHERE t.usuario.id = :usuarioId
          AND t.sesion.id = :sesionId
          AND t.estado = 'ACTIVO'
        """)
    Optional<V2TurnoCajero> findActivoByUsuarioIdAndSesionId(
        @Param("usuarioId") Long usuarioId,
        @Param("sesionId") Long sesionId);

    // Todos los turnos de una sesión
    List<V2TurnoCajero> findBySesionId(Long sesionId);

    // Turnos activos de una sesión
    @Query("""
        SELECT t FROM V2TurnoCajero t
        WHERE t.sesion.id = :sesionId
          AND t.estado = 'ACTIVO'
        """)
    List<V2TurnoCajero> findActivosBySesionId(@Param("sesionId") Long sesionId);

    // ¿Todos los turnos de la sesión están cerrados?
    @Query("""
        SELECT CASE WHEN COUNT(t) = 0 THEN true ELSE false END
        FROM V2TurnoCajero t
        WHERE t.sesion.id = :sesionId
          AND t.estado = 'ACTIVO'
        """)
    boolean todosLosTurnosCerrados(@Param("sesionId") Long sesionId);

    // Efectivo real acumulado en la sesión — desde facturas electrónicas
    @Query("""
        SELECT COALESCE(SUM(mp.monto), 0)
        FROM Factura f
        JOIN f.mediosPago mp
        WHERE f.v2SesionId = :sesionId
          AND f.estado NOT IN ('ANULADA', 'RECHAZADA')
          AND mp.medioPago = 'EFECTIVO'
        """)
    BigDecimal sumEfectivoFacturasBySesionId(@Param("sesionId") Long sesionId);

    // Efectivo real acumulado en la sesión — desde facturas internas
    @Query("""
        SELECT COALESCE(SUM(mp.monto), 0)
        FROM FacturaInterna fi
        JOIN fi.mediosPago mp
        WHERE fi.v2SesionId = :sesionId
          AND fi.estado != 'ANULADA'
          AND mp.tipo = 'EFECTIVO'
        """)
    BigDecimal sumEfectivoInternasBySesionId(@Param("sesionId") Long sesionId);
}