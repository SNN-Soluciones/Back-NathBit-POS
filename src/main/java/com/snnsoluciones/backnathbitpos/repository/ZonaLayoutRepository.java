// ZonaLayoutRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ZonaLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZonaLayoutRepository extends JpaRepository<ZonaLayout, Long> {
    
    /**
     * Buscar layout por zona
     */
    Optional<ZonaLayout> findByZonaId(Long zonaId);
    
    /**
     * Verificar si existe layout para una zona
     */
    boolean existsByZonaId(Long zonaId);
    
    /**
     * Eliminar layout de una zona
     */
    void deleteByZonaId(Long zonaId);
}
