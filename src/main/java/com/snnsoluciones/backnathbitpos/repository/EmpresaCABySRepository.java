package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.EmpresaCAByS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaCABySRepository extends JpaRepository<EmpresaCAByS, Long> {
    
    boolean existsByEmpresaIdAndCodigoCabysId(Long empresaId, Long codigoCabysId);
    
    List<EmpresaCAByS> findByEmpresaIdAndActivoTrue(Long empresaId);
}