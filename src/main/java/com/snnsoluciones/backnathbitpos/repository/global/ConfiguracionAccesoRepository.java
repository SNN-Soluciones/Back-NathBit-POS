package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.ConfiguracionAcceso;
import com.snnsoluciones.backnathbitpos.enums.TipoDeteccion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para ConfiguracionAcceso
 */
@Repository
public interface ConfiguracionAccesoRepository extends JpaRepository<ConfiguracionAcceso, UUID> {
    
    List<ConfiguracionAcceso> findBySucursalId(UUID sucursalId);
    
    List<ConfiguracionAcceso> findByTipoDeteccionAndActivoOrderByPrioridadDesc(
        TipoDeteccion tipoDeteccion, Boolean activo);
    
    @Query("SELECT ca FROM ConfiguracionAcceso ca " +
           "JOIN FETCH ca.sucursal s " +
           "JOIN FETCH s.empresa " +
           "WHERE ca.activo = true " +
           "ORDER BY ca.prioridad DESC")
    List<ConfiguracionAcceso> findAllActivasWithSucursal();
}