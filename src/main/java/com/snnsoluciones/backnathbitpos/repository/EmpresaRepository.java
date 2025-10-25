package com.snnsoluciones.backnathbitpos.repository;

import com.google.common.io.Files;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

  boolean existsByEmail(String email);

  List<Empresa> findAllByActivaTrue();

  Optional<Empresa> findByIdentificacion(String identificacion);

  Optional<Empresa> findByEmail(String email);
}