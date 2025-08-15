package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

  boolean existsByIdentificacion(String identificacion);

  @Query("""
      SELECT DISTINCT e FROM Empresa e
      JOIN UsuarioEmpresa ue ON ue.empresa.id = e.id
      WHERE ue.usuario.id = :usuarioId
      AND ue.activo = true
      ORDER BY e.nombreRazonSocial, e.nombreComercial
      """)
  Page<Empresa> findByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);

  // Buscar empresas por régimen tributario
  @Query("SELECT e FROM Empresa e WHERE e.regimenTributario = :regimen")
  List<Empresa> findByRegimenTributario(@Param("regimen") String regimen);

  // Verificar si una empresa tiene facturación electrónica configurada
  @Query("""
      SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
      FROM Empresa e
      JOIN e.configHacienda ch
      WHERE e.id = :empresaId
      AND e.requiereHacienda = true
      AND ch.usuarioHacienda IS NOT NULL
      """)
  boolean tieneFacturacionElectronicaConfigurada(@Param("empresaId") Long empresaId);

  boolean existsByEmail(String email);

  List<Empresa> findAllByActivaTrue();
}