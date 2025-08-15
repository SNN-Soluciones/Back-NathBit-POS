package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.UsuarioRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRegistroRepository extends JpaRepository<UsuarioRegistro, Long> {
    
    Optional<UsuarioRegistro> findByUsuarioId(Long usuarioId);
    
    boolean existsByUsuarioId(Long usuarioId);
}