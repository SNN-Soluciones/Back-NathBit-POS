package com.snnsoluciones.backnathbitpos.repository;

import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {
    
    List<UsuarioEmpresa> findByUsuarioId(Long usuarioId);
    
    List<UsuarioEmpresa> findByEmpresaId(Long empresaId);
    
    List<UsuarioEmpresa> findBySucursalId(Long sucursalId);
    
    Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaIdAndSucursalId(
        Long usuarioId, Long empresaId, Long sucursalId);
    
    boolean existsByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);
}