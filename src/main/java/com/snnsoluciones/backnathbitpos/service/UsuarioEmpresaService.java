package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import java.util.List;
import java.util.Optional;
import org.apache.poi.sl.draw.geom.GuideIf.Op;

public interface UsuarioEmpresaService {
    
    UsuarioEmpresa asignar(Long usuarioId, Long empresaId, Long sucursalId);
    
    List<UsuarioEmpresa> listarPorUsuario(Long usuarioId);
    
    List<UsuarioEmpresa> listarPorEmpresa(Long empresaId);

    boolean tieneAcceso(Long usuarioId, Long empresaId, Long sucursalId);

    boolean existsByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);

    Optional<UsuarioEmpresa> buscarPorId(Long id);
}