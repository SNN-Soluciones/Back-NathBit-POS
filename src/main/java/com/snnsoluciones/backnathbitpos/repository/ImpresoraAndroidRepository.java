package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid;
import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid.TipoUsoImpresora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImpresoraAndroidRepository extends JpaRepository<ImpresoraAndroid, Long> {

    // Buscar todas las impresoras de una sucursal
    List<ImpresoraAndroid> findBySucursalId(Long sucursalId);

    // Buscar impresoras activas de una sucursal
    List<ImpresoraAndroid> findBySucursalIdAndActivaTrue(Long sucursalId);

    // Buscar por sucursal y nombre
    Optional<ImpresoraAndroid> findBySucursalIdAndNombre(Long sucursalId, String nombre);

    // Buscar por sucursal e IP
    Optional<ImpresoraAndroid> findBySucursalIdAndIp(Long sucursalId, String ip);

    // Buscar impresoras por tipo de uso
    List<ImpresoraAndroid> findBySucursalIdAndTipoUso(Long sucursalId, TipoUsoImpresora tipoUso);

    // Buscar impresora predeterminada por tipo de uso
    Optional<ImpresoraAndroid> findBySucursalIdAndTipoUsoAndPredeterminadaTrue(
            Long sucursalId, 
            TipoUsoImpresora tipoUso
    );

    // Buscar impresora predeterminada general (sin tipo de uso específico)
    @Query("SELECT i FROM ImpresoraAndroid i WHERE i.sucursal.id = :sucursalId " +
           "AND i.predeterminada = true AND i.tipoUso IS NULL AND i.activa = true")
    Optional<ImpresoraAndroid> findPredeterminadaGeneral(@Param("sucursalId") Long sucursalId);

    // Verificar si existe una impresora con el mismo nombre en la sucursal (excepto la actual)
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ImpresoraAndroid i " +
           "WHERE i.sucursal.id = :sucursalId AND i.nombre = :nombre AND i.id != :id")
    boolean existsByNombreExceptoId(
            @Param("sucursalId") Long sucursalId, 
            @Param("nombre") String nombre, 
            @Param("id") Long id
    );

    // Verificar si existe una impresora con la misma IP en la sucursal (excepto la actual)
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ImpresoraAndroid i " +
           "WHERE i.sucursal.id = :sucursalId AND i.ip = :ip AND i.id != :id")
    boolean existsByIpExceptoId(
            @Param("sucursalId") Long sucursalId, 
            @Param("ip") String ip, 
            @Param("id") Long id
    );

    // Contar impresoras activas por sucursal
    long countBySucursalIdAndActivaTrue(Long sucursalId);
}