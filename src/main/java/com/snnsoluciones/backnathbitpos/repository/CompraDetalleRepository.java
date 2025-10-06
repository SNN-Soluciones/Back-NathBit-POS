package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.CompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {
    
    /**
     * Buscar detalles por compra ordenados por línea
     */
    List<CompraDetalle> findByCompraIdOrderByNumeroLineaAsc(Long compraId);
    
    /**
     * Top productos (CABYS) del mes para métricas
     */
    @Query("SELECT cd.codigoCabys, SUM(cd.montoTotalLinea) as total " +
           "FROM CompraDetalle cd " +
           "JOIN cd.compra c " +
           "WHERE c.empresa.id = :empresaId " +
           "AND c.sucursal.id = :sucursalId " +
           "AND YEAR(c.fechaEmision) = :anio " +
           "AND MONTH(c.fechaEmision) = :mes " +
           "AND cd.codigoCabys IS NOT NULL " +
           "GROUP BY cd.codigoCabys " +
           "ORDER BY total DESC")
    List<Object[]> findTopCabysByMesAnio(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("anio") int anio,
        @Param("mes") int mes
    );
    
    /**
     * Verificar si producto está en alguna compra (para validaciones)
     */
    boolean existsByProductoId(Long productoId);
}