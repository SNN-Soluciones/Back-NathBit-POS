package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.OrdenItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenItemRepository extends JpaRepository<OrdenItem, Long> {
    
    // Items pendientes de cocina
    @Query("SELECT i FROM OrdenItem i WHERE i.orden.sucursal.id = :sucursalId AND i.enviadoCocina = false")
    List<OrdenItem> findItemsPendientesCocina(@Param("sucursalId") Long sucursalId);

    // Items en preparación
    @Query("SELECT i FROM OrdenItem i WHERE i.orden.sucursal.id = :sucursalId AND i.enviadoCocina = true AND i.preparado = false")
    List<OrdenItem> findItemsEnPreparacion(@Param("sucursalId") Long sucursalId);

    // Items por orden
    List<OrdenItem> findByOrdenIdOrderById(Long ordenId);
}