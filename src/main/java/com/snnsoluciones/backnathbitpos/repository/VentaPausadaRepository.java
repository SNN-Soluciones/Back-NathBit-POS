package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.VentaPausada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaPausadaRepository extends JpaRepository<VentaPausada, Long> {
    
    // Buscar ventas pausadas activas por usuario y sucursal
    List<VentaPausada> findByUsuarioIdAndSucursalIdAndFechaExpiracionAfterOrderByFechaCreacionDesc(
            Long usuarioId, Long sucursalId, LocalDateTime fechaActual
    );
    
    // Buscar por ID validando que pertenezca al usuario
    Optional<VentaPausada> findByIdAndUsuarioIdAndSucursalId(Long id, Long usuarioId, Long sucursalId);
    
    // Contar ventas pausadas activas para el badge
    long countByUsuarioIdAndSucursalIdAndFechaExpiracionAfter(
            Long usuarioId, Long sucursalId, LocalDateTime fechaActual
    );
    
    // Eliminar ventas expiradas (para job programado)
    @Modifying
    @Query("DELETE FROM VentaPausada v WHERE v.fechaExpiracion < :fechaActual")
    int deleteExpiredVentas(@Param("fechaActual") LocalDateTime fechaActual);
    
    // Buscar todas las ventas de una sucursal (para supervisores)
    @Query("SELECT v FROM VentaPausada v WHERE v.sucursalId = :sucursalId " +
           "AND v.fechaExpiracion > :fechaActual ORDER BY v.fechaCreacion DESC")
    List<VentaPausada> findAllBySucursalActivas(
            @Param("sucursalId") Long sucursalId,
            @Param("fechaActual") LocalDateTime fechaActual
    );
}