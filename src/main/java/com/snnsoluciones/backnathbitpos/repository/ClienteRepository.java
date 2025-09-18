package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import java.math.BigDecimal;
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

    /**
     * Buscar clientes GLOBALES de una empresa
     */
    List<Cliente> findByEmpresaIdAndSucursalIdIsNullAndActivoTrueOrderByNombreComercialAsc(
        Long empresaId);

    /**
     * Buscar clientes LOCALES de una sucursal
     */
    List<Cliente> findByEmpresaIdAndSucursalIdAndActivoTrueOrderByNombreComercialAsc(Long empresaId,
        Long sucursalId);

    /**
     * Verificar si existe cliente global por identificación
     */
    boolean existsByEmpresaIdAndNumeroIdentificacionAndSucursalIdIsNull(Long empresaId,
        String numeroIdentificacion);

    /**
     * Verificar si existe cliente local por identificación
     */
    boolean existsByEmpresaIdAndNumeroIdentificacionAndSucursalId(Long empresaId,
        String numeroIdentificacion, Long sucursalId);

    /**
     * Buscar cliente global por identificación
     */
    Optional<Cliente> findByEmpresaIdAndNumeroIdentificacionAndSucursalIdIsNull(Long empresaId,
        String numeroIdentificacion);

    /**
     * Buscar cliente local por identificación
     */
    Optional<Cliente> findByEmpresaIdAndNumeroIdentificacionAndSucursalId(Long empresaId,
        String numeroIdentificacion, Long sucursalId);

    /**
     * Búsqueda global por término (CORREGIDA para emails)
     */
    @Query("""
        SELECT DISTINCT c FROM Cliente c
        LEFT JOIN c.clienteEmails e
        WHERE c.empresa.id = :empresaId
          AND c.sucursal.id IS NULL
          AND c.activo = true
          AND (LOWER(c.numeroIdentificacion) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(e.email) LIKE LOWER(CONCAT('%', :termino, '%')))
        ORDER BY c.razonSocial
        """)
    List<Cliente> buscarGlobalesPorTermino(@Param("empresaId") Long empresaId,
        @Param("termino") String termino);

    /**
     * Búsqueda local por término (CORREGIDA para emails)
     */
    @Query("""
        SELECT DISTINCT c FROM Cliente c
        LEFT JOIN c.clienteEmails e
        WHERE c.empresa.id = :empresaId
          AND c.sucursal.id = :sucursalId
          AND c.activo = true
          AND (LOWER(c.numeroIdentificacion) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(e.email) LIKE LOWER(CONCAT('%', :termino, '%')))
        ORDER BY c.razonSocial
        """)
    List<Cliente> buscarLocalesPorTermino(@Param("empresaId") Long empresaId,
        @Param("sucursalId") Long sucursalId,
        @Param("termino") String termino);

    /**
     * Para el método que ya existe en tu código también hay que corregirlo
     */
    @Query("""
        SELECT DISTINCT c FROM Cliente c
        LEFT JOIN c.clienteEmails e
        WHERE c.empresa.id = :empresaId
          AND c.activo = true
          AND (LOWER(c.numeroIdentificacion) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%'))
               OR LOWER(e.email) LIKE LOWER(CONCAT('%', :termino, '%')))
        ORDER BY c.razonSocial
        """)
    List<Cliente> buscarPorEmpresaYTermino(@Param("empresaId") Long empresaId,
        @Param("termino") String termino);
}