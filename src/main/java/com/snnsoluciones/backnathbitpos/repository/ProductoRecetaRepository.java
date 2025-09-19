package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ProductoReceta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRecetaRepository extends JpaRepository<ProductoReceta, Long> {

  List<ProductoReceta> findByEmpresaIdAndEstadoTrue(Long empresaId);

  @Query("SELECT pr FROM ProductoReceta pr JOIN FETCH pr.ingredientes " +
      "WHERE pr.empresa.id = :empresaId AND pr.producto.id = :productoId")
  Optional<ProductoReceta> findByEmpresaIdAndProductoIdWithIngredientes(
      @Param("empresaId") Long empresaId,
      @Param("productoId") Long productoId);

  boolean existsByIngredientesProductoId(Long id);

  boolean existsByProductoIdAndEmpresaId(Long productoId, Long empresaId);

  Optional<ProductoReceta> findByIdAndEmpresaId(Long recetaId, Long empresaId);

  Optional<ProductoReceta> findByProductoIdAndEmpresaId(Long productoId, Long empresaId);
}