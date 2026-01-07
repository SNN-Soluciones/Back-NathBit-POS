package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository para movimientos de inventario (Kardex)
 */
@Repository
public interface ProductoMovimientoRepository extends JpaRepository<ProductoMovimiento, Long> {

    /**
     * Buscar movimientos por producto y sucursal (kardex completo)
     * Ordenados por fecha descendente (más reciente primero)
     */
    Page<ProductoMovimiento> findByProductoIdAndSucursalIdOrderByFechaMovimientoDesc(
        Long productoId, Long sucursalId, Pageable pageable
    );

    /**
     * Buscar todos los movimientos de una sucursal
     * Ordenados por fecha descendente
     */
    Page<ProductoMovimiento> findBySucursalIdOrderByFechaMovimientoDesc(
        Long sucursalId, Pageable pageable
    );

    /**
     * Buscar movimientos de un producto en todas las sucursales
     */
    Page<ProductoMovimiento> findByProductoIdOrderByFechaMovimientoDesc(
        Long productoId, Pageable pageable
    );
}