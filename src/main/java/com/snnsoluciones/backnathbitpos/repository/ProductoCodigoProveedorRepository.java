package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoCodigoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoCodigoProveedorRepository extends JpaRepository<ProductoCodigoProveedor, Long> {
    
    /**
     * Busca un producto por código y proveedor
     */
    @Query("SELECT pcp FROM ProductoCodigoProveedor pcp " +
           "WHERE pcp.proveedor.id = :proveedorId " +
           "AND pcp.codigo = :codigo " +
           "AND pcp.activo = true")
    Optional<ProductoCodigoProveedor> findByProveedorAndCodigo(
        @Param("proveedorId") Long proveedorId, 
        @Param("codigo") String codigo
    );
    
    /**
     * Busca todos los códigos de un producto
     */
    List<ProductoCodigoProveedor> findByProductoIdAndActivoTrue(Long productoId);
    
    /**
     * Busca todos los productos de un proveedor
     */
    List<ProductoCodigoProveedor> findByProveedorIdAndActivoTrue(Long proveedorId);
    
    /**
     * Verifica si existe un código para un proveedor
     */
    boolean existsByProveedorIdAndCodigoAndActivoTrue(Long proveedorId, String codigo);
    
    /**
     * Busca productos por código (sin importar el proveedor)
     */
    @Query("SELECT pcp FROM ProductoCodigoProveedor pcp " +
           "WHERE pcp.codigo = :codigo " +
           "AND pcp.activo = true")
    List<ProductoCodigoProveedor> findByCodigo(@Param("codigo") String codigo);
}