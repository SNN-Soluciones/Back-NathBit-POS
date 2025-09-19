package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoInventario;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoInventarioRepository extends JpaRepository<ProductoInventario, Long> {

  Optional<ProductoInventario> findByProductoIdAndSucursalId(Long productoId, Long sucursalId);

  List<ProductoInventario> findBySucursalIdAndEstadoTrue(Long sucursalId);

  List<ProductoInventario> findByProductoIdAndEstadoTrue(Long productoId);

  @Query("SELECT pi FROM ProductoInventario pi WHERE pi.sucursal.id = :sucursalId " +
      "AND pi.cantidadActual < pi.cantidadMinima AND pi.estado = true")
  List<ProductoInventario> findBajoMinimosBySucursal(@Param("sucursalId") Long sucursalId);

  @Query("SELECT pi FROM ProductoInventario pi WHERE pi.sucursal.empresa.id = :empresaId " +
      "AND pi.cantidadActual < pi.cantidadMinima AND pi.estado = true")
  List<ProductoInventario> findBajoMinimosByEmpresa(@Param("empresaId") Long empresaId);

  boolean existsByProductoIdAndCantidadActualGreaterThan(Long id, BigDecimal zero);

}