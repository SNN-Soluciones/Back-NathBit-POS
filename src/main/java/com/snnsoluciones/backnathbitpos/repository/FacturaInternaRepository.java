package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInterna;
import java.math.BigDecimal;
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
public interface FacturaInternaRepository extends JpaRepository<FacturaInterna, Long> {

    // Buscar por empresa
    Page<FacturaInterna> findByEmpresaId(Long empresaId, Pageable pageable);
    /**
     * Buscar factura interna por número
     */
    Optional<FacturaInterna> findByNumero(String numero);

    // Buscar por sucursal
    Page<FacturaInterna> findBySucursalId(Long sucursalId, Pageable pageable);

    // Buscar por estado
    Page<FacturaInterna> findByEmpresaIdAndEstado(Long empresaId, String estado, Pageable pageable);

    // Obtener el último número de factura para generar el siguiente
    @Query("SELECT f.numero FROM FacturaInterna f WHERE f.empresa.id = :empresaId " +
        "AND f.numero LIKE :prefix% " +
        "ORDER BY f.numero DESC")
    List<String> findUltimoNumeroByEmpresaAndPrefix(
        @Param("empresaId") Long empresaId,
        @Param("prefix") String prefix,
        Pageable pageable
    );

    @Query("SELECT fi FROM FacturaInterna fi WHERE fi.sesionCaja.id = :sesionId ORDER BY fi.fecha DESC")
    List<FacturaInterna> findBySesionCajaId(@Param("sesionId") Long sesionId);

    // src/main/java/com/snnsoluciones/backnathbitpos/repository/FacturaInternaRepository.java

    /**
     * Buscar facturas por empresa, sucursal y término de búsqueda (número o cliente)
     */
    @Query("SELECT f FROM FacturaInterna f " +
        "WHERE f.empresa.id = :empresaId " +
        "AND f.sucursal.id = :sucursalId " +
        "AND f.fecha BETWEEN :fechaDesde AND :fechaHasta " +
        "AND (LOWER(f.numero) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
        "     OR LOWER(f.nombreCliente) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
        "ORDER BY f.fecha DESC")
    Page<FacturaInterna> buscarConFiltros(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

    /**
     * Buscar facturas por empresa, sucursal y rango de fechas (sin búsqueda)
     */
    @Query("SELECT f FROM FacturaInterna f " +
        "WHERE f.empresa.id = :empresaId " +
        "AND f.sucursal.id = :sucursalId " +
        "AND f.fecha BETWEEN :fechaDesde AND :fechaHasta " +
        "ORDER BY f.fecha DESC")
    Page<FacturaInterna> buscarPorFechas(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta,
        Pageable pageable
    );

}