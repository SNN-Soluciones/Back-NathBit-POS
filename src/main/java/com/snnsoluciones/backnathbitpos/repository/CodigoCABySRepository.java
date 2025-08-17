package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.CodigoCAByS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodigoCABySRepository extends JpaRepository<CodigoCAByS, Long> {
    
    // Buscar por término
    @Query("SELECT c FROM CodigoCAByS c WHERE c.activo = true AND " +
           "(LOWER(c.codigo) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :termino, '%')))")
    List<CodigoCAByS> buscarPorTermino(@Param("termino") String termino);
    
    // Por impuesto
    List<CodigoCAByS> findByImpuestoSugeridoContainingAndActivoTrue(String impuesto);
    
    // Top 100
    List<CodigoCAByS> findTop100ByActivoTrueOrderByCodigoAsc();
}