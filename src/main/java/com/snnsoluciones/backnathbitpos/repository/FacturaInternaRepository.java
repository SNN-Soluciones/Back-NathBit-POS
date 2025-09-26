package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.FacturaInterna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaInternaRepository extends JpaRepository<FacturaInterna, Long> {

    // Buscar por empresa
    Page<FacturaInterna> findByEmpresaId(Long empresaId, Pageable pageable);
    /**
     * Buscar factura interna por número
     */
    Optional<FacturaInterna> findByNumero(String numero);

    // Buscar por sucursal
    Page<FacturaInterna> findBySucursalId(Long sucursalId, Pageable pageable);

    // Buscar por estado
    Page<FacturaInterna> findByEmpresaIdAndEstado(Long empresaId, String estado, Pageable pageable);

    // Obtener el último número de factura para generar el siguiente
    @Query("SELECT f.numero FROM FacturaInterna f WHERE f.empresa.id = :empresaId " +
        "AND f.numero LIKE :prefix% " +
        "ORDER BY f.numero DESC")
    List<String> findUltimoNumeroByEmpresaAndPrefix(
        @Param("empresaId") Long empresaId,
        @Param("prefix") String prefix,
        Pageable pageable
    );
}