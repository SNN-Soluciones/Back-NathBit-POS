package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.TarifaIVA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarifaIVARepository extends JpaRepository<TarifaIVA, Long> {
    
    Optional<TarifaIVA> findByCodigoHacienda(String codigoHacienda);
    
    boolean existsByCodigoHacienda(String codigoHacienda);
    
    List<TarifaIVA> findByActivoTrueOrderByPorcentajeAsc();
    
    // Tarifa IVA general (13%)
    @Query("SELECT t FROM TarifaIVA t WHERE t.codigoHacienda = '01' AND t.activo = true")
    Optional<TarifaIVA> findTarifaGeneral();
    
    // Tarifa exento
    @Query("SELECT t FROM TarifaIVA t WHERE t.codigoHacienda = '01' AND t.porcentaje = 0 AND t.activo = true")
    Optional<TarifaIVA> findTarifaExento();
}