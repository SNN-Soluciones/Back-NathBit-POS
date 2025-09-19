// ProductoQuerySimpleRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoQuerySimpleRepository extends JpaRepository<Producto, Long> {
    
    // Solo las queries REALMENTE necesarias
    @Query("SELECT p FROM Producto p WHERE p.empresa.id = :empresaId " +
           "AND p.activo = true AND (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) " +
           "OR p.codigoInterno = :termino OR p.codigoBarras = :termino)")
    List<Producto> buscarPorTermino(Long empresaId, String termino);
    
    @Query("SELECT COUNT(p) > 0 FROM Producto p WHERE p.empresa.id = :empresaId " +
           "AND p.codigoInterno = :codigo AND (:id IS NULL OR p.id != :id)")
    boolean existeCodigoInterno(Long empresaId, String codigo, Long id);
}