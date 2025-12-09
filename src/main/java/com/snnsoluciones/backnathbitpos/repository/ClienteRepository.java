package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    @Query("SELECT COUNT(c) > 0 FROM Cliente c " +
        "LEFT JOIN c.clienteEmails ce " +
        "WHERE c.empresa.id = :empresaId " +
        "AND c.numeroIdentificacion = :numeroIdentificacion " +
        "AND ce.email = :email " +
        "AND c.activo = true")
    boolean existsByEmpresaIdAndNumeroIdentificacionAndEmailsAndActivoTrue(
        @Param("empresaId") Long empresaId,
        @Param("numeroIdentificacion") String numeroIdentificacion,
        @Param("email") String email
    );


    // Contar clientes por empresa
    long countByEmpresaIdAndActivoTrue(Long empresaId);

    /**
     * Extra: Si también quieres por empresa
     */
    long countByEmpresaIdAndBloqueadoPorMora(Long empresaId, Boolean bloqueadoPorMora);

    /**
     * Extra: Buscar clientes bloqueados
     */
    Page<Cliente> findByBloqueadoPorMora(Boolean bloqueadoPorMora, Pageable pageable);

    // En ClienteRepository.java agregar:

    /**
     * Buscar clientes con saldo mayor a un monto
     */
    Page<Cliente> findBySaldoActualGreaterThan(BigDecimal monto, Pageable pageable);

    /**
     * Buscar clientes por permiso de crédito
     */
    Page<Cliente> findByPermiteCredito(Boolean permiteCredito, Pageable pageable);

    List<Cliente> findByEmpresaIdAndUpdatedAtAfter(Long empresaId, LocalDateTime updatedAt);

    List<Cliente> findByEmpresaId(Long empresaId);

}