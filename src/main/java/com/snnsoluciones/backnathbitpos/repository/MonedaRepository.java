package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Moneda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonedaRepository extends JpaRepository<Moneda, Long> {
    
    Optional<Moneda> findByCodigo(String codigo);
    
    List<Moneda> findByActivaTrueOrderByOrden();
    
    Optional<Moneda> findByEsLocalTrue();
    
    boolean existsByCodigo(String codigo);
}