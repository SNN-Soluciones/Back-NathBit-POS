// repository/SillaBarraRepository.java
package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.SillaBarra;
import com.snnsoluciones.backnathbitpos.enums.EstadoSillaBarra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SillaBarraRepository extends JpaRepository<SillaBarra, Long> {
    
    List<SillaBarra> findByBarraIdOrderByNumeroAsc(Long barraId);
    
    List<SillaBarra> findByBarraIdAndEstado(Long barraId, EstadoSillaBarra estado);
    
    Optional<SillaBarra> findByBarraIdAndNumero(Long barraId, Integer numero);
    
    Optional<SillaBarra> findByOrdenPersonaId(Long ordenPersonaId);
    
    List<SillaBarra> findByOrdenId(Long ordenId);
}