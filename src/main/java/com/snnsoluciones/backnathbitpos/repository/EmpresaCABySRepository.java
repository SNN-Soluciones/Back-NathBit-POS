package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.EmpresaCAByS;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaCABySRepository extends JpaRepository<EmpresaCAByS, Long> {
    
    // Buscar por empresa y código
    Optional<EmpresaCAByS> findByEmpresaIdAndCodigoCabysId(Long empresaId, Long codigoCabysId);
    
    // Verificar si existe
    boolean existsByEmpresaIdAndCodigoCabysId(Long empresaId, Long codigoCabysId);
    
    // Todos los códigos de una empresa
    List<EmpresaCAByS> findByEmpresaIdAndActivoTrue(Long empresaId);
    
    // Búsqueda paginada por empresa
    @Query("SELECT ec FROM EmpresaCAByS ec " +
           "JOIN FETCH ec.codigoCabys c " +
           "WHERE ec.empresa.id = :empresaId AND ec.activo = true AND " +
           "(LOWER(c.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(ec.descripcionPersonalizada) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<EmpresaCAByS> buscarPorEmpresa(@Param("empresaId") Long empresaId, 
                                        @Param("busqueda") String busqueda, 
                                        Pageable pageable);
    
    // Contar códigos activos por empresa
    long countByEmpresaIdAndActivoTrue(Long empresaId);
}