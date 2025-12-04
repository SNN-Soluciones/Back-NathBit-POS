package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.CodigoRegistro;
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
 * Repositorio para entidad CodigoRegistro (OTP para dispositivos)
 */
@Repository
public interface CodigoRegistroRepository extends JpaRepository<CodigoRegistro, Long> {

    /**
     * Busca código activo por tenant y código OTP
     * (no expirado y no usado)
     */
    @Query("SELECT c FROM CodigoRegistro c WHERE " +
           "c.tenant.id = :tenantId " +
           "AND c.codigo = :codigo " +
           "AND c.usado = false " +
           "AND c.expiraAt > :ahora")
    Optional<CodigoRegistro> findCodigoValido(
        @Param("tenantId") Long tenantId,
        @Param("codigo") String codigo,
        @Param("ahora") LocalDateTime ahora
    );

    /**
     * Busca código activo por código OTP únicamente
     * (no expirado y no usado)
     */
    @Query("SELECT c FROM CodigoRegistro c WHERE " +
           "c.codigo = :codigo " +
           "AND c.usado = false " +
           "AND c.expiraAt > :ahora")
    Optional<CodigoRegistro> findCodigoValidoPorCodigo(
        @Param("codigo") String codigo,
        @Param("ahora") LocalDateTime ahora
    );

    /**
     * Lista códigos pendientes de un tenant (no usados)
     */
    List<CodigoRegistro> findByTenantIdAndUsadoFalseOrderByCreatedAtDesc(Long tenantId);

    /**
     * Lista códigos de un tenant
     */
    List<CodigoRegistro> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    /**
     * Lista códigos pendientes para un dispositivo específico
     */
    @Query("SELECT c FROM CodigoRegistro c WHERE " +
           "c.tenant.id = :tenantId " +
           "AND c.dispositivoNombre = :nombreDispositivo " +
           "AND c.usado = false " +
           "AND c.expiraAt > :ahora " +
           "ORDER BY c.createdAt DESC")
    List<CodigoRegistro> findCodigosPendientesPorDispositivo(
        @Param("tenantId") Long tenantId,
        @Param("nombreDispositivo") String nombreDispositivo,
        @Param("ahora") LocalDateTime ahora
    );

    /**
     * Cuenta códigos activos de un tenant
     */
    @Query("SELECT COUNT(c) FROM CodigoRegistro c WHERE " +
           "c.tenant.id = :tenantId " +
           "AND c.usado = false " +
           "AND c.expiraAt > :ahora")
    long countCodigosActivosPorTenant(@Param("tenantId") Long tenantId, @Param("ahora") LocalDateTime ahora);

    /**
     * Verifica si hay código activo para un dispositivo
     */
    @Query("SELECT COUNT(c) > 0 FROM CodigoRegistro c WHERE " +
           "c.tenant.id = :tenantId " +
           "AND c.dispositivoNombre = :nombreDispositivo " +
           "AND c.usado = false " +
           "AND c.expiraAt > :ahora")
    boolean existeCodigoActivoParaDispositivo(
        @Param("tenantId") Long tenantId,
        @Param("nombreDispositivo") String nombreDispositivo,
        @Param("ahora") LocalDateTime ahora
    );

    /**
     * Marca un código como usado
     */
    @Modifying
    @Query("UPDATE CodigoRegistro c SET c.usado = true, c.usadoAt = :fecha WHERE c.id = :id")
    void marcarComoUsado(@Param("id") Long id, @Param("fecha") LocalDateTime fecha);

    /**
     * Invalida códigos anteriores de un dispositivo
     * (los marca como usados para que no se puedan usar)
     */
    @Modifying
    @Query("UPDATE CodigoRegistro c SET c.usado = true, c.usadoAt = :fecha WHERE " +
           "c.tenant.id = :tenantId " +
           "AND c.dispositivoNombre = :nombreDispositivo " +
           "AND c.usado = false")
    void invalidarCodigosAnteriores(
        @Param("tenantId") Long tenantId,
        @Param("nombreDispositivo") String nombreDispositivo,
        @Param("fecha") LocalDateTime fecha
    );

    /**
     * Elimina códigos expirados (para limpieza periódica)
     */
    @Modifying
    @Query("DELETE FROM CodigoRegistro c WHERE c.expiraAt < :fecha")
    int eliminarCodigosExpirados(@Param("fecha") LocalDateTime fecha);

    /**
     * Elimina códigos usados con más de X días de antigüedad
     */
    @Modifying
    @Query("DELETE FROM CodigoRegistro c WHERE c.usado = true AND c.usadoAt < :fecha")
    int eliminarCodigosUsadosAntiguos(@Param("fecha") LocalDateTime fecha);

    /**
     * Lista códigos recientes de un tenant (últimas 24 horas)
     */
    @Query("SELECT c FROM CodigoRegistro c WHERE " +
           "c.tenant.id = :tenantId " +
           "AND c.createdAt > :fecha " +
           "ORDER BY c.createdAt DESC")
    List<CodigoRegistro> findCodigosRecientes(@Param("tenantId") Long tenantId, @Param("fecha") LocalDateTime fecha);
}
