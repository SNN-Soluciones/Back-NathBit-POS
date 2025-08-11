package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoImpuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoImpuestoRepository extends JpaRepository<ProductoImpuesto, Long> {
    
    // Buscar por producto y tipo
    Optional<ProductoImpuesto> findByProductoIdAndTipoImpuestoId(Long productoId, Long tipoImpuestoId);
    
    // Verificar si existe
    boolean existsByProductoIdAndTipoImpuestoId(Long productoId, Long tipoImpuestoId);
    
    // Todos los impuestos de un producto
    List<ProductoImpuesto> findByProductoIdAndActivoTrue(Long productoId);
    
    // Impuestos con fetch para evitar lazy loading
    @Query("SELECT pi FROM ProductoImpuesto pi " +
           "JOIN FETCH pi.tipoImpuesto " +
           "LEFT JOIN FETCH pi.tarifaIva " +
           "WHERE pi.producto.id = :productoId AND pi.activo = true")
    List<ProductoImpuesto> findByProductoIdConRelaciones(@Param("productoId") Long productoId);
    
    // Calcular total de impuestos
    @Query("SELECT COALESCE(SUM(pi.porcentaje), 0) FROM ProductoImpuesto pi " +
           "WHERE pi.producto.id = :productoId AND pi.activo = true")
    BigDecimal calcularTotalImpuestos(@Param("productoId") Long productoId);
    
    // Eliminar todos los impuestos de un producto
    @Modifying
    @Query("DELETE FROM ProductoImpuesto pi WHERE pi.producto.id = :productoId")
    void deleteByProductoId(@Param("productoId") Long productoId);
    
    // Productos con un tipo de impuesto específico
    @Query("SELECT pi.producto.id FROM ProductoImpuesto pi " +
           "WHERE pi.tipoImpuesto.id = :tipoImpuestoId AND pi.activo = true")
    List<Long> findProductosIdByTipoImpuesto(@Param("tipoImpuestoId") Long tipoImpuestoId);
}