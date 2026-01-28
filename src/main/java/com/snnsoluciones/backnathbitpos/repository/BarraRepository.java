// repository/BarraRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Barra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BarraRepository extends JpaRepository<Barra, Long> {
    
    List<Barra> findByZonaIdOrderByOrdenAsc(Long zonaId);
    
    List<Barra> findBySucursalIdAndActivoTrue(Long sucursalId);
    
    Optional<Barra> findByZonaIdAndCodigo(Long zonaId, String codigo);
    
    boolean existsByZonaIdAndCodigo(Long zonaId, String codigo);
}