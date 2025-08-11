package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.CodigoCAByS;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoCABySRepository extends JpaRepository<CodigoCAByS, Long> {
    
    Optional<CodigoCAByS> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    // Búsqueda por código o descripción
    @Query("SELECT c FROM CodigoCAByS c WHERE c.activo = true AND " +
           "(LOWER(c.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<CodigoCAByS> buscarActivos(@Param("busqueda") String busqueda, Pageable pageable);
    
    // Códigos más usados (para sugerencias)
    @Query("SELECT c FROM CodigoCAByS c WHERE c.id IN " +
           "(SELECT ec.codigoCabys.id FROM EmpresaCAByS ec " +
           "GROUP BY ec.codigoCabys.id ORDER BY COUNT(ec.id) DESC)")
    List<CodigoCAByS> findCodigosMasUsados(Pageable pageable);
    
    // Por tipo (BIEN o SERVICIO)
    List<CodigoCAByS> findByTipoAndActivoTrue(String tipo);
}