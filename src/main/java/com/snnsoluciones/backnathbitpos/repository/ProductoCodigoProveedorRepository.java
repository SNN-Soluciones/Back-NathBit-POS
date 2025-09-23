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

    // Buscar por producto
    @Query("SELECT pcp FROM ProductoCodigoProveedor pcp " +
        "JOIN FETCH pcp.producto p " +
        "JOIN FETCH pcp.proveedor pr " +
        "WHERE pcp.producto.id = :productoId " +
        "ORDER BY pcp.activo DESC, pr.nombreComercial")
    List<ProductoCodigoProveedor> findByProductoId(@Param("productoId") Long productoId);

    // Buscar por proveedor y activos
    @Query("SELECT pcp FROM ProductoCodigoProveedor pcp " +
        "JOIN FETCH pcp.producto p " +
        "JOIN FETCH pcp.proveedor pr " +
        "WHERE pcp.proveedor.id = :proveedorId AND pcp.activo = :activo " +
        "ORDER BY p.nombre")
    List<ProductoCodigoProveedor> findByProveedorIdAndActivo(
        @Param("proveedorId") Long proveedorId,
        @Param("activo") Boolean activo);

    // Buscar por código y proveedor
    @Query("SELECT pcp FROM ProductoCodigoProveedor pcp " +
        "JOIN FETCH pcp.producto p " +
        "JOIN FETCH pcp.proveedor pr " +
        "WHERE pcp.proveedor.id = :proveedorId AND pcp.codigo = :codigo")
    Optional<ProductoCodigoProveedor> findByProveedorIdAndCodigo(
        @Param("proveedorId") Long proveedorId,
        @Param("codigo") String codigo);

    // Verificar si existe por producto y proveedor activo
    boolean existsByProductoIdAndProveedorIdAndActivo(
        Long productoId, Long proveedorId, Boolean activo);

    // Verificar si existe código para proveedor
    boolean existsByProveedorIdAndCodigo(Long proveedorId, String codigo);

    // Verificar si existe código para proveedor excluyendo un ID
    @Query("SELECT COUNT(pcp) > 0 FROM ProductoCodigoProveedor pcp " +
        "WHERE pcp.proveedor.id = :proveedorId " +
        "AND pcp.codigo = :codigo " +
        "AND pcp.id <> :id")
    boolean existsByProveedorIdAndCodigoAndIdNot(
        @Param("proveedorId") Long proveedorId,
        @Param("codigo") String codigo,
        @Param("id") Long id);

    // Buscar activos por producto
    @Query("SELECT pcp FROM ProductoCodigoProveedor pcp " +
        "JOIN FETCH pcp.proveedor pr " +
        "WHERE pcp.producto.id = :productoId AND pcp.activo = true " +
        "ORDER BY pr.nombreComercial")
    List<ProductoCodigoProveedor> findActivosByProductoId(@Param("productoId") Long productoId);

    // Buscar por producto y empresa del proveedor
    @Query("SELECT pcp FROM ProductoCodigoProveedor pcp " +
        "JOIN FETCH pcp.producto p " +
        "JOIN FETCH pcp.proveedor pr " +
        "WHERE p.empresa.id = :empresaId " +
        "AND pr.empresa.id = :empresaId " +
        "AND pcp.activo = true")
    List<ProductoCodigoProveedor> findByEmpresaId(@Param("empresaId") Long empresaId);
}