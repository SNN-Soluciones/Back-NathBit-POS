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

    @Query("""
    SELECT DISTINCT e FROM Empresa e
    JOIN UsuarioEmpresa ue ON ue.empresa.id = e.id
    WHERE ue.usuario.id = :usuarioId
    AND ue.activo = true
    AND e.activa = true
    ORDER BY e.nombre
    """)
    List<Empresa> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    // === NUEVOS MÉTODOS PARA FACTURACIÓN ===

    // Buscar empresas que requieren configuración de Hacienda
    List<Empresa> findByRequiereHaciendaTrue();

    // Buscar empresas por régimen tributario
    @Query("SELECT e FROM Empresa e WHERE e.regimenTributario = :regimen")
    List<Empresa> findByRegimenTributario(@Param("regimen") String regimen);

    // Buscar empresa con su configuración de Hacienda
    @Query("""
    SELECT e FROM Empresa e
    LEFT JOIN FETCH e.configHacienda
    WHERE e.id = :id
    """)
    Optional<Empresa> findByIdWithConfigHacienda(@Param("id") Long id);

    // Buscar empresas con actividades
    @Query("""
    SELECT DISTINCT e FROM Empresa e
    LEFT JOIN FETCH e.actividades ea
    LEFT JOIN FETCH ea.actividad
    WHERE e.id = :id
    """)
    Optional<Empresa> findByIdWithActividades(@Param("id") Long id);

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

    // Buscar empresas por provincia
    @Query("SELECT e FROM Empresa e WHERE e.provincia.id = :provinciaId")
    List<Empresa> findByProvinciaId(@Param("provinciaId") Integer provinciaId);
}