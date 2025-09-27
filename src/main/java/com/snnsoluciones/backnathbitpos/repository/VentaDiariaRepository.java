package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.VentaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VentaDiariaRepository extends JpaRepository<VentaDiaria, Long> {

    // Ventas del día - Empresa (consolidado)
    @Query("SELECT v FROM VentaDiaria v WHERE v.empresa.id = :empresaId " +
           "AND v.sucursal IS NULL AND v.fecha = :fecha")
    Optional<VentaDiaria> findVentasHoyEmpresa(@Param("empresaId") Long empresaId,
                                               @Param("fecha") LocalDate fecha);

    // Ventas del día - Sucursal
    @Query("SELECT v FROM VentaDiaria v WHERE v.sucursal.id = :sucursalId " +
           "AND v.fecha = :fecha")
    Optional<VentaDiaria> findVentasHoySucursal(@Param("sucursalId") Long sucursalId, 
                                                @Param("fecha") LocalDate fecha);
}