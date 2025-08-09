package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
    
    Optional<Sucursal> findByCodigo(String codigo);
    
    List<Sucursal> findByEmpresaId(Long empresaId);
    
    boolean existsByCodigo(String codigo);
}