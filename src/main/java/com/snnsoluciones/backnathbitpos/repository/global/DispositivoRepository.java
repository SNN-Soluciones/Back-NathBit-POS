package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo.Plataforma;
import com.snnsoluciones.backnathbitpos.entity.global.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad Dispositivo
 */
@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    @Query("SELECT d FROM Dispositivo d JOIN FETCH d.tenant WHERE d.tipo = :tipo AND d.activo = true")
    List<Dispositivo> findByTipoAndActivoTrue(@Param("tipo") String tipo);

    /**
     * Busca dispositivo por token único
     */
    Optional<Dispositivo> findByToken(String token);

    /**
     * Busca dispositivo activo por token
     */
    Optional<Dispositivo> findByTokenAndActivoTrue(String token);

    /**
     * Verifica si existe un dispositivo con el token dado
     */
    boolean existsByToken(String token);

    /**
     * Lista dispositivos de un tenant
     */
    List<Dispositivo> findByTenant(Tenant tenant);

    /**
     * Lista dispositivos activos de un tenant
     */
    List<Dispositivo> findByTenantAndActivoTrue(Tenant tenant);

    /**
     * Lista dispositivos por ID de tenant
     */
    List<Dispositivo> findByTenantId(Long tenantId);

    /**
     * Lista dispositivos activos por ID de tenant
     */
    List<Dispositivo> findByTenantIdAndActivoTrue(Long tenantId);

    /**
     * Lista dispositivos por ID de tenant ordenados por último uso
     */
    List<Dispositivo> findByTenantIdOrderByUltimoUsoDesc(Long tenantId);

    /**
     * Lista dispositivos por plataforma
     */
    List<Dispositivo> findByPlataforma(Plataforma plataforma);

    /**
     * Lista dispositivos activos de un tenant por plataforma
     */
    List<Dispositivo> findByTenantIdAndPlataformaAndActivoTrue(Long tenantId, Plataforma plataforma);

    /**
     * Cuenta dispositivos activos de un tenant
     */
    long countByTenantIdAndActivoTrue(Long tenantId);

    /**
     * Cuenta dispositivos por plataforma de un tenant
     */
    long countByTenantIdAndPlataforma(Long tenantId, Plataforma plataforma);

    /**
     * Busca dispositivos sin uso reciente (para limpieza)
     */
    @Query("SELECT d FROM Dispositivo d WHERE d.ultimoUso < :fecha OR d.ultimoUso IS NULL")
    List<Dispositivo> findDispositivosSinUsoDesde(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca dispositivos inactivos de un tenant
     */
    List<Dispositivo> findByTenantIdAndActivoFalse(Long tenantId);

    /**
     * Actualiza último uso de un dispositivo
     */
    @Modifying
    @Query("UPDATE Dispositivo d SET d.ultimoUso = :fecha WHERE d.id = :id")
    void actualizarUltimoUso(@Param("id") Long id, @Param("fecha") LocalDateTime fecha);

    /**
     * Desactiva un dispositivo
     */
    @Modifying
    @Query("UPDATE Dispositivo d SET d.activo = false, d.updatedAt = :fecha WHERE d.id = :id")
    void desactivar(@Param("id") Long id, @Param("fecha") LocalDateTime fecha);

    /**
     * Desactiva todos los dispositivos de un tenant
     */
    @Modifying
    @Query("UPDATE Dispositivo d SET d.activo = false, d.updatedAt = :fecha WHERE d.tenant.id = :tenantId")
    void desactivarPorTenant(@Param("tenantId") Long tenantId, @Param("fecha") LocalDateTime fecha);

    /**
     * Valida y retorna el tenant asociado al token
     */
    @Query("SELECT d.tenant FROM Dispositivo d WHERE d.token = :token AND d.activo = true")
    Optional<Tenant> findTenantByToken(@Param("token") String token);

    /**
     * Busca dispositivos por nombre (parcial) en un tenant
     */
    @Query("SELECT d FROM Dispositivo d WHERE d.tenant.id = :tenantId " +
           "AND LOWER(d.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<Dispositivo> buscarPorNombre(@Param("tenantId") Long tenantId, @Param("nombre") String nombre);

    @Query("SELECT d FROM Dispositivo d JOIN FETCH d.tenant WHERE d.token = :token AND d.activo = true")
    Optional<Dispositivo> findByTokenAndActivoTrueWithTenant(@Param("token") String token);

    @Query("SELECT d FROM Dispositivo d JOIN FETCH d.tenant WHERE d.token = :token AND d.activo = true")
    Optional<Dispositivo> findByTokenWithTenant(@Param("token") String token);
}
