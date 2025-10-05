package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Orden;
import com.snnsoluciones.backnathbitpos.entity.OrdenItem;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    // Orden activa principal de una mesa (no split)
    @Query("SELECT o FROM Orden o WHERE o.mesa.id = :mesaId AND o.estado NOT IN ('PAGADA', 'ANULADA') AND o.esSplit = false")
    Optional<Orden> findOrdenActivaPrincipalByMesaId(@Param("mesaId") Long mesaId);

    // Órdenes por estado y sucursal
    List<Orden> findBySucursalIdAndEstadoOrderByFechaCreacionDesc(Long sucursalId, EstadoOrden estado);

    // Órdenes abiertas por sucursal
    @Query("SELECT o FROM Orden o WHERE o.sucursal.id = :sucursalId AND o.estado IN ('ABIERTA', 'EN_PREPARACION', 'PREPARADA', 'SERVIDA', 'POR_PAGAR')")
    List<Orden> findOrdenesAbiertasBySucursalId(@Param("sucursalId") Long sucursalId);

    // Siguiente número de orden
    @Query("SELECT MAX(CAST(SUBSTRING(o.numero, 12) AS integer)) FROM Orden o WHERE o.sucursal.id = :sucursalId AND o.numero LIKE :prefijo%")
    Optional<Integer> findMaxNumeroOrden(@Param("sucursalId") Long sucursalId, @Param("prefijo") String prefijo);

    // Órdenes para cocina
    @Query("SELECT DISTINCT o FROM Orden o JOIN o.items i WHERE o.sucursal.id = :sucursalId AND i.enviadoCocina = true AND i.preparado = false")
    List<Orden> findOrdenesEnCocina(@Param("sucursalId") Long sucursalId);
}