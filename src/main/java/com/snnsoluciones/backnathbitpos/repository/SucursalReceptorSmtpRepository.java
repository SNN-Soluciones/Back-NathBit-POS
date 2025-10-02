package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.SucursalReceptorSmtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SucursalReceptorSmtpRepository extends JpaRepository<SucursalReceptorSmtp, Long> {
    
    @Query("SELECT srs FROM SucursalReceptorSmtp srs " +
           "JOIN srs.sucursal s " +
           "WHERE s.empresa.id = :empresaId AND srs.email = :email " +
           "AND srs.procesarAutomaticamente = true")
    Optional<SucursalReceptorSmtp> findByEmpresaIdAndEmailReceptor(
            @Param("empresaId") Long empresaId, 
            @Param("email") String email);
}