package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.ModoFacturacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    List<Sucursal> findByEmpresaId(Long empresaId);

    @Query("""
    SELECT DISTINCT s FROM Sucursal s
    JOIN FETCH s.empresa e
    JOIN UsuarioEmpresa ue ON ue.empresa.id = s.empresa.id
    WHERE ue.usuario.id = :usuarioId
    AND ue.activo = true
    AND s.activa = true
    AND (ue.sucursal.id = s.id OR ue.sucursal IS NULL)
    ORDER BY e.nombreRazonSocial, s.nombre
    """)
    List<Sucursal> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
    SELECT DISTINCT s FROM Sucursal s
    JOIN UsuarioEmpresa ue ON (
        ue.empresa.id = :empresaId 
        AND ue.usuario.id = :usuarioId
        AND (ue.sucursal.id = s.id OR ue.sucursal IS NULL)
    )
    WHERE s.empresa.id = :empresaId
    AND ue.activo = true
    AND s.activa = true
    ORDER BY s.nombre
    """)
    List<Sucursal> findByUsuarioIdAndEmpresaId(
        @Param("usuarioId") Long usuarioId,
        @Param("empresaId") Long empresaId
    );

    // === NUEVOS MÉTODOS PARA FACTURACIÓN ===

    // Buscar sucursales por modo de facturación
    List<Sucursal> findByModoFacturacion(ModoFacturacion modoFacturacion);

    // Buscar sucursales con terminales
    @Query("""
    SELECT s FROM Sucursal s
    LEFT JOIN FETCH s.terminales t
    WHERE s.id = :id
    """)
    Optional<Sucursal> findByIdWithTerminales(@Param("id") Long id);

    // Buscar por número de sucursal y empresa
    @Query("""
    SELECT s FROM Sucursal s
    WHERE s.empresa.id = :empresaId
    AND s.numeroSucursal = :numeroSucursal
    """)
    Optional<Sucursal> findByEmpresaIdAndNumeroSucursal(
        @Param("empresaId") Long empresaId,
        @Param("numeroSucursal") String numeroSucursal
    );

    // Contar sucursales activas por empresa
    @Query("SELECT COUNT(s) FROM Sucursal s WHERE s.empresa.id = :empresaId AND s.activa = true")
    long countActivasByEmpresaId(@Param("empresaId") Long empresaId);

    // Obtener el máximo número de sucursal por empresa
    @Query("""
    SELECT MAX(CAST(s.numeroSucursal AS integer))
    FROM Sucursal s
    WHERE s.empresa.id = :empresaId
    """)
    Integer findMaxNumeroSucursalByEmpresaId(@Param("empresaId") Long empresaId);

    // Verificar si existe número de sucursal
    @Query("""
    SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
    FROM Sucursal s
    WHERE s.empresa.id = :empresaId
    AND s.numeroSucursal = :numeroSucursal
    AND (:sucursalId IS NULL OR s.id != :sucursalId)
    """)
    boolean existsNumeroSucursalInEmpresa(
        @Param("empresaId") Long empresaId,
        @Param("numeroSucursal") String numeroSucursal,
        @Param("sucursalId") Long sucursalId
    );

    boolean existsByNumeroSucursalAndEmpresaId(String numeroSucursal, Long empresaId);
}