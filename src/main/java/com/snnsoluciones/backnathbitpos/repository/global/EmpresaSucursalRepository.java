package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.EmpresaSucursal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para EmpresaSucursal
 */
@Repository
public interface EmpresaSucursalRepository extends JpaRepository<EmpresaSucursal, UUID> {
    
    Optional<EmpresaSucursal> findBySchemaName(String schemaName);
    
    boolean existsBySchemaName(String schemaName);
    
    List<EmpresaSucursal> findByEmpresaId(UUID empresaId);
    
    List<EmpresaSucursal> findByEmpresaIdAndActivaTrue(UUID empresaId);
    
    @Query("SELECT s FROM EmpresaSucursal s WHERE s.empresa.id = :empresaId AND s.codigoSucursal = :codigo")
    Optional<EmpresaSucursal> findByEmpresaIdAndCodigo(@Param("empresaId") UUID empresaId,
                                                       @Param("codigo") String codigo);
}
