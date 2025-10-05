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

}