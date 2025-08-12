package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoImpuesto;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoImpuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoImpuestoRepository extends JpaRepository<ProductoImpuesto, Long> {
    
    // Buscar por producto
    List<ProductoImpuesto> findByProductoIdAndActivoTrue(Long productoId);
    
    // Buscar por producto y tipo
    Optional<ProductoImpuesto> findByProductoIdAndTipoImpuesto(Long productoId, TipoImpuesto tipoImpuesto);
    
    // Verificar si existe
    boolean existsByProductoIdAndTipoImpuesto(Long productoId, TipoImpuesto tipoImpuesto);
    
    // Contar impuestos activos de un producto
    long countByProductoIdAndActivoTrue(Long productoId);
    
    // Eliminar todos los impuestos de un producto
    void deleteByProductoId(Long productoId);
    
    // Buscar productos con un tipo de impuesto específico
    @Query("SELECT DISTINCT pi.producto FROM ProductoImpuesto pi " +
           "WHERE pi.tipoImpuesto = :tipoImpuesto " +
           "AND pi.activo = true " +
           "AND pi.producto.empresa.id = :empresaId")
    List<Long> findProductoIdsConTipoImpuesto(@Param("empresaId") Long empresaId, 
                                               @Param("tipoImpuesto") TipoImpuesto tipoImpuesto);
}