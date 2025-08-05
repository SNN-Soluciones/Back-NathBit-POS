package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.*;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para UsuarioGlobal
 */
@Repository
public interface UsuarioGlobalRepository extends JpaRepository<UsuarioGlobal, UUID>,
    JpaSpecificationExecutor<UsuarioGlobal> {

    Optional<UsuarioGlobal> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UsuarioGlobal u LEFT JOIN FETCH u.usuarioEmpresas WHERE u.email = :email")
    Optional<UsuarioGlobal> findByEmailWithEmpresas(@Param("email") String email);

    @Query("SELECT u FROM UsuarioGlobal u WHERE u.activo = true AND u.bloqueado = false")
    List<UsuarioGlobal> findAllActivos();
}