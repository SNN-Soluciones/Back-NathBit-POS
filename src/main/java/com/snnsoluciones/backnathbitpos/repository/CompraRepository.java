package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {
    

    // Verificar si existe por clave
    boolean existsByClaveHacienda(String claveHacienda);
    
    // Buscar por empresa
    List<Compra> findByEmpresaIdOrderByFechaEmisionDesc(Long empresaId);
    
    // Buscar por sucursal
    List<Compra> findBySucursalIdOrderByFechaEmisionDesc(Long sucursalId);
    
}