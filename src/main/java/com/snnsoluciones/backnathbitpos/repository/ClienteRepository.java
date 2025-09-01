package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Buscar por sucursal y número de identificación (puede devolver múltiples)
    List<Cliente> findByEmpresaIdAndNumeroIdentificacionAndActivoTrue(
        Long sucursalId,
        String numeroIdentificacion
    );

    // Buscar por sucursal, número y emails específicos
    @Query("SELECT DISTINCT c FROM Cliente c " +
        "LEFT JOIN c.clienteEmails ce " +
        "WHERE c.empresa.id = :empresaId " +
        "AND (:numeroIdentificacion IS NULL OR c.numeroIdentificacion = :numeroIdentificacion) " +
        "AND (:email IS NULL OR ce.email = :email)")
    List<Cliente> findByEmpresaIdAndNumeroIdentificacionAndEmails(
        @Param("empresaId") Long empresaId,
        @Param("numeroIdentificacion") String numeroIdentificacion,
        @Param("email") String email
    );

    Page<Cliente> findAllByEmpresaId(Long empresaId, Pageable pageable);

    // Búsqueda con filtros
    @Query("SELECT DISTINCT c FROM Cliente c " +
        "LEFT JOIN c.clienteEmails ce " +
        "WHERE c.empresa.id = :empresaId " +
        "AND c.activo = true " +
        "AND (:busqueda IS NULL OR :busqueda = '' OR " +
        "     LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
        "     c.numeroIdentificacion LIKE CONCAT('%', :busqueda, '%') OR " +
        "     LOWER(ce.email) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<Cliente> buscarPorEmpresa(
        @Param("empresaId") Long empresaId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

    // Buscar clientes con exoneración activa
    @Query("SELECT DISTINCT c FROM Cliente c " +
        "JOIN c.exoneraciones e " +
        "WHERE c.empresa.id = :empresaId " +
        "AND c.activo = true " +
        "AND c.tieneExoneracion = true " +
        "AND e.activo = true")
    List<Cliente> findClientesConExoneracionActiva(@Param("empresaId") Long empresaId);

    // Verificar si existe cliente activo con la misma identificación y emails
    boolean existsByEmpresaIdAndNumeroIdentificacionAndEmailsAndActivoTrue(
        Long empresaId,
        String numeroIdentificacion,
        String emails
    );

    // Contar clientes por empresa
    long countByEmpresaIdAndActivoTrue(Long empresaId);

    // Buscar por tipo de identificación
    List<Cliente> findByEmpresaIdAndTipoIdentificacionAndActivoTrue(
        Long empresaId,
        TipoIdentificacion tipoIdentificacion
    );
}