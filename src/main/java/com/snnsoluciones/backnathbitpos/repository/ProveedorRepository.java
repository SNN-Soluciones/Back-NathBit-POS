package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

  // Buscar por empresa
  List<Proveedor> findByEmpresaIdAndActivoTrueOrderByNombreComercialAsc(Long empresaId);

  // Buscar por identificación
  Optional<Proveedor> findByEmpresaIdAndNumeroIdentificacion(Long empresaId,
      String numeroIdentificacion);

  // Verificar si existe
  boolean existsByEmpresaIdAndNumeroIdentificacion(Long empresaId, String numeroIdentificacion);

  // Verificar duplicado al actualizar
  boolean existsByEmpresaIdAndNumeroIdentificacionAndIdNot(Long empresaId,
      String numeroIdentificacion, Long id);

  // Búsqueda por término
  @Query("""
      SELECT p FROM Proveedor p
      WHERE p.empresa.id = :empresaId
        AND p.activo = true
        AND (LOWER(p.numeroIdentificacion) LIKE LOWER(CONCAT('%', :termino, '%'))
             OR LOWER(p.nombreComercial) LIKE LOWER(CONCAT('%', :termino, '%'))
             OR LOWER(p.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%'))
             OR LOWER(p.email) LIKE LOWER(CONCAT('%', :termino, '%')))
      ORDER BY p.nombreComercial
      """)
  List<Proveedor> buscarPorEmpresaYTermino(@Param("empresaId") Long empresaId,
      @Param("termino") String termino);

  Optional<Proveedor> findByNumeroIdentificacionAndEmpresaId(String numeroIdentificacion,
      Long empresaId);

  /**
   * Buscar proveedores GLOBALES de una empresa
   */
  List<Proveedor> findByEmpresaIdAndSucursalIdIsNullAndActivoTrueOrderByNombreComercialAsc(Long empresaId);

  /**
   * Buscar proveedores LOCALES de una sucursal
   */
  List<Proveedor> findByEmpresaIdAndSucursalIdAndActivoTrueOrderByNombreComercialAsc(Long empresaId, Long sucursalId);

  /**
   * Verificar si existe proveedor global por identificación
   */
  boolean existsByEmpresaIdAndNumeroIdentificacionAndSucursalIdIsNull(Long empresaId, String numeroIdentificacion);

  /**
   * Verificar si existe proveedor local por identificación
   */
  boolean existsByEmpresaIdAndNumeroIdentificacionAndSucursalId(Long empresaId, String numeroIdentificacion, Long sucursalId);

  /**
   * Buscar proveedor global por identificación
   */
  Optional<Proveedor> findByEmpresaIdAndNumeroIdentificacionAndSucursalIdIsNull(Long empresaId, String numeroIdentificacion);

  /**
   * Buscar proveedor local por identificación
   */
  Optional<Proveedor> findByEmpresaIdAndNumeroIdentificacionAndSucursalId(Long empresaId, String numeroIdentificacion, Long sucursalId);

  /**
   * Búsqueda global por término
   */
  @Query("""
        SELECT p FROM Proveedor p
        WHERE p.empresa.id = :empresaId
          AND p.sucursal.id IS NULL
          AND p.activo = true
          AND (LOWER(p.numeroIdentificacion) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(p.nombreComercial) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(p.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%')))
        ORDER BY p.nombreComercial
        """)
  List<Proveedor> buscarGlobalesPorTermino(@Param("empresaId") Long empresaId,
      @Param("termino") String termino);

  /**
   * Búsqueda local por término
   */
  @Query("""
        SELECT p FROM Proveedor p
        WHERE p.empresa.id = :empresaId
          AND p.sucursal.id = :sucursalId
          AND p.activo = true
          AND (LOWER(p.numeroIdentificacion) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(p.nombreComercial) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(p.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%')))
        ORDER BY p.nombreComercial
        """)
  List<Proveedor> buscarLocalesPorTermino(@Param("empresaId") Long empresaId,
      @Param("sucursalId") Long sucursalId,
      @Param("termino") String termino);
}