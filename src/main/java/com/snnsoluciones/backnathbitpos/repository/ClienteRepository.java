package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
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
    List<Cliente> findBySucursalIdAndNumeroIdentificacionAndActivoTrue(
        Long sucursalId,
        String numeroIdentificacion
    );

    // Buscar por sucursal, número y emails específicos
    Optional<Cliente> findBySucursalIdAndNumeroIdentificacionAndEmails(
        Long sucursalId,
        String numeroIdentificacion,
        String emails
    );

    // Búsqueda por sucursal con paginación
    Page<Cliente> findBySucursalIdAndActivoTrue(Long sucursalId, Pageable pageable);

    // Búsqueda con filtros
    @Query("SELECT c FROM Cliente c WHERE c.sucursal.id = :sucursalId " +
        "AND c.activo = true " +
        "AND (:busqueda IS NULL OR :busqueda = '' OR " +
        "     LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
        "     c.numeroIdentificacion LIKE CONCAT('%', :busqueda, '%') OR " +
        "     LOWER(c.emails) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<Cliente> buscarPorSucursal(
        @Param("sucursalId") Long sucursalId,
        @Param("busqueda") String busqueda,
        Pageable pageable
    );

    // Buscar clientes con exoneración activa
    @Query("SELECT DISTINCT c FROM Cliente c " +
        "JOIN c.exoneraciones e " +
        "WHERE c.sucursal.id = :sucursalId " +
        "AND c.activo = true " +
        "AND c.tieneExoneracion = true " +
        "AND e.activo = true")
    List<Cliente> findClientesConExoneracionActiva(@Param("sucursalId") Long sucursalId);

    // Verificar si existe cliente activo con la misma identificación y emails
    boolean existsBySucursalIdAndNumeroIdentificacionAndEmailsAndActivoTrue(
        Long sucursalId,
        String numeroIdentificacion,
        String emails
    );

    // Contar clientes por sucursal
    long countBySucursalIdAndActivoTrue(Long sucursalId);

    // Buscar por tipo de identificación
    List<Cliente> findBySucursalIdAndTipoIdentificacionAndActivoTrue(
        Long sucursalId,
        TipoIdentificacion tipoIdentificacion
    );
}