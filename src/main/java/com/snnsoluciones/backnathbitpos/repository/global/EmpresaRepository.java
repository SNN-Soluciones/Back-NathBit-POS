package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.Empresa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para Empresa
 */
@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
    
    Optional<Empresa> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    boolean existsByCedulaJuridica(String cedulaJuridica);
    
    @Query("SELECT e FROM Empresa e LEFT JOIN FETCH e.sucursales WHERE e.id = :id")
    Optional<Empresa> findByIdWithSucursales(@Param("id") UUID id);
    
    List<Empresa> findByActivaTrue();
}
