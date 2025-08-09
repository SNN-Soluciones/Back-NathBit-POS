package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.EmpresaActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaActividadRepository extends JpaRepository<EmpresaActividad, Long> {
    
    // Buscar actividades por empresa
    List<EmpresaActividad> findByEmpresaIdOrderByOrden(Long empresaId);
    
    // Buscar actividad principal de una empresa
    @Query("""
    SELECT ea FROM EmpresaActividad ea
    WHERE ea.empresa.id = :empresaId
    AND ea.esPrincipal = true
    """)
    Optional<EmpresaActividad> findActividadPrincipalByEmpresaId(@Param("empresaId") Long empresaId);
    
    // Verificar si existe la combinación empresa-actividad
    boolean existsByEmpresaIdAndActividadCodigo(Long empresaId, String actividadCodigo);
    
    // Contar actividades por empresa
    long countByEmpresaId(Long empresaId);
}