package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.EmpresaCAByS;
import java.util.Optional;
import org.apache.poi.sl.draw.geom.GuideIf.Op;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaCABySRepository extends JpaRepository<EmpresaCAByS, Long> {
    
    boolean existsByEmpresaIdAndCodigoCabysId(Long empresaId, Long codigoCabysId);
    
    List<EmpresaCAByS> findByEmpresaIdAndActivoTrue(Long empresaId);

    // Buscar por empresa y código CABYS (string)
    Optional<EmpresaCAByS> findByEmpresaIdAndCodigoCabysCodigoAndActivoTrue(
        Long empresaId,
        String codigoCabys
    );

    Optional<EmpresaCAByS> findBySucursalIdAndCodigoCabysCodigoAndActivoTrue(
        Long sucursalId,
        String codigoCabys
    );

    Optional<EmpresaCAByS> findByEmpresaIdAndCodigoCabysCodigo(Long empresaId, String codigoCabys);


}