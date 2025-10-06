package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Verificar si existe por clave
    boolean existsByClaveHacienda(String claveHacienda);

    // Buscar por empresa
    List<Compra> findByEmpresaIdOrderByFechaEmisionDesc(Long empresaId);

    // Buscar por sucursal
    List<Compra> findBySucursalIdOrderByFechaEmisionDesc(Long sucursalId);

    /**
     * Contar proveedores únicos en un mes
     * Solo cuenta compras en estados válidos (aceptadas o completadas)
     */
    @Query("SELECT COUNT(DISTINCT c.proveedor.id) FROM Compra c " +
        "WHERE c.empresa.id = :empresaId " +
        "AND c.sucursal.id = :sucursalId " +
        "AND YEAR(c.fechaEmision) = :anio " +
        "AND MONTH(c.fechaEmision) = :mes " +
        "AND c.estado IN ('ACEPTADA', 'ACEPTADA_PARCIAL', 'COMPLETADA')")
    long countDistinctProveedoresByMesAnio(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("anio") int anio,
        @Param("mes") int mes
    );

    /**
     * Top proveedor del mes por monto total
     * Retorna [proveedorId, totalMonto] ordenado DESC
     * Solo considera compras en estados válidos
     */
    @Query("SELECT c.proveedor.id, SUM(c.totalComprobante) as total " +
        "FROM Compra c " +
        "WHERE c.empresa.id = :empresaId " +
        "AND c.sucursal.id = :sucursalId " +
        "AND YEAR(c.fechaEmision) = :anio " +
        "AND MONTH(c.fechaEmision) = :mes " +
        "AND c.estado IN ('ACEPTADA', 'ACEPTADA_PARCIAL', 'COMPLETADA') " +
        "GROUP BY c.proveedor.id " +
        "ORDER BY total DESC")
    List<Object[]> findTopProveedorByMesAnio(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("anio") int anio,
        @Param("mes") int mes
    );
}