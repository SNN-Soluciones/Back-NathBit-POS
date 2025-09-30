package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MetricasVentasDiarias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MetricasVentasDiariasRepository extends JpaRepository<MetricasVentasDiarias, Long> {

    // Ventas del día - Empresa (consolidado)
    @Query("SELECT v FROM MetricasVentasDiarias v WHERE v.empresa.id = :empresaId " +
           "AND v.sucursal IS NULL AND v.fecha = :fecha")
    Optional<MetricasVentasDiarias> findVentasHoyEmpresa(@Param("empresaId") Long empresaId,
                                               @Param("fecha") LocalDate fecha);

    // Ventas del día - Sucursal
    @Query("SELECT v FROM MetricasVentasDiarias v WHERE v.sucursal.id = :sucursalId " +
           "AND v.fecha = :fecha")
    Optional<MetricasVentasDiarias> findVentasHoySucursal(@Param("sucursalId") Long sucursalId,
                                                @Param("fecha") LocalDate fecha);
}