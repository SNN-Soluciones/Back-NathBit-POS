package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.PlataformaDigitalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlataformaDigitalConfigRepository extends JpaRepository<PlataformaDigitalConfig, Long> {

    boolean existsByEmpresaIdAndCodigo(Long empresaId, String codigo);

    // ⭐ NUEVO - Listar por empresa Y sucursal
    @Query("SELECT p FROM PlataformaDigitalConfig p " +
        "WHERE p.empresa.id = :empresaId " +
        "AND (p.sucursal.id = :sucursalId OR p.sucursal IS NULL) " +
        "AND p.activo = true " +
        "ORDER BY p.orden, p.nombre")
    List<PlataformaDigitalConfig> findActivasByEmpresaIdAndSucursalId(
        @Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId
    );

    // Listar solo por empresa (todas las sucursales)
    @Query("SELECT p FROM PlataformaDigitalConfig p " +
        "WHERE p.empresa.id = :empresaId " +
        "AND p.activo = true " +
        "ORDER BY p.orden, p.nombre")
    List<PlataformaDigitalConfig> findActivasByEmpresaId(@Param("empresaId") Long empresaId);

    // Buscar por código, empresa y sucursal
    Optional<PlataformaDigitalConfig> findByEmpresaIdAndSucursalIdAndCodigo(
        Long empresaId, Long sucursalId, String codigo);

    // Verificar existencia
    boolean existsByEmpresaIdAndSucursalIdAndCodigo(
        Long empresaId, Long sucursalId, String codigo);

    // Listar todas por empresa
    List<PlataformaDigitalConfig> findByEmpresaIdOrderByOrdenAsc(Long empresaId);

    // ⭐ NUEVO - Listar por sucursal específica
    List<PlataformaDigitalConfig> findBySucursalIdOrderByOrdenAsc(Long sucursalId);
}