package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    
    Optional<UnidadMedida> findByCodigo(String codigo);
    
    Optional<UnidadMedida> findBySimbolo(String simbolo);
    
    boolean existsByCodigo(String codigo);
    
    boolean existsBySimbolo(String simbolo);
    
    List<UnidadMedida> findByActivoTrueOrderByDescripcion();
    
    // Búsqueda por código, símbolo o descripción
    @Query("SELECT u FROM UnidadMedida u WHERE u.activo = true AND " +
           "(LOWER(u.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(u.simbolo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(u.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<UnidadMedida> buscar(@Param("busqueda") String busqueda);
    
    // Unidad por defecto (Unidad)
    @Query("SELECT u FROM UnidadMedida u WHERE u.codigo = 'Unid' AND u.activo = true")
    Optional<UnidadMedida> findUnidadPorDefecto();
}