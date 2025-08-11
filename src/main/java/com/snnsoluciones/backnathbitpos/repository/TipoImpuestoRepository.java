package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.TipoImpuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoImpuestoRepository extends JpaRepository<TipoImpuesto, Long> {
    
    Optional<TipoImpuesto> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    List<TipoImpuesto> findByActivoTrueOrderByDescripcion();
    
    // Buscar IVA
    @Query("SELECT t FROM TipoImpuesto t WHERE t.codigo = '01' AND t.activo = true")
    Optional<TipoImpuesto> findTipoIVA();
    
    // Buscar Servicio
    @Query("SELECT t FROM TipoImpuesto t WHERE t.codigo = '10' AND t.activo = true")
    Optional<TipoImpuesto> findTipoServicio();
}