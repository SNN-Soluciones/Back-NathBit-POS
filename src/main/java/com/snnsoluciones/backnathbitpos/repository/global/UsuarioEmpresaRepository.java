package com.snnsoluciones.backnathbitpos.repository.global;

import com.snnsoluciones.backnathbitpos.entity.global.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioEmpresa.UsuarioEmpresaId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para UsuarioEmpresa
 */
@Repository
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UsuarioEmpresaId> {
    
    List<UsuarioEmpresa> findByUsuarioId(UUID usuarioId);
    
    List<UsuarioEmpresa> findByEmpresaId(UUID empresaId);
    
    List<UsuarioEmpresa> findByUsuarioIdAndActivoTrue(UUID usuarioId);
    
    @Query("SELECT ue FROM UsuarioEmpresa ue " +
           "LEFT JOIN FETCH ue.empresa " +
           "LEFT JOIN FETCH ue.usuarioSucursales " +
           "WHERE ue.usuario.id = :usuarioId AND ue.activo = true")
    List<UsuarioEmpresa> findActivosByUsuarioIdWithRelations(@Param("usuarioId") UUID usuarioId);
    
    @Query("SELECT COUNT(ue) FROM UsuarioEmpresa ue WHERE ue.empresa.id = :empresaId AND ue.activo = true")
    long countUsuariosActivosByEmpresa(@Param("empresaId") UUID empresaId);
}
