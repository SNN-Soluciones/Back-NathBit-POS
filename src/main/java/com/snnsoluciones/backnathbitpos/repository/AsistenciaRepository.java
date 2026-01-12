package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gestionar asistencias de usuarios
 */
@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    
    /**
     * Busca la asistencia de un usuario en una fecha específica
     * 
     * @param usuarioId ID del usuario
     * @param fecha Fecha a buscar
     * @return Optional con la asistencia si existe
     */
    Optional<Asistencia> findByUsuarioIdAndFecha(Long usuarioId, LocalDate fecha);
    
    /**
     * Verifica si un usuario tiene entrada activa HOY (sin salida)
     * 
     * @param usuarioId ID del usuario
     * @param fecha Fecha (normalmente HOY)
     * @return true si tiene entrada activa
     */
    @Query("SELECT COUNT(a) > 0 FROM Asistencia a " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.fecha = :fecha " +
           "AND a.horaSalida IS NULL")
    boolean tieneEntradaActiva(
        @Param("usuarioId") Long usuarioId,
        @Param("fecha") LocalDate fecha
    );
    
    /**
     * Busca asistencias de un usuario en un rango de fechas
     * 
     * @param usuarioId ID del usuario
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de asistencias
     */
    @Query("SELECT a FROM Asistencia a " +
           "WHERE a.usuario.id = :usuarioId " +
           "AND a.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY a.fecha DESC")
    List<Asistencia> findByUsuarioIdAndFechaBetween(
        @Param("usuarioId") Long usuarioId,
        @Param("fechaInicio") LocalDate fechaInicio,
        @Param("fechaFin") LocalDate fechaFin
    );
    
    /**
     * Lista asistencias de una empresa en una fecha
     * 
     * @param empresaId ID de la empresa
     * @param fecha Fecha
     * @return Lista de asistencias
     */
    @Query("SELECT a FROM Asistencia a " +
           "LEFT JOIN FETCH a.usuario " +
           "WHERE a.empresa.id = :empresaId " +
           "AND a.fecha = :fecha " +
           "ORDER BY a.horaEntrada DESC")
    List<Asistencia> findByEmpresaIdAndFecha(
        @Param("empresaId") Long empresaId,
        @Param("fecha") LocalDate fecha
    );
    
    /**
     * Lista asistencias activas (sin salida) de una empresa HOY
     * 
     * @param empresaId ID de la empresa
     * @param fecha Fecha (normalmente HOY)
     * @return Lista de asistencias activas
     */
    @Query("SELECT a FROM Asistencia a " +
           "LEFT JOIN FETCH a.usuario " +
           "WHERE a.empresa.id = :empresaId " +
           "AND a.fecha = :fecha " +
           "AND a.horaSalida IS NULL " +
           "ORDER BY a.horaEntrada ASC")
    List<Asistencia> findEntradasActivasByEmpresa(
        @Param("empresaId") Long empresaId,
        @Param("fecha") LocalDate fecha
    );
    
    /**
     * Lista asistencias de una sucursal en una fecha
     * 
     * @param sucursalId ID de la sucursal
     * @param fecha Fecha
     * @return Lista de asistencias
     */
    List<Asistencia> findBySucursalIdAndFecha(Long sucursalId, LocalDate fecha);
    
    /**
     * Cuenta usuarios con entrada activa en una empresa HOY
     * 
     * @param empresaId ID de la empresa
     * @param fecha Fecha (normalmente HOY)
     * @return Cantidad de usuarios presentes
     */
    @Query("SELECT COUNT(a) FROM Asistencia a " +
           "WHERE a.empresa.id = :empresaId " +
           "AND a.fecha = :fecha " +
           "AND a.horaSalida IS NULL")
    long contarUsuariosPresentes(
        @Param("empresaId") Long empresaId,
        @Param("fecha") LocalDate fecha
    );
}