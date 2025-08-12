package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaConfigHaciendaRepository extends JpaRepository<EmpresaConfigHacienda, Long> {
    
    // Buscar configuración por empresa
    Optional<EmpresaConfigHacienda> findByEmpresaId(Long empresaId);
    
    // Buscar configuraciones por ambiente
    List<EmpresaConfigHacienda> findByAmbiente(AmbienteHacienda ambiente);
    
    // Buscar configuraciones con certificados próximos a vencer
    @Query("""
    SELECT ch FROM EmpresaConfigHacienda ch
    WHERE ch.fechaVencimientoCertificado IS NOT NULL
    AND ch.fechaVencimientoCertificado <= :fechaLimite
    """)
    List<EmpresaConfigHacienda> findCertificadosProximosVencer(
        @Param("fechaLimite") LocalDate fechaLimite
    );
    
    // Verificar si existe configuración completa
    @Query("""
    SELECT CASE WHEN COUNT(ch) > 0 THEN true ELSE false END
    FROM EmpresaConfigHacienda ch
    WHERE ch.empresa.id = :empresaId
    AND ch.usuarioHacienda IS NOT NULL
    AND ch.claveHacienda IS NOT NULL
    """)
    boolean existsConfiguracionCompleta(@Param("empresaId") Long empresaId);
}