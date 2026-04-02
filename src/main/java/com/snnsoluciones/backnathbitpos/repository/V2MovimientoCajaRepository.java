// src/main/java/com/snnsoluciones/backnathbitpos/repository/V2MovimientoCajaRepository.java

package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.V2MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface V2MovimientoCajaRepository extends JpaRepository<V2MovimientoCaja, Long> {

    // Todos los movimientos de una sesión
    List<V2MovimientoCaja> findBySesionIdOrderByFechaHoraDesc(Long sesionId);

    // Todos los movimientos de un turno
    List<V2MovimientoCaja> findByTurnoIdOrderByFechaHoraDesc(Long turnoId);

    // Suma de entradas de efectivo de un turno (excluye abonos de crédito)
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0)
        FROM V2MovimientoCaja m
        WHERE m.turno.id = :turnoId
          AND m.tipo = 'ENTRADA_EFECTIVO'
        """)
    BigDecimal sumEntradasEfectivoByTurnoId(@Param("turnoId") Long turnoId);

    // Suma de todas las salidas de efectivo de un turno
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0)
        FROM V2MovimientoCaja m
        WHERE m.turno.id = :turnoId
          AND m.tipo LIKE 'SALIDA_%'
        """)
    BigDecimal sumSalidasByTurnoId(@Param("turnoId") Long turnoId);

    // Suma de entradas de efectivo de toda la sesión
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0)
        FROM V2MovimientoCaja m
        WHERE m.sesion.id = :sesionId
          AND m.tipo = 'ENTRADA_EFECTIVO'
        """)
    BigDecimal sumEntradasEfectivoBySesionId(@Param("sesionId") Long sesionId);

    // Suma de todas las salidas de toda la sesión
    @Query("""
        SELECT COALESCE(SUM(m.monto), 0)
        FROM V2MovimientoCaja m
        WHERE m.sesion.id = :sesionId
          AND m.tipo LIKE 'SALIDA_%'
        """)
    BigDecimal sumSalidasBySesionId(@Param("sesionId") Long sesionId);
}