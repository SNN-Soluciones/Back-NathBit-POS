package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Long> {
    
    Optional<Sucursal> findByCodigo(String codigo);
    
    List<Sucursal> findByEmpresaId(Long empresaId);
    
    boolean existsByCodigo(String codigo);

    @Query("""
    SELECT DISTINCT s FROM Sucursal s
    JOIN FETCH s.empresa e
    JOIN UsuarioEmpresa ue ON ue.empresa.id = s.empresa.id
    WHERE ue.usuario.id = :usuarioId
    AND ue.activo = true
    AND s.activa = true
    AND (ue.sucursal.id = s.id OR ue.sucursal IS NULL)
    ORDER BY e.nombre, s.nombre
    """)
    List<Sucursal> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    // En SucursalRepository.java
    @Query("""
    SELECT DISTINCT s FROM Sucursal s
    JOIN UsuarioEmpresa ue ON (
        ue.empresa.id = :empresaId\s
        AND ue.usuario.id = :usuarioId
        AND (ue.sucursal.id = s.id OR ue.sucursal IS NULL)
    )
    WHERE s.empresa.id = :empresaId
    AND ue.activo = true
    AND s.activa = true
    ORDER BY s.nombre
   \s""")
    List<Sucursal> findByUsuarioIdAndEmpresaId(
        @Param("usuarioId") Long usuarioId,
        @Param("empresaId") Long empresaId
    );
}