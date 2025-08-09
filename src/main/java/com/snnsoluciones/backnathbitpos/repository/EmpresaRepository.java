package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    
    Optional<Empresa> findByCodigo(String codigo);
    
    boolean existsByCodigo(String codigo);
    
    boolean existsByIdentificacion(String identificacion);

    // En EmpresaRepository.java
    @Query("""
    SELECT DISTINCT e FROM Empresa e
    JOIN UsuarioEmpresa ue ON ue.empresa.id = e.id
    WHERE ue.usuario.id = :usuarioId
    AND ue.activo = true
    AND e.activa = true
    ORDER BY e.nombre
    """)
    List<Empresa> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}