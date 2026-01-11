package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.DispositivoPdv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gestionar dispositivos PDV registrados
 */
@Repository
public interface DispositivoPdvRepository extends JpaRepository<DispositivoPdv, Long> {
    
    /**
     * Busca un dispositivo por su token permanente
     * 
     * @param deviceToken Token del dispositivo
     * @return Optional con el dispositivo si existe
     */
    Optional<DispositivoPdv> findByDeviceToken(String deviceToken);
    
    /**
     * Busca un dispositivo activo por su token
     * 
     * @param deviceToken Token del dispositivo
     * @return Optional con el dispositivo si existe y está activo
     */
    Optional<DispositivoPdv> findByDeviceTokenAndActivoTrue(String deviceToken);
    
    /**
     * Busca un dispositivo por UUID de hardware
     * 
     * @param uuidHardware UUID del hardware
     * @return Optional con el dispositivo si existe
     */
    Optional<DispositivoPdv> findByUuidHardware(String uuidHardware);
    
    /**
     * Lista dispositivos de una empresa
     * 
     * @param empresaId ID de la empresa
     * @return Lista de dispositivos
     */
    List<DispositivoPdv> findByEmpresaId(Long empresaId);
    
    /**
     * Lista dispositivos activos de una empresa
     * 
     * @param empresaId ID de la empresa
     * @return Lista de dispositivos activos
     */
    List<DispositivoPdv> findByEmpresaIdAndActivoTrue(Long empresaId);
    
    /**
     * Lista dispositivos de una sucursal
     * 
     * @param sucursalId ID de la sucursal
     * @return Lista de dispositivos
     */
    List<DispositivoPdv> findBySucursalId(Long sucursalId);
    
    /**
     * Lista dispositivos activos de una sucursal
     * 
     * @param sucursalId ID de la sucursal
     * @return Lista de dispositivos activos
     */
    List<DispositivoPdv> findBySucursalIdAndActivoTrue(Long sucursalId);
    
    /**
     * Obtiene dispositivos con su información de empresa y sucursal
     * Optimizado con JOIN FETCH
     * 
     * @param empresaId ID de la empresa
     * @return Lista de dispositivos con relaciones cargadas
     */
    @Query("SELECT d FROM DispositivoPdv d " +
           "LEFT JOIN FETCH d.empresa " +
           "LEFT JOIN FETCH d.sucursal " +
           "WHERE d.empresa.id = :empresaId " +
           "ORDER BY d.createdAt DESC")
    List<DispositivoPdv> findByEmpresaIdWithRelations(@Param("empresaId") Long empresaId);
    
    /**
     * Cuenta dispositivos activos de una empresa
     * 
     * @param empresaId ID de la empresa
     * @return Cantidad de dispositivos activos
     */
    long countByEmpresaIdAndActivoTrue(Long empresaId);
    
    /**
     * Cuenta dispositivos activos de una sucursal
     * 
     * @param sucursalId ID de la sucursal
     * @return Cantidad de dispositivos activos
     */
    long countBySucursalIdAndActivoTrue(Long sucursalId);
    
    /**
     * Verifica si existe un dispositivo con ese nombre en la sucursal
     * 
     * @param nombre Nombre del dispositivo
     * @param sucursalId ID de la sucursal
     * @return true si existe
     */
    boolean existsByNombreAndSucursalId(String nombre, Long sucursalId);
    
    /**
     * Obtiene dispositivos inactivos desde hace más de X días
     * Útil para limpieza o alertas
     * 
     * @param fechaLimite Fecha límite de último uso
     * @return Lista de dispositivos inactivos
     */
    @Query("SELECT d FROM DispositivoPdv d WHERE d.activo = true " +
           "AND (d.ultimoUso IS NULL OR d.ultimoUso < :fechaLimite) " +
           "ORDER BY d.ultimoUso ASC NULLS FIRST")
    List<DispositivoPdv> findDispositivosInactivosDesde(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    /**
     * Obtiene dispositivos ordenados por último uso
     * 
     * @param empresaId ID de la empresa
     * @return Lista de dispositivos ordenados
     */
    @Query("SELECT d FROM DispositivoPdv d WHERE d.empresa.id = :empresaId " +
           "ORDER BY d.ultimoUso DESC NULLS LAST")
    List<DispositivoPdv> findByEmpresaIdOrderByUltimoUsoDesc(@Param("empresaId") Long empresaId);
    
    /**
     * Busca dispositivos por plataforma
     * 
     * @param empresaId ID de la empresa
     * @param plataforma Plataforma (ANDROID, IOS, WEB)
     * @return Lista de dispositivos
     */
    @Query("SELECT d FROM DispositivoPdv d WHERE d.empresa.id = :empresaId " +
           "AND d.plataforma = :plataforma " +
           "ORDER BY d.createdAt DESC")
    List<DispositivoPdv> findByEmpresaIdAndPlataforma(
        @Param("empresaId") Long empresaId,
        @Param("plataforma") String plataforma
    );
}