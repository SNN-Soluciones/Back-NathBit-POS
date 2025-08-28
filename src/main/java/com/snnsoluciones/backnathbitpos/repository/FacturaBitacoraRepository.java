package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.dto.factura.ResumenBitacoraDto;
import com.snnsoluciones.backnathbitpos.entity.FacturaBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión de bitácora de facturación electrónica
 */
@Repository
public interface FacturaBitacoraRepository extends JpaRepository<FacturaBitacora, Long> {

    /**
     * Verificar si existe bitácora para una factura
     */
    boolean existsByFacturaId(Long facturaId);

    /**
     * Buscar por factura ID
     */
    Optional<FacturaBitacora> findByFacturaId(Long facturaId);

    /**
     * Buscar por clave
     */
    Optional<FacturaBitacora> findByClave(String clave);

    /**
     * Obtener facturas pendientes de procesar
     * - Estado PENDIENTE o ERROR (con intentos restantes)
     * - proximoIntento es null o ya pasó
     * - Ordenado por createdAt para respetar FIFO
     */
    @Query("""
        SELECT b FROM FacturaBitacora b
        WHERE (b.estado = 'PENDIENTE' 
               OR (b.estado = 'ERROR' AND b.intentos < :maxIntentos))
          AND (b.proximoIntento IS NULL OR b.proximoIntento <= :ahora)
        ORDER BY b.createdAt ASC
        """)
    List<FacturaBitacora> findPendientes(
        @Param("maxIntentos") int maxIntentos,
        @Param("ahora") LocalDateTime ahora,
        Pageable pageable
    );

    /**
     * Versión simplificada para el servicio (usa valores por defecto)
     */
    default List<FacturaBitacora> findPendientes(Pageable pageable) {
        return findPendientes(3, LocalDateTime.now(), pageable);
    }

    /**
     * Buscar con lock para evitar procesamiento concurrente
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM FacturaBitacora b WHERE b.id = :id")
    Optional<FacturaBitacora> findByIdWithLock(@Param("id") Long id);

    /**
     * Buscar bitácoras con filtros complejos
     */
    @Query("""
        SELECT b FROM FacturaBitacora b
        JOIN Factura f ON b.facturaId = f.id
        JOIN Sucursal s ON f.sucursal.id = s.id
        WHERE (:estado IS NULL OR b.estado = :estado)
          AND (:empresaId IS NULL OR s.empresa.id = :empresaId)
          AND (:sucursalId IS NULL OR s.id = :sucursalId)
          AND (:fechaDesde IS NULL OR b.createdAt >= :fechaDesde)
          AND (:fechaHasta IS NULL OR b.createdAt <= :fechaHasta)
        """)
    Page<FacturaBitacora> buscarConFiltros(
        @Param("estado") EstadoBitacora estado,
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta,
        Pageable pageable
    );

    /**
     * Contar por estado para dashboard
     */
    @Query("""
        SELECT 
            COUNT(b) as total,
            SUM(CASE WHEN b.estado = 'PENDIENTE' THEN 1 ELSE 0 END) as pendientes,
            SUM(CASE WHEN b.estado = 'PROCESANDO' THEN 1 ELSE 0 END) as procesando,
            SUM(CASE WHEN b.estado = 'ACEPTADA' THEN 1 ELSE 0 END) as aceptadas,
            SUM(CASE WHEN b.estado = 'RECHAZADA' THEN 1 ELSE 0 END) as rechazadas,
            SUM(CASE WHEN b.estado = 'ERROR' THEN 1 ELSE 0 END) as conError
        FROM FacturaBitacora b
        JOIN Factura f ON b.facturaId = f.id
        JOIN Sucursal s ON f.sucursal.id = s.id
        WHERE :empresaId IS NULL OR s.empresa.id = :empresaId
        """)
    Object[] contarPorEstados(@Param("empresaId") Long empresaId);

    /**
     * Obtener resumen para dashboard
     */
    @Query("""
        SELECT new com.snnsoluciones.backnathbitpos.dto.facturacion.ResumenBitacoraDto(
            COUNT(b),
            SUM(CASE WHEN b.estado = 'PENDIENTE' THEN 1 ELSE 0 END),
            SUM(CASE WHEN b.estado = 'PROCESANDO' THEN 1 ELSE 0 END),
            SUM(CASE WHEN b.estado = 'ACEPTADA' THEN 1 ELSE 0 END),
            SUM(CASE WHEN b.estado = 'RECHAZADA' THEN 1 ELSE 0 END),
            SUM(CASE WHEN b.estado = 'ERROR' THEN 1 ELSE 0 END),
            CAST(SUM(CASE WHEN b.estado = 'ACEPTADA' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(b), 0) AS DOUBLE),
            AVG(TIMESTAMPDIFF(SECOND, b.createdAt, b.procesadoAt)),
            MAX(b.procesadoAt)
        )
        FROM FacturaBitacora b
        JOIN Factura f ON b.facturaId = f.id
        JOIN Sucursal s ON f.sucursal.id = s.id
        WHERE (:empresaId IS NULL OR s.empresa.id = :empresaId)
          AND b.procesadoAt IS NOT NULL
        """)
    ResumenBitacoraDto obtenerResumen(@Param("empresaId") Long empresaId);

    /**
     * Buscar bitácoras procesándose por más tiempo del esperado (stuck)
     */
    @Query("""
        SELECT b FROM FacturaBitacora b
        WHERE b.estado = 'PROCESANDO'
          AND b.updatedAt < :tiempoLimite
        """)
    List<FacturaBitacora> findStuckProcessing(@Param("tiempoLimite") LocalDateTime tiempoLimite);

    /**
     * Actualizar estado masivamente (útil para mantenimiento)
     */
    @Modifying
    @Query("""
        UPDATE FacturaBitacora b
        SET b.estado = :nuevoEstado, b.updatedAt = CURRENT_TIMESTAMP
        WHERE b.estado = :estadoActual
          AND b.createdAt BETWEEN :fechaDesde AND :fechaHasta
        """)
    int actualizarEstadoMasivo(
        @Param("estadoActual") EstadoBitacora estadoActual,
        @Param("nuevoEstado") EstadoBitacora nuevoEstado,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta
    );

    /**
     * Limpiar bitácoras antiguas (mantenimiento)
     */
    @Modifying
    @Query("""
        DELETE FROM FacturaBitacora b
        WHERE b.estado IN ('ACEPTADA', 'RECHAZADA')
          AND b.procesadoAt < :fechaLimite
        """)
    int limpiarAntiguos(@Param("fechaLimite") LocalDateTime fechaLimite);

    /**
     * Obtener bitácoras para reporte
     */
    @Query("""
        SELECT b FROM FacturaBitacora b
        JOIN FETCH Factura f ON b.facturaId = f.id
        JOIN FETCH f.sucursal s
        JOIN FETCH s.empresa
        WHERE b.procesadoAt BETWEEN :fechaInicio AND :fechaFin
          AND b.estado IN ('ACEPTADA', 'RECHAZADA')
        ORDER BY b.procesadoAt DESC
        """)
    List<FacturaBitacora> findParaReporte(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Contar errores recurrentes por mensaje
     */
    @Query("""
        SELECT b.ultimoError, COUNT(b) as cantidad
        FROM FacturaBitacora b
        WHERE b.estado = 'ERROR'
          AND b.updatedAt >= :desde
        GROUP BY b.ultimoError
        ORDER BY COUNT(b) DESC
        """)
    List<Object[]> topErrores(@Param("desde") LocalDateTime desde);

    /**
     * Buscar por múltiples claves (útil para consultas batch)
     */
    @Query("SELECT b FROM FacturaBitacora b WHERE b.clave IN :claves")
    List<FacturaBitacora> findByClaves(@Param("claves") List<String> claves);

    /**
     * Estadísticas por día para gráficos
     */
    @Query(value = """
        SELECT 
            DATE(b.created_at) as fecha,
            COUNT(*) as total,
            SUM(CASE WHEN b.estado = 'ACEPTADA' THEN 1 ELSE 0 END) as aceptadas,
            SUM(CASE WHEN b.estado = 'RECHAZADA' THEN 1 ELSE 0 END) as rechazadas,
            SUM(CASE WHEN b.estado = 'ERROR' THEN 1 ELSE 0 END) as errores
        FROM factura_bitacora b
        WHERE b.created_at >= :desde
        GROUP BY DATE(b.created_at)
        ORDER BY DATE(b.created_at) DESC
        """, nativeQuery = true)
    List<Object[]> estadisticasPorDia(@Param("desde") LocalDateTime desde);

    /**
     * Verificar salud del sistema (alertas)
     */
    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(CASE WHEN b.estado = 'ERROR' AND b.intentos >= 3 THEN 1 END) > 10 THEN 'CRITICO'
                WHEN COUNT(CASE WHEN b.estado = 'PENDIENTE' AND b.createdAt < :hace1Hora THEN 1 END) > 20 THEN 'ALERTA'
                WHEN COUNT(CASE WHEN b.estado = 'PROCESANDO' AND b.updatedAt < :hace30Min THEN 1 END) > 5 THEN 'ADVERTENCIA'
                ELSE 'OK'
            END as estado
        FROM FacturaBitacora b
        """)
    String verificarSalud(
        @Param("hace1Hora") LocalDateTime hace1Hora,
        @Param("hace30Min") LocalDateTime hace30Min
    );
}