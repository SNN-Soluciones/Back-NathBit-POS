package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.TokenRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gestionar tokens de registro de dispositivos PDV
 */
@Repository
public interface TokenRegistroRepository extends JpaRepository<TokenRegistro, Long> {
    
    /**
     * Busca un token por su valor
     * 
     * @param token Token a buscar
     * @return Optional con el token si existe
     */
    Optional<TokenRegistro> findByToken(String token);
    
    /**
     * Busca un token válido (no usado y no expirado)
     * 
     * @param token Token a buscar
     * @param ahora Fecha/hora actual para comparar expiración
     * @return Optional con el token si es válido
     */
    @Query("SELECT t FROM TokenRegistro t WHERE t.token = :token " +
           "AND t.usado = false " +
           "AND t.expiraEn > :ahora")
    Optional<TokenRegistro> findByTokenAndValidoTrue(
        @Param("token") String token,
        @Param("ahora") LocalDateTime ahora
    );
    
    /**
     * Obtiene tokens activos de una empresa
     * 
     * @param empresaId ID de la empresa
     * @param ahora Fecha/hora actual
     * @return Lista de tokens no usados y no expirados
     */
    @Query("SELECT t FROM TokenRegistro t WHERE t.empresa.id = :empresaId " +
           "AND t.usado = false " +
           "AND t.expiraEn > :ahora " +
           "ORDER BY t.createdAt DESC")
    List<TokenRegistro> findTokensActivosByEmpresa(
        @Param("empresaId") Long empresaId,
        @Param("ahora") LocalDateTime ahora
    );
    
    /**
     * Obtiene tokens activos de una sucursal
     * 
     * @param sucursalId ID de la sucursal
     * @param ahora Fecha/hora actual
     * @return Lista de tokens no usados y no expirados
     */
    @Query("SELECT t FROM TokenRegistro t WHERE t.sucursal.id = :sucursalId " +
           "AND t.usado = false " +
           "AND t.expiraEn > :ahora " +
           "ORDER BY t.createdAt DESC")
    List<TokenRegistro> findTokensActivosBySucursal(
        @Param("sucursalId") Long sucursalId,
        @Param("ahora") LocalDateTime ahora
    );
    
    /**
     * Elimina tokens expirados (para limpieza periódica)
     * 
     * @param fechaLimite Fecha límite para considerar expirados
     * @return Cantidad de tokens eliminados
     */
    @Modifying
    @Query("DELETE FROM TokenRegistro t WHERE t.expiraEn < :fechaLimite")
    int eliminarTokensExpirados(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    /**
     * Elimina tokens usados con antigüedad mayor a X días
     * 
     * @param fechaLimite Fecha límite para tokens usados
     * @return Cantidad de tokens eliminados
     */
    @Modifying
    @Query("DELETE FROM TokenRegistro t WHERE t.usado = true AND t.usedAt < :fechaLimite")
    int eliminarTokensUsadosAntiguos(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    /**
     * Cuenta tokens activos de una empresa
     * 
     * @param empresaId ID de la empresa
     * @param ahora Fecha/hora actual
     * @return Cantidad de tokens activos
     */
    @Query("SELECT COUNT(t) FROM TokenRegistro t WHERE t.empresa.id = :empresaId " +
           "AND t.usado = false " +
           "AND t.expiraEn > :ahora")
    long contarTokensActivosByEmpresa(
        @Param("empresaId") Long empresaId,
        @Param("ahora") LocalDateTime ahora
    );
    
    /**
     * Verifica si existe un token válido para una sucursal y nombre de dispositivo
     * Útil para evitar duplicados
     * 
     * @param sucursalId ID de la sucursal
     * @param nombreDispositivo Nombre del dispositivo
     * @param ahora Fecha/hora actual
     * @return true si existe un token activo con ese nombre
     */
    @Query("SELECT COUNT(t) > 0 FROM TokenRegistro t " +
           "WHERE t.sucursal.id = :sucursalId " +
           "AND t.nombreDispositivo = :nombreDispositivo " +
           "AND t.usado = false " +
           "AND t.expiraEn > :ahora")
    boolean existsTokenActivoBySucursalAndNombre(
        @Param("sucursalId") Long sucursalId,
        @Param("nombreDispositivo") String nombreDispositivo,
        @Param("ahora") LocalDateTime ahora
    );
}