package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ActividadEconomica;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActividadEconomicaRepository extends JpaRepository<ActividadEconomica, String> {
    
    // Buscar por descripción (like)
    @Query("SELECT a FROM ActividadEconomica a WHERE LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :descripcion, '%'))")
    List<ActividadEconomica> findByDescripcionContaining(@Param("descripcion") String descripcion);
    
    // Buscar activas
    List<ActividadEconomica> findByActivaTrue();
    Optional<ActividadEconomica> findByCodigo(String codigo);

}