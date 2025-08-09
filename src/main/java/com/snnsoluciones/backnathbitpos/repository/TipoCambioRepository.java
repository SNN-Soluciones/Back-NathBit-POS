package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.TipoCambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCambioRepository extends JpaRepository<TipoCambio, Long> {
    
    // Buscar tipo de cambio por moneda y fecha
    @Query("""
    SELECT tc FROM TipoCambio tc
    WHERE tc.moneda.id = :monedaId
    AND tc.fecha = :fecha
    """)
    Optional<TipoCambio> findByMonedaIdAndFecha(
        @Param("monedaId") Long monedaId,
        @Param("fecha") LocalDate fecha
    );
    
    // Buscar tipo de cambio por código de moneda y fecha
    @Query("""
    SELECT tc FROM TipoCambio tc
    JOIN tc.moneda m
    WHERE m.codigo = :codigoMoneda
    AND tc.fecha = :fecha
    """)
    Optional<TipoCambio> findByCodigoMonedaAndFecha(
        @Param("codigoMoneda") String codigoMoneda,
        @Param("fecha") LocalDate fecha
    );
    
    // Buscar últimos tipos de cambio por moneda
    @Query("""
    SELECT tc FROM TipoCambio tc
    WHERE tc.moneda.id = :monedaId
    ORDER BY tc.fecha DESC
    LIMIT :limite
    """)
    List<TipoCambio> findUltimosByMonedaId(
        @Param("monedaId") Long monedaId,
        @Param("limite") int limite
    );
    
    // Buscar tipos de cambio por rango de fechas
    @Query("""
    SELECT tc FROM TipoCambio tc
    WHERE tc.fecha >= :fechaInicio
    AND tc.fecha <= :fechaFin
    ORDER BY tc.fecha DESC, tc.moneda.codigo
    """)
    List<TipoCambio> findByFechaRango(
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
    );
}