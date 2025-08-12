package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.TipoCambio;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TipoCambioRepository extends JpaRepository<TipoCambio, Long> {

    // Buscar por moneda (enum) y fecha
    @Query("""
           SELECT tc
           FROM TipoCambio tc
           WHERE tc.moneda = :moneda
             AND tc.fecha  = :fecha
           """)
    Optional<TipoCambio> findByMonedaAndFecha(@Param("moneda") Moneda moneda,
        @Param("fecha")  LocalDate fecha);

    // Último registro (más reciente) por moneda
    Optional<TipoCambio> findTopByMonedaOrderByFechaDesc(Moneda moneda);

    // Últimos N por moneda (usar Pageable para el límite)
    List<TipoCambio> findByMonedaOrderByFechaDesc(Moneda moneda, Pageable pageable);

    // Por rango de fechas (ordena por fecha desc y moneda asc)
    @Query("""
           SELECT tc
           FROM TipoCambio tc
           WHERE tc.fecha BETWEEN :fechaInicio AND :fechaFin
           ORDER BY tc.fecha DESC, tc.moneda ASC
           """)
    List<TipoCambio> findByFechaRango(@Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin")    LocalDate fechaFin);

    // --- Helper opcional para compatibilidad con "codigoMoneda" (CRC/USD/EUR) ---
    // Permite seguir llamando con String si lo necesitas
    default Optional<TipoCambio> findByCodigoMonedaAndFecha(String codigoMoneda, LocalDate fecha) {
        try {
            Moneda m = Moneda.valueOf(codigoMoneda); // Debe venir "CRC", "USD", "EUR", etc.
            return findByMonedaAndFecha(m, fecha);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}