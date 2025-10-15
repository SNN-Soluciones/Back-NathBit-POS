package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.MetricaProductoVendido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetricaProductoVendidoRepository extends JpaRepository<MetricaProductoVendido, Long> {

    /**
     * Buscar métrica de un producto en una fecha específica
     */
    Optional<MetricaProductoVendido> findByFechaAndSucursalIdAndProductoId(
        LocalDate fecha, Long sucursalId, Long productoId
    );

    /**
     * Top productos más vendidos en un rango de fechas
     */
    @Query("""
        SELECT 
            p.id,
            p.nombre,
            SUM(m.cantidadVendida) as total
        FROM MetricaProductoVendido m
        JOIN m.producto p
        WHERE m.sucursal.id = :sucursalId
          AND m.fecha BETWEEN :fechaDesde AND :fechaHasta
        GROUP BY p.id, p.nombre
        ORDER BY total DESC
        """)
    List<Object[]> findTopProductos(
        @Param("sucursalId") Long sucursalId,
        @Param("fechaDesde") LocalDate fechaDesde,
        @Param("fechaHasta") LocalDate fechaHasta
    );
}